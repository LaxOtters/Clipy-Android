package com.laxotters.clipy.feature.session.browser.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogCoordinator
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogState
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogStrings
import com.laxotters.clipy.feature.session.browser.dialog.sessionWebDialogStrings
import com.laxotters.clipy.feature.session.browser.handoff.ExternalActionCoordinator
import com.laxotters.clipy.feature.session.browser.handoff.ExternalActionStrings
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.handoff.createExternalAppHandler
import com.laxotters.clipy.feature.session.browser.handoff.externalActionStrings
import com.laxotters.clipy.feature.session.browser.webview.client.SessionWebChromeClient
import com.laxotters.clipy.feature.session.browser.webview.client.SessionWebViewClient
import com.laxotters.clipy.feature.session.browser.webview.download.DownloadSourceTracker
import com.laxotters.clipy.feature.session.browser.webview.scroll.RootScrollWebView

/**
 * Session의 단일 Main WebView를 렌더링하고 페이지·방문 기록·root scroll 상태를 호출자에게 전달합니다.
 * 실제 WebView release 시 대기 중인 JavaScript Dialog와 외부 이동 확인 요청을 정리합니다.
 */
@Composable
internal fun SessionWebView(
    url: String?,
    controller: SessionWebViewController,
    dialogState: SessionWebDialogState,
    callbacks: SessionWebViewCallbacks,
    modifier: Modifier = Modifier,
    externalAppHandlerFactory: (Context) -> ExternalAppHandler = ::createExternalAppHandler,
) {
    // 실제 WebView를 다시 만들지 않고도 오래 유지되는 callback에서 최신 호출부를 사용합니다.
    val currentCallbacks by rememberUpdatedState(callbacks)
    val currentExternalAppHandlerFactory by rememberUpdatedState(externalAppHandlerFactory)
    val dialogStrings = sessionWebDialogStrings()
    val externalActionStrings = externalActionStrings()
    val loadState = remember { SessionWebViewLoadState() }
    val dependencies = remember(controller, dialogState) {
        WebViewDependencies(
            controller = controller,
            dialogState = dialogState,
            callbacks = SessionWebViewCallbacks(
                onPageFinished = { pageInfo -> currentCallbacks.onPageFinished(pageInfo) },
                onRootScrolled = { metrics -> currentCallbacks.onRootScrolled(metrics) },
                onErrorMessage = { message -> currentCallbacks.onErrorMessage(message) },
            ),
            externalAppHandlerFactory = { context -> currentExternalAppHandlerFactory(context) },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            createSessionWebViewContainer(
                context = context,
                dependencies = dependencies,
                dialogStrings = dialogStrings,
                externalActionStrings = externalActionStrings,
            )
        },
        update = { container ->
            container.dialogCoordinator.updateStrings(dialogStrings)
            container.externalActionCoordinator.updateStrings(externalActionStrings)
            controller.attach(container.webView)

            // 전달된 URL이 바뀔 때만 새 탐색을 시작해 기존 WebView 상태를 유지합니다.
            if (url != null && loadState.lastRequestedUrl != url) {
                loadState.lastRequestedUrl = url
                container.webView.loadUrl(url)
            }
        },
        onRelease = { container -> container.releaseAction.release() },
    )
}

private class WebViewDependencies(
    val controller: SessionWebViewController,
    val dialogState: SessionWebDialogState,
    val callbacks: SessionWebViewCallbacks,
    val externalAppHandlerFactory: (Context) -> ExternalAppHandler,
)

private fun createSessionWebViewContainer(
    context: Context,
    dependencies: WebViewDependencies,
    dialogStrings: SessionWebDialogStrings,
    externalActionStrings: ExternalActionStrings,
): SessionWebViewContainer {
    val dialogCoordinator = SessionWebDialogCoordinator(
        manager = dependencies.dialogState.jsDialogManager,
        strings = dialogStrings,
        displayGate = dependencies.dialogState.displayGate,
    )
    val externalActionCoordinator = ExternalActionCoordinator(
        dialogManager = dependencies.dialogState.externalActionManager,
        appHandler = dependencies.externalAppHandlerFactory(context),
        onErrorMessage = dependencies.callbacks.onErrorMessage,
        strings = externalActionStrings,
    )
    val downloadSourceTracker = DownloadSourceTracker()
    val webViewClient = SessionWebViewClient(
        callbacks = dependencies.callbacks,
        downloadSourceTracker = downloadSourceTracker,
        onExternalAppRequested = externalActionCoordinator::requestExternalApp,
    )
    return SessionWebViewContainer(
        context = context,
        dialogCoordinator = dialogCoordinator,
        externalActionCoordinator = externalActionCoordinator,
        controller = dependencies.controller,
        downloadSourceTracker = downloadSourceTracker,
    ).apply {
        webView.configureSessionWebView(
            dialogCoordinator = dialogCoordinator,
            webViewClient = webViewClient,
        )
        webView.onRootScrolled = dependencies.callbacks.onRootScrolled
        // DownloadListener에는 frame 정보가 없어 같은 URL의 탐색 callback 근거로 출처를 확인합니다.
        webView.setDownloadListener { downloadUrl, _, _, _, _ ->
            val requestedUrl = downloadUrl.orEmpty()
            if (webViewClient.consumeConfirmedMainFrameDownload(requestedUrl)) {
                externalActionCoordinator.requestOpenInBrowser(requestedUrl)
            } else {
                externalActionCoordinator.reportUnsupportedDownload()
            }
        }
    }
}

private class SessionWebViewLoadState {
    var lastRequestedUrl: String? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSessionWebView(
    dialogCoordinator: SessionWebDialogCoordinator,
    webViewClient: SessionWebViewClient,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.setSupportMultipleWindows(false)
    settings.javaScriptCanOpenWindowsAutomatically = false
    this.webViewClient = webViewClient
    webChromeClient = SessionWebChromeClient(dialogCoordinator)
}

// AndroidView.factory에서만 생성하며, WebView별 종료에 필요한 의존성을 함께 받습니다.
@SuppressLint("ViewConstructor")
private class SessionWebViewContainer(
    context: Context,
    val dialogCoordinator: SessionWebDialogCoordinator,
    val externalActionCoordinator: ExternalActionCoordinator,
    controller: SessionWebViewController,
    downloadSourceTracker: DownloadSourceTracker,
) : FrameLayout(context) {
    val webView: RootScrollWebView = RootScrollWebView(context)
    val releaseAction = SessionWebViewRelease(
        closeDialog = dialogCoordinator::close,
        closeExternalAction = externalActionCoordinator::close,
        detachController = { controller.detach(webView) },
        releaseCallbacks = {
            downloadSourceTracker.clear()
            webView.setDownloadListener(null)
            webView.onRootScrolled = null
            webView.release()
        },
        removeFromParent = { removeView(webView) },
        destroy = {
            check(webView.parent == null) { "WebView must be detached before destroy" }
            webView.destroy()
        },
    )

    init {
        addView(
            webView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
    }
}
