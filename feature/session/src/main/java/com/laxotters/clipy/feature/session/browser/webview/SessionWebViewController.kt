package com.laxotters.clipy.feature.session.browser.webview

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.webview.client.SessionWebViewClient

@Stable
class SessionWebViewController internal constructor() {
    private var connection: Connection? = null

    fun goBack() {
        connection?.webView?.takeIf { it.canGoBack() }?.goBack()
    }

    fun goForward() {
        connection?.webView?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reload() {
        val current = connection ?: return
        current.client.reload(current.webView)
    }

    internal fun attach(webView: WebView, webViewId: Long, webViewClient: SessionWebViewClient) {
        connection = Connection(webView, webViewId, webViewClient)
    }

    internal fun retry(navigation: SessionPageNavigation): Boolean {
        val current = connection?.takeIf { it.webViewId == navigation.webViewId } ?: return false
        current.client.retry(current.webView, navigation.url)
        return true
    }

    internal fun currentPage(): PageInfo? {
        val view = connection?.webView ?: return null
        val url = view.url ?: return null
        return PageInfo(url, view.canGoBack(), view.canGoForward())
    }

    internal fun goBack(webViewId: Long): Boolean {
        val view = connection?.takeIf { it.webViewId == webViewId }?.webView ?: return false
        if (!view.canGoBack()) return false
        view.goBack()
        return true
    }

    internal fun detach(webView: WebView) {
        if (connection?.webView === webView) {
            connection = null
        }
    }

    private class Connection(
        val webView: WebView,
        val webViewId: Long,
        val client: SessionWebViewClient,
    )
}

@Composable
fun rememberSessionWebViewController(): SessionWebViewController =
    remember { SessionWebViewController() }
