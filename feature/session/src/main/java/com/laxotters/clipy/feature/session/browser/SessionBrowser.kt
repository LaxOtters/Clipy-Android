package com.laxotters.clipy.feature.session.browser

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarIcon
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.browser.dialog.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.handoff.createExternalAppHandler
import com.laxotters.clipy.feature.session.browser.recovery.SessionRecoveryContent
import com.laxotters.clipy.feature.session.browser.webview.RootScrollMetrics
import com.laxotters.clipy.feature.session.browser.webview.SessionNetworkAvailabilityReader
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
    onHomeRequested: () -> Unit,
    modifier: Modifier = Modifier,
    externalAppHandlerFactory: (Context) -> ExternalAppHandler = ::createExternalAppHandler,
) {
    key(state) {
        val dialogState = rememberSessionWebDialogState()
        val snackbarController = rememberClipySnackbarController()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val networkReader = remember(context) { SessionNetworkAvailabilityReader(context) }

        DisposableEffect(state) {
            onDispose { state.reset() }
        }

        Box(modifier) {
            // renderer가 종료되면 이전 인스턴스를 재사용하지 않고 세대마다 새 WebView를 만듭니다.
            key(state.webViewGeneration) {
                SessionWebView(
                    url = state.reopenUrl ?: initialUrl,
                    controller = state.controller,
                    dialogState = dialogState,
                    callbacks = SessionWebViewCallbacks(
                        onPageFinished = { pageInfo ->
                            state.updatePageInfo(pageInfo)
                            onPageFinished()
                        },
                        onRootScrolled = onRootScrolled,
                        onPageLoadChanged = state::onPageLoadChanged,
                        onHistoryChanged = state::updatePageInfo,
                        onErrorMessage = { message ->
                            scope.launch {
                                snackbarController.showSnackbar(
                                    message = message.message,
                                    icon = ClipySnackbarIcon.Error,
                                    key = message.key,
                                )
                            }
                        },
                        onRenderProcessGone = state::onRenderProcessGone,
                    ),
                    externalAppHandlerFactory = externalAppHandlerFactory,
                    networkAvailability = networkReader::snapshot,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            SessionRecoveryContent(state, onHomeRequested)
        }
        SessionWebDialogHosts(state = dialogState)
    }
}
