package com.laxotters.clipy.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.webview.SessionWebView
import com.laxotters.clipy.feature.session.webview.rememberSessionWebViewController

@Composable
fun SessionRoute(
    sessionId: String,
    initialUrl: String,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewController = rememberSessionWebViewController()
    val screenState = state.copy(
        sessionId = state.sessionId.ifBlank { sessionId },
        currentUrl = state.currentUrl.ifBlank { initialUrl.ifBlank { DEFAULT_HOME_URL } },
    )

    LaunchedEffect(sessionId, initialUrl) {
        viewModel.dispatch(
            SessionUiEvent.ScreenEntered(
                sessionId = sessionId,
                initialUrl = initialUrl,
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
        actions = SessionScreenActions(
            onHomeClick = onHomeClick,
            onBackClick = webViewController::goBack,
            onForwardClick = webViewController::goForward,
            onRefreshClick = webViewController::reload,
        ),
        webContent = {
            SessionWebView(
                initialUrl = state.currentUrl,
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
        },
        modifier = modifier,
    )
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

private data class SessionScreenActions(
    val onHomeClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onForwardClick: () -> Unit,
    val onRefreshClick: () -> Unit,
)

@Composable
private fun SessionScreen(
    state: SessionUiState,
    actions: SessionScreenActions,
    webContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: 디자인 시스템 적용
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        SessionHeader(
            onHomeClick = actions.onHomeClick,
        )
        SessionBrowserToolbar(
            state = state,
            actions = actions,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            webContent()
        }
    }
}

@Composable
private fun SessionHeader(
    onHomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrowserToolbarTextButton(
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
private fun SessionBrowserToolbar(
    state: SessionUiState,
    actions: SessionScreenActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrowserToolbarTextButton(
            text = "<",
            enabled = state.canGoBack,
            onClick = actions.onBackClick,
        )
        BrowserToolbarTextButton(
            text = ">",
            enabled = state.canGoForward,
            onClick = actions.onForwardClick,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(14.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = state.currentUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            BrowserToolbarTextButton(
                text = "↻",
                onClick = actions.onRefreshClick,
            )
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun BrowserToolbarTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
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
            actions = SessionScreenActions(
                onHomeClick = { },
                onBackClick = { },
                onForwardClick = { },
                onRefreshClick = { },
            ),
            webContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "WebView")
                }
            },
        )
    }
}
