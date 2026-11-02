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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetDefaults.HeaderHeight
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.designsystem.component.bottomsheet.ClipyBottomSheetLayout
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarIcon
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.webview.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.webview.SessionWebDialogState
import com.laxotters.clipy.feature.session.webview.SessionWebView
import com.laxotters.clipy.feature.session.webview.SessionWebViewCallbacks
import com.laxotters.clipy.feature.session.webview.SessionWebViewController
import com.laxotters.clipy.feature.session.webview.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.webview.rememberSessionWebViewController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun SessionRoute(
    viewModel: SessionViewModel,
    navigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewController = rememberSessionWebViewController()
    val webDialogState = key(state.sessionId) { rememberSessionWebDialogState() }
    val snackbarController = rememberClipySnackbarController()

    BackHandler { viewModel.dispatch(SessionUiEvent.SystemBackPressed) }

    SessionScreen(
        state = state,
        actions = createScreenActions(
            viewModel = viewModel,
            webViewController = webViewController,
            navigateToHome = navigateToHome,
        ),
    ) {
        SessionWebViewHost(
            sessionId = state.sessionId,
            initialUrl = state.initialUrl,
            controller = webViewController,
            dialogState = webDialogState,
            onEvent = viewModel::dispatch,
            modifier = Modifier.fillMaxSize(),
        )
    }

    SessionWebDialogHosts(state = webDialogState)
    LaunchedEffect(viewModel.effect, snackbarController) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SessionUiSideEffect.GoBackInWebView -> webViewController.goBack()
                SessionUiSideEffect.NavigateToHome -> navigateToHome()
                // Snackbar 표시가 다음 탐색 효과 수집을 막지 않도록 별도 coroutine에서 표시합니다.
                is SessionUiSideEffect.ShowSnackbar -> launch {
                    snackbarController.showSnackbar(
                        message = effect.message,
                        icon = ClipySnackbarIcon.Error,
                        key = effect.key,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionWebViewHost(
    sessionId: String,
    initialUrl: String?,
    controller: SessionWebViewController,
    dialogState: SessionWebDialogState,
    onEvent: (SessionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // sessionId가 바뀌면 WebView 참조와 초기 URL 로딩 상태를 새로 잡습니다.
    key(sessionId) {
        SessionWebView(
            url = initialUrl,
            controller = controller,
            dialogState = dialogState,
            callbacks = SessionWebViewCallbacks(
                onPageFinished = { pageInfo ->
                    onEvent(
                        SessionUiEvent.PageLoaded(
                            sessionId = sessionId,
                            url = pageInfo.url,
                            canGoBack = pageInfo.canGoBack,
                            canGoForward = pageInfo.canGoForward,
                        ),
                    )
                },
                onRootScrolled = { metrics ->
                    onEvent(
                        SessionUiEvent.WebViewRootScrolled(
                            deltaY = metrics.deltaY,
                            scrollableDistance = metrics.scrollableDistance,
                            viewportHeight = metrics.viewportHeight,
                            touchSlopPx = metrics.touchSlopPx,
                        ),
                    )
                },
                onErrorMessage = { message ->
                    onEvent(SessionUiEvent.ErrorMessageReceived(message.message, message.key))
                },
            ),
            modifier = modifier,
        )
    }
}

private fun createScreenActions(
    viewModel: SessionViewModel,
    webViewController: SessionWebViewController,
    navigateToHome: () -> Unit,
): ScreenActions = ScreenActions(
    topBarActions = TopBarActions(
        onHomeClick = navigateToHome,
        onAddItemClick = { },
        onTopBarFoldClick = { viewModel.dispatch(SessionUiEvent.TopBarFoldClicked) },
    ),
    browserBarActions = BrowserBarActions(
        onBackClick = webViewController::goBack,
        onForwardClick = webViewController::goForward,
        onRefreshClick = webViewController::reload,
    ),
    onBottomSheetValueChange = { bottomSheetValue ->
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(bottomSheetValue))
    },
)

private data class ScreenActions(
    val topBarActions: TopBarActions,
    val browserBarActions: BrowserBarActions,
    val onBottomSheetValueChange: (BottomSheetValue) -> Unit,
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
            value = state.bottomSheetState.toBottomSheetValue(),
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
    value: BottomSheetValue,
    browserBarState: BrowserBarState,
    browserBarActions: BrowserBarActions,
    modifier: Modifier = Modifier,
) {
    when (value) {
        BottomSheetValue.MINIMIZED,
        BottomSheetValue.PEEK,
        -> BrowserBar(
            state = browserBarState,
            actions = browserBarActions,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        BottomSheetValue.HIDDEN,
        BottomSheetValue.EXPANDED,
        -> Unit
    }
}

@Composable
private fun SheetContent(
    value: BottomSheetValue,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (value) {
            BottomSheetValue.HIDDEN -> Unit
            BottomSheetValue.MINIMIZED -> Unit
            BottomSheetValue.PEEK -> PeekPlaceholder()
            BottomSheetValue.EXPANDED -> ExpandedPlaceholder()
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        ),
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

private fun BottomSheetState.toBottomSheetValue(): BottomSheetValue =
    when (this) {
        BottomSheetState.HIDDEN -> BottomSheetValue.HIDDEN
        BottomSheetState.MINIMIZED -> BottomSheetValue.MINIMIZED
        BottomSheetState.PEEK -> BottomSheetValue.PEEK
        BottomSheetState.EXPANDED -> BottomSheetValue.EXPANDED
    }
