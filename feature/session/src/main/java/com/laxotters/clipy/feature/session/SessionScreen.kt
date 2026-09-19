package com.laxotters.clipy.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.browser.SessionBrowser
import com.laxotters.clipy.feature.session.browser.SessionBrowserState
import com.laxotters.clipy.feature.session.browser.rememberSessionBrowserState
import com.laxotters.clipy.feature.session.chrome.BrowserBarActions
import com.laxotters.clipy.feature.session.chrome.BrowserBarState
import com.laxotters.clipy.feature.session.chrome.SessionChrome
import com.laxotters.clipy.feature.session.chrome.TopBarActions
import kotlinx.coroutines.flow.collect

/** Session의 Browser와 Chrome을 조립하고 화면 이동과 시스템 Back을 연결합니다. */
@Composable
fun SessionRoute(
    viewModel: SessionViewModel,
    navigateToHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val browserState = rememberSessionBrowserState(state.sessionId, state.initialUrl)
    val currentNavigateToHome by rememberUpdatedState(navigateToHome)

    BackHandler {
        viewModel.dispatch(SessionUiEvent.SystemBackPressed(browserState.canGoBack))
    }

    SessionScreen(
        state = state,
        browserState = browserState,
        topBarActions = TopBarActions(
            onHomeClick = navigateToHome,
            onAddItemClick = {},
            onTopBarFoldClick = { viewModel.dispatch(SessionUiEvent.TopBarFoldClicked) },
        ),
        onBottomSheetValueChange = { viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(it)) },
    ) {
        SessionBrowser(
            state = browserState,
            initialUrl = state.initialUrl,
            onPageFinished = { viewModel.dispatch(SessionUiEvent.PageLoaded(state.sessionId)) },
            onRootScrolled = { metrics ->
                viewModel.dispatch(
                    SessionUiEvent.WebViewRootScrolled(
                        deltaY = metrics.deltaY,
                        scrollableDistance = metrics.scrollableDistance,
                        viewportHeight = metrics.viewportHeight,
                        touchSlopPx = metrics.touchSlopPx,
                    ),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    LaunchedEffect(viewModel, browserState) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SessionUiSideEffect.GoBackInWebView -> browserState.goBack()
                SessionUiSideEffect.NavigateToHome -> currentNavigateToHome()
            }
        }
    }
}

@Composable
private fun SessionScreen(
    state: SessionUiState,
    browserState: SessionBrowserState,
    topBarActions: TopBarActions,
    onBottomSheetValueChange: (BottomSheetValue) -> Unit,
    modifier: Modifier = Modifier,
    webContent: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
    ) {
        webContent()
        SessionChrome(
            state = state,
            topBarActions = topBarActions,
            browserBarState = BrowserBarState(
                urlLabel = browserState.urlLabel,
                canGoBack = browserState.canGoBack,
                canGoForward = browserState.canGoForward,
                canRefresh = browserState.canRefresh,
            ),
            browserBarActions = BrowserBarActions(
                onBackClick = browserState::goBack,
                onForwardClick = browserState::goForward,
                onRefreshClick = browserState::reload,
            ),
            onBottomSheetValueChange = onBottomSheetValueChange,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionScreenPreview() {
    ClipyTheme {
        SessionScreen(
            state = SessionUiState(sessionId = "preview-session"),
            browserState = rememberSessionBrowserState("preview-session", DEFAULT_HOME_URL),
            topBarActions = TopBarActions({}, {}, {}),
            onBottomSheetValueChange = {},
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "WebView")
            }
        }
    }
}
