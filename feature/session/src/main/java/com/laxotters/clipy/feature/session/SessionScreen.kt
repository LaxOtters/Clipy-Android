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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetLayout
import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.webview.SessionWebView
import com.laxotters.clipy.feature.session.webview.rememberSessionWebViewController

@Composable
fun SessionRoute(
    sessionId: String,
    onHomeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewController = rememberSessionWebViewController()

    val screenState = state.copy(
        sessionId = state.sessionId.ifBlank { sessionId },
        currentUrl = state.currentUrl.ifBlank { DEFAULT_HOME_URL },
    )

    LaunchedEffect(sessionId) {
        viewModel.dispatch(
            SessionUiEvent.ScreenEntered(
                sessionId = sessionId,
            ),
        )
    }

    SessionBackHandler(
        canGoBack = screenState.canGoBack,
        onWebViewBack = webViewController::goBack,
        onSessionExit = onHomeClick,
    )

    SessionScreen(
        state = screenState,
        onHomeClick = onHomeClick,
        onBottomSheetValueChange = { bottomSheetValue ->
            viewModel.dispatch(
                SessionUiEvent.BottomSheetValueChanged(bottomSheetValue),
            )
        },
        modifier = modifier,
    ) {
        SessionWebView(
            url = screenState.currentUrl,
            controller = webViewController,
            onPageStateChanged = { url, canGoBack, canGoForward ->
                viewModel.dispatch(
                    SessionUiEvent.PageLoaded(
                        url = url,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                    ),
                )
            },
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun SessionScreen(
    state: SessionUiState,
    onHomeClick: () -> Unit,
    onBottomSheetValueChange: (ClipyBottomSheetValue) -> Unit,
    modifier: Modifier = Modifier,
    webContent: @Composable () -> Unit,
) {
    // TODO: 디자인 시스템 적용
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        SessionHeader(
            onHomeClick = onHomeClick,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            webContent()
            ClipyBottomSheetLayout(
                value = state.bottomSheetState.toClipyBottomSheetValue(),
                onValueChange = { bottomSheetValue ->
                    onBottomSheetValueChange(bottomSheetValue)
                },
                modifier = Modifier,
                headerContent = { contentValue ->
                    SessionBottomSheetChrome(
                        value = contentValue,
                    )
                },
                sheetContent = { contentValue ->
                    SessionBottomSheetContent(
                        value = contentValue,
                    )
                },
            )
        }
    }
}

@Composable
private fun SessionHeader(
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionHeaderTextButton(
            text = "홈",
            onClick = onHomeClick,
        )
        Text(
            text = "Clipy",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun SessionBottomSheetChrome(
    value: ClipyBottomSheetValue,
    modifier: Modifier = Modifier,
) {
    when (value) {
        ClipyBottomSheetValue.MINIMIZED,
        ClipyBottomSheetValue.PEEK,
        -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                SessionBottomSheetUrlBar()
            }
        }

        ClipyBottomSheetValue.HIDDEN,
        ClipyBottomSheetValue.EXPANDED,
        -> Unit
    }
}

@Composable
private fun SessionBottomSheetContent(
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
            ClipyBottomSheetValue.PEEK -> SessionPeekPlaceholder(
                modifier = Modifier.padding(top = 16.dp),
            )

            ClipyBottomSheetValue.EXPANDED -> SessionExpandedPlaceholder()
        }
    }
}

@Composable
private fun SessionBottomSheetUrlBar(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "Url Bar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionPeekPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "Items",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun SessionExpandedPlaceholder(
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
private fun SessionHeaderTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(
                minWidth = Dp.Hairline,
                minHeight = Dp.Hairline,
            )
            .size(width = 20.dp, height = 32.dp),
        colors = transparentButtonColors(),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
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
private fun transparentButtonColors() = ButtonDefaults.buttonColors(
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
            ),
            onHomeClick = { },
            onBottomSheetValueChange = { },
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
