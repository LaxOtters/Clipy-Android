// TODO: design system 컴포넌트로 분리 후 suppress 제거
@file:Suppress("TooManyFunctions")

package com.laxotters.clipy.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetDefaults.HeaderHeight
import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetLayout
import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.webview.SessionWebView
import com.laxotters.clipy.feature.session.webview.SessionWebViewController
import com.laxotters.clipy.feature.session.webview.rememberSessionWebViewController

@Composable
fun SessionRoute(
    sessionId: String,
    onHomeClick: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewController = rememberSessionWebViewController()

    val routeInitialUrl = if (state.sessionId == sessionId) state.initialUrl else null
    val screenState = if (routeInitialUrl != null) {
        state
    } else {
        SessionUiState(sessionId = sessionId)
    }

    LaunchedEffect(sessionId) { viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = sessionId)) }

    SessionBackHandler(
        canGoBack = screenState.canGoBack,
        onWebViewBack = webViewController::goBack,
        onSessionExit = onHomeClick,
    )

    SessionScreen(
        state = screenState,
        actions = ScreenActions(
            topBarActions = TopBarActions(
                onHomeClick = onHomeClick,
                onAddItemClick = { },
                onTopBarFoldClick = { viewModel.dispatch(SessionUiEvent.TopBarFoldClicked) },
            ),
            browserBarActions = BrowserBarActions(
                onBackClick = webViewController::goBack,
                onForwardClick = webViewController::goForward,
                onRefreshClick = webViewController::reload,
            ),
            onBottomSheetValueChange = { bottomSheetValue ->
                viewModel.dispatch(
                    SessionUiEvent.BottomSheetValueChanged(bottomSheetValue),
                )
            },
        ),
    ) {
        SessionWebViewHost(
            sessionId = sessionId,
            initialUrl = routeInitialUrl,
            controller = webViewController,
            onPageLoaded = viewModel::dispatch,
            onRootScrolled = { deltaY, scrollableDistance, viewportHeight, touchSlopPx ->
                viewModel.dispatch(
                    SessionUiEvent.WebViewRootScrolled(
                        deltaY = deltaY,
                        scrollableDistance = scrollableDistance,
                        viewportHeight = viewportHeight,
                        touchSlopPx = touchSlopPx,
                    ),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SessionWebViewHost(
    sessionId: String,
    initialUrl: String?,
    controller: SessionWebViewController,
    onPageLoaded: (SessionUiEvent.PageLoaded) -> Unit,
    onRootScrolled: (deltaY: Int, scrollableDistance: Int, viewportHeight: Int, touchSlopPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // sessionId가 바뀌면 WebView 참조와 초기 URL 로딩 상태를 새로 잡습니다.
    key(sessionId) {
        SessionWebView(
            url = initialUrl,
            controller = controller,
            onPageStateChanged = { pageUrl, canGoBack, canGoForward ->
                onPageLoaded(
                    SessionUiEvent.PageLoaded(
                        sessionId = sessionId,
                        url = pageUrl,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                    ),
                )
            },
            onRootScrolled = onRootScrolled,
            modifier = modifier,
        )
    }
}

@Composable
private fun SessionBackHandler(
    canGoBack: Boolean,
    onWebViewBack: () -> Unit,
    onSessionExit: () -> Unit,
) {
    BackHandler {
        if (canGoBack) {
            onWebViewBack()
        } else {
            onSessionExit()
        }
    }
}

private data class ScreenActions(
    val topBarActions: TopBarActions,
    val browserBarActions: BrowserBarActions,
    val onBottomSheetValueChange: (ClipyBottomSheetValue) -> Unit,
)

private data class TopBarActions(
    val onHomeClick: () -> Unit,
    val onAddItemClick: () -> Unit,
    val onTopBarFoldClick: () -> Unit,
)

private data class BrowserBarState(
    val urlLabel: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val canRefresh: Boolean,
)

private data class BrowserBarActions(
    val onBackClick: () -> Unit,
    val onForwardClick: () -> Unit,
    val onRefreshClick: () -> Unit,
)

@Composable
private fun SessionScreen(
    state: SessionUiState,
    actions: ScreenActions,
    modifier: Modifier = Modifier,
    webContent: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        webContent()
        SessionTopBar(
            topBarState = state.topBarState,
            sessionName = "Untitled",
            isFoldEnabled = !state.isWebViewRootScrolling,
            actions = actions.topBarActions,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(70.dp),
        )
        ClipyBottomSheetLayout(
            value = state.bottomSheetState.toClipyBottomSheetValue(),
            onValueChange = { bottomSheetValue ->
                actions.onBottomSheetValueChange(bottomSheetValue)
            },
            modifier = Modifier.fillMaxSize(),
            headerContent = { renderedValue ->
                SheetHeader(
                    value = renderedValue,
                    browserBarState = BrowserBarState(
                        urlLabel = state.currentUrlLabel,
                        canGoBack = state.canGoBack,
                        canGoForward = state.canGoForward,
                        canRefresh = state.initialUrl != null,
                    ),
                    browserBarActions = actions.browserBarActions,
                )
            },
            sheetContent = { renderedValue ->
                SheetContent(
                    value = renderedValue,
                )
            },
        )
    }
}

@Composable
private fun SessionTopBar(
    topBarState: SessionTopBarState,
    sessionName: String,
    isFoldEnabled: Boolean,
    actions: TopBarActions,
    modifier: Modifier = Modifier,
) {
    val foldControlText = when (topBarState) {
        SessionTopBarState.FOLDED -> "펼치기"
        SessionTopBarState.UNFOLDED -> "접기"
    }

    Box(
        modifier = modifier,
    ) {
        if (topBarState == SessionTopBarState.UNFOLDED) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    text = "홈",
                    onClick = actions.onHomeClick,
                    minWidth = 44.dp,
                )
                Text(
                    text = sessionName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ControlButton(
                    text = "추가",
                    onClick = actions.onAddItemClick,
                    minWidth = 44.dp,
                )
            }
        }
        ControlButton(
            text = foldControlText,
            onClick = actions.onTopBarFoldClick,
            enabled = isFoldEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp),
            minWidth = 52.dp,
            height = 28.dp,
        )
    }
}

@Composable
private fun SheetHeader(
    value: ClipyBottomSheetValue,
    browserBarState: BrowserBarState,
    browserBarActions: BrowserBarActions,
    modifier: Modifier = Modifier,
) {
    when (value) {
        ClipyBottomSheetValue.MINIMIZED,
        ClipyBottomSheetValue.PEEK,
        -> BrowserBar(
            state = browserBarState,
            actions = browserBarActions,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        ClipyBottomSheetValue.HIDDEN,
        ClipyBottomSheetValue.EXPANDED,
        -> Unit
    }
}

@Composable
private fun SheetContent(
    value: ClipyBottomSheetValue,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (value) {
            ClipyBottomSheetValue.HIDDEN -> Unit
            ClipyBottomSheetValue.MINIMIZED -> Unit
            ClipyBottomSheetValue.PEEK -> PeekPlaceholder()
            ClipyBottomSheetValue.EXPANDED -> ExpandedPlaceholder()
        }
    }
}

@Composable
private fun BrowserBar(
    state: BrowserBarState,
    actions: BrowserBarActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            text = "<",
            onClick = actions.onBackClick,
            enabled = state.canGoBack,
            minWidth = 20.dp,
            height = 20.dp,
        )
        ControlButton(
            text = ">",
            onClick = actions.onForwardClick,
            enabled = state.canGoForward,
            minWidth = 20.dp,
            height = 20.dp,
        )
        UrlDisplay(
            url = state.urlLabel,
            modifier = Modifier.weight(1f),
        )
        ControlButton(
            text = "새로고침",
            onClick = actions.onRefreshClick,
            enabled = state.canRefresh,
            minWidth = 64.dp,
            height = 32.dp,
        )
    }
}

