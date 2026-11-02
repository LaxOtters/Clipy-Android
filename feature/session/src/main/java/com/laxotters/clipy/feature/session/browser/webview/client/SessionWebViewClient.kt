package com.laxotters.clipy.feature.session.browser.webview.client

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageFailure
import com.laxotters.clipy.feature.session.browser.SessionPageLoad
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.webview.PageInfo
import com.laxotters.clipy.feature.session.browser.webview.SessionDocumentTracker
import com.laxotters.clipy.feature.session.browser.webview.SessionWebViewCallbacks
import com.laxotters.clipy.feature.session.browser.webview.download.DownloadSourceTracker
import com.laxotters.clipy.feature.session.browser.webview.navigation.WebNavigationDecision
import com.laxotters.clipy.feature.session.browser.webview.navigation.WebNavigationPolicy

internal class SessionWebViewClient(
    private val callbacks: SessionWebViewCallbacks,
    private val downloadSourceTracker: DownloadSourceTracker,
    private val onExternalAppRequested: (String) -> Unit,
    private val documentTracker: SessionDocumentTracker,
    private val networkAvailability: () -> SessionNetworkAvailability = { SessionNetworkAvailability.UNKNOWN },
) : WebViewClient() {
    private var failure: SessionPageFailure? = null
    private var requestedNavigation: RequestedNavigation? = null

    @Volatile
    private var mainFrameRequest: MainFrameRequest? = null

    @Volatile
    private var isClosed = false

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (isClosed) return
        val requested = requestedNavigation
        requestedNavigation = null
        val navigation = requested?.navigation
        if (navigation != null && navigation.url == url && navigation == documentTracker.navigation) {
            if (requested?.isReported == false) {
                reportLoading(navigation)
            }
            return
        }
        startNavigation(url.orEmpty())
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        if (isClosed || !documentTracker.commit(url.orEmpty())) return
        // 오류 문서가 이전 본문을 덮으면 문서 유지 여부를 다시 반영합니다.
        failure?.let {
            failure = it.copy(
                hasVisibleDocument = documentTracker.hasVisibleDocument,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            )
            hideErrorDocument(view)
            reportFailure()
        }
        if (documentTracker.canShowDocument) {
            requestDocumentDisplay(view)
        }
        reportNavigationFinished(view)
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!isClosed && request.isForMainFrame) {
            mainFrameRequest = MainFrameRequest(request.url.toString(), request.method)
        }
        return null
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        if (isClosed) return
        callbacks.onHistoryChanged(PageInfo(url.orEmpty(), view.canGoBack(), view.canGoForward()))
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (isClosed || !request.isForMainFrame) return
        val url = request.url.toString()
        if (documentTracker.navigation == null) {
            documentTracker.start(url)
        }
        if (!documentTracker.fail(url)) return
        val navigation = checkNotNull(documentTracker.navigation)
        failure = SessionPageFailure(
            navigation = navigation,
            error = error.errorCode.toLoadError(),
            method = request.method,
            hasVisibleDocument = documentTracker.hasVisibleDocument,
            canGoBack = view.canGoBack(),
            canGoForward = view.canGoForward(),
            network = networkAvailability(),
        )
        hideErrorDocument(view)
        reportFailure()
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        if (isClosed) return true
        val url = request.url.toString()
        downloadSourceTracker.recordNavigation(
            url = url,
            isForMainFrame = request.isForMainFrame,
        )
        return when (val decision = WebNavigationPolicy.decide(url, request.isForMainFrame)) {
            WebNavigationDecision.ContinueInWebView -> {
                if (request.isForMainFrame) {
                    requestedNavigation = RequestedNavigation(startNavigation(url), isReported = true)
                }
                false
            }
            is WebNavigationDecision.RequestExternalApp -> {
                reportNavigationStopped()
                onExternalAppRequested(decision.url)
                true
            }
        }
    }

    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        if (isClosed) return
        // 완료된 탐색의 근거가 다음 다운로드 판정에 재사용되지 않도록 비웁니다.
        downloadSourceTracker.clear()
        val isCurrentNavigation = documentTracker.finish(url.orEmpty())
        if (!isCurrentNavigation && view.url != url) return
        callbacks.onPageFinished(
            PageInfo(
                url = url.orEmpty(),
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            ),
        )
        if (isCurrentNavigation) {
            reportNavigationFinished(view)
        }
    }

    fun close() {
        isClosed = true
        failure = null
        requestedNavigation = null
        mainFrameRequest = null
        documentTracker.close()
    }

    fun reload(view: WebView) {
        val url = view.url ?: return
        val method = mainFrameRequest?.takeIf { it.url == url }?.method.orEmpty()
        if (!reportOfflineCommand(view, url, method)) {
            // POST 재전송 거부처럼 탐색이 시작되지 않을 수 있어 Loading은 onPageStarted에서 전달합니다.
            failure = null
            requestedNavigation = RequestedNavigation(documentTracker.start(url), isReported = false)
            view.reload()
        }
    }

    fun retry(view: WebView, url: String) {
        if (!reportOfflineCommand(view, url, "GET")) {
            // onPageStarted 없이 다운로드나 외부 이동으로 끝날 수 있어 먼저 탐색을 등록합니다.
            requestedNavigation = RequestedNavigation(startNavigation(url), isReported = true)
            // 앱이 직접 요청한 본문 GET은 shouldOverrideUrlLoading을 거치지 않습니다.
            downloadSourceTracker.recordNavigation(url, isForMainFrame = true)
            view.loadUrl(url)
        }
    }

    private fun reportOfflineCommand(view: WebView, url: String, method: String): Boolean {
        if (isClosed) return true
        // 검증 전 네트워크도 실제 요청으로 확인하므로 기본 네트워크가 없을 때만 중단합니다.
        if (networkAvailability() != SessionNetworkAvailability.UNAVAILABLE) return false
        view.stopLoading()
        requestedNavigation = null
        val navigation = documentTracker.start(url)
        // 유지 중인 문서가 있어도 오프라인 안내는 오류 화면으로 표시합니다.
        documentTracker.fail(url, discardVisibleDocument = true)
        failure = SessionPageFailure(
            navigation = navigation,
            error = SessionLoadError.CONNECTION,
            method = method,
            hasVisibleDocument = false,
            canGoBack = view.canGoBack(),
            canGoForward = view.canGoForward(),
            network = SessionNetworkAvailability.UNAVAILABLE,
        )
        hideErrorDocument(view)
        reportFailure()
        return true
    }

    private fun requestDocumentDisplay(view: WebView) {
        val navigation = documentTracker.navigation ?: return
        view.postVisualStateCallback(
            navigation.navigationId,
            object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    showReadyDocument(view, navigation)
                }
            },
        )
    }

    private fun showReadyDocument(view: WebView, navigation: SessionPageNavigation) {
        if (isClosed || !documentTracker.markDocumentVisible(navigation)) return
        view.visibility = View.VISIBLE
        callbacks.onPageLoadChanged(SessionPageLoad.Ready(navigation))
    }

    private fun hideErrorDocument(view: WebView) {
        // Compose가 그려지기 전에 기본 오류 문서를 숨깁니다. 보안 오류는 별도 대응 범위입니다.
        if (failure?.error == SessionLoadError.SECURITY) {
            view.visibility = View.VISIBLE
        } else if (!documentTracker.hasVisibleDocument) {
            view.visibility = View.INVISIBLE
        }
    }

    private fun reportNavigationFinished(view: WebView) {
        if (!documentTracker.hasFinished || documentTracker.isSuccessful) return
        val current = failure ?: return
        // 실패 callback 이후 history가 바뀔 수 있어 완료 시점에 다시 확인합니다.
        failure = current.copy(canGoBack = view.canGoBack(), canGoForward = view.canGoForward())
        reportFailure()
    }

    private fun reportFailure() {
        callbacks.onPageLoadChanged(SessionPageLoad.Failed(checkNotNull(failure)))
    }

    fun onDownloadRequested(url: String): Boolean {
        if (isClosed || !downloadSourceTracker.consumeConfirmedMainFrame(url)) return false
        // 다른 탐색 중에 도착한 다운로드 결과는 현재 본문 대기를 끝내지 않습니다.
        if (documentTracker.navigation?.url == url) {
            reportNavigationStopped()
        }
        return true
    }

    private fun reportNavigationStopped() {
        val navigation = documentTracker.stop() ?: return
        failure = null
        callbacks.onPageLoadChanged(SessionPageLoad.Stopped(navigation))
    }

    private fun startNavigation(url: String): SessionPageNavigation {
        failure = null
        return documentTracker.start(url).also(::reportLoading)
    }

    private fun reportLoading(navigation: SessionPageNavigation) {
        callbacks.onPageLoadChanged(SessionPageLoad.Loading(navigation))
    }
}

private data class MainFrameRequest(
    val url: String,
    val method: String,
)

/** 앱 명령이나 탐색 판정에서 먼저 만든 탐색과, Browser에 Loading을 이미 전달했는지를 나타냅니다. */
private class RequestedNavigation(
    val navigation: SessionPageNavigation,
    val isReported: Boolean,
)

private fun Int.toLoadError(): SessionLoadError = when (this) {
    WebViewClient.ERROR_FAILED_SSL_HANDSHAKE, WebViewClient.ERROR_UNSAFE_RESOURCE -> SessionLoadError.SECURITY
    WebViewClient.ERROR_BAD_URL, WebViewClient.ERROR_UNSUPPORTED_SCHEME,
    WebViewClient.ERROR_FILE, WebViewClient.ERROR_FILE_NOT_FOUND,
    -> SessionLoadError.UNAVAILABLE
    WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_HOST_LOOKUP,
    WebViewClient.ERROR_IO, WebViewClient.ERROR_TIMEOUT,
    -> SessionLoadError.CONNECTION
    else -> SessionLoadError.UNKNOWN
}
