package com.laxotters.clipy.feature.session.webview.client

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.laxotters.clipy.feature.session.webview.PageInfo
import com.laxotters.clipy.feature.session.webview.SessionWebViewCallbacks
import com.laxotters.clipy.feature.session.webview.download.DownloadSourceTracker
import com.laxotters.clipy.feature.session.webview.navigation.WebNavigationDecision
import com.laxotters.clipy.feature.session.webview.navigation.WebNavigationPolicy

internal class SessionWebViewClient(
    private val callbacks: SessionWebViewCallbacks,
    private val downloadSourceTracker: DownloadSourceTracker,
    private val onExternalAppRequested: (String) -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url.toString()
        downloadSourceTracker.recordNavigation(
            url = url,
            isForMainFrame = request.isForMainFrame,
        )
        return when (val decision = WebNavigationPolicy.decide(url, request.isForMainFrame)) {
            WebNavigationDecision.ContinueInWebView -> false
            is WebNavigationDecision.RequestExternalApp -> {
                onExternalAppRequested(decision.url)
                true
            }
        }
    }

    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        // 완료된 페이지의 탐색 근거가 이후 다운로드 판정에 재사용되지 않도록 초기화합니다.
        downloadSourceTracker.clear()
        callbacks.onPageFinished(
            PageInfo(
                url = url.orEmpty(),
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            ),
        )
    }

    fun consumeConfirmedMainFrameDownload(url: String): Boolean =
        downloadSourceTracker.consumeConfirmedMainFrame(url)
}
