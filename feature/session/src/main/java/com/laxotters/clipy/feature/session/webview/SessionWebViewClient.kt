package com.laxotters.clipy.feature.session.webview

import android.webkit.WebView
import android.webkit.WebViewClient

internal class SessionWebViewClient(
    private val callbacks: SessionWebViewCallbacks,
) : WebViewClient() {
    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        callbacks.onPageFinished(
            PageInfo(
                url = url.orEmpty(),
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            ),
        )
    }
}