@Composable
private fun UrlDisplay(
    url: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(HeaderHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PeekPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(128.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "비교할 아이템 없음",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ExpandedPlaceholder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Session",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Item ${index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Dp = 44.dp,
    height: Dp = 36.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(
                minWidth = minWidth,
                minHeight = height,
            )
            .height(height),
        colors = controlButtonColors(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun controlButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
)

@Preview(showBackground = true)
@Composable
private fun SessionScreenPreview() {
    ClipyTheme {
        SessionScreen(
            state = SessionUiState(
                sessionId = "preview-session",
                currentUrl = DEFAULT_HOME_URL,
                currentUrlLabel = "google.com",
            ),
            actions = ScreenActions(
                topBarActions = TopBarActions(
                    onHomeClick = { },
                    onAddItemClick = { },
                    onTopBarFoldClick = { },
                ),
                browserBarActions = BrowserBarActions(
                    onBackClick = { },
                    onForwardClick = { },
                    onRefreshClick = { },
                ),
                onBottomSheetValueChange = { },
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "WebView")
            }
        }
    }
}

private fun BottomSheetState.toClipyBottomSheetValue(): ClipyBottomSheetValue =
    when (this) {
        BottomSheetState.HIDDEN -> ClipyBottomSheetValue.HIDDEN
        BottomSheetState.MINIMIZED -> ClipyBottomSheetValue.MINIMIZED
        BottomSheetState.PEEK -> ClipyBottomSheetValue.PEEK
        BottomSheetState.EXPANDED -> ClipyBottomSheetValue.EXPANDED
    }
