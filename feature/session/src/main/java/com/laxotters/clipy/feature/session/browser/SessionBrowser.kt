package com.laxotters.clipy.feature.session.browser

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarIcon
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.browser.dialog.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.handoff.createExternalAppHandler
import com.laxotters.clipy.feature.session.browser.webview.RootScrollMetrics
import com.laxotters.clipy.feature.session.browser.webview.SessionWebView
import com.laxotters.clipy.feature.session.browser.webview.SessionWebViewCallbacks
import kotlinx.coroutines.launch

/** Browser의 WebView와 요청 UI를 연결하고 화면 조율에 필요한 탐색·스크롤만 전달합니다. */
@Composable
internal fun SessionBrowser(
    state: SessionBrowserState,
    initialUrl: String?,
    onPageFinished: () -> Unit,
    onRootScrolled: (RootScrollMetrics) -> Unit,
    modifier: Modifier = Modifier,
    externalAppHandlerFactory: (Context) -> ExternalAppHandler = ::createExternalAppHandler,
) {
    key(state) {
        val dialogState = rememberSessionWebDialogState()
        val snackbarController = rememberClipySnackbarController()
        val scope = rememberCoroutineScope()

        DisposableEffect(state) {
            onDispose { state.reset() }
        }

        SessionWebView(
            url = initialUrl,
            controller = state.controller,
            dialogState = dialogState,
            callbacks = SessionWebViewCallbacks(
                onPageFinished = { pageInfo ->
                    state.updatePageInfo(pageInfo)
                    onPageFinished()
                },
                onRootScrolled = onRootScrolled,
                onErrorMessage = { message ->
                    scope.launch {
                        snackbarController.showSnackbar(
                            message = message.message,
                            icon = ClipySnackbarIcon.Error,
                            key = message.key,
                        )
                    }
                },
            ),
            modifier = modifier,
            externalAppHandlerFactory = externalAppHandlerFactory,
        )
        SessionWebDialogHosts(state = dialogState)
    }
}
