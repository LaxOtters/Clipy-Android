package com.laxotters.clipy.feature.session.webview

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class SessionWebViewController internal constructor() {
    private var webView: WebView? = null

    fun goBack() {
        webView?.takeIf { it.canGoBack() }?.goBack()
    }

    fun goForward() {
        webView?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reload() {
        webView?.reload()
    }

    internal fun attach(webView: WebView) {
        this.webView = webView
    }

    internal fun detach(webView: WebView) {
        if (this.webView === webView) {
            this.webView = null
        }
    }
}

@Composable
fun rememberSessionWebViewController(): SessionWebViewController =
    remember { SessionWebViewController() }
