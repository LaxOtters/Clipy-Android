package com.laxotters.clipy.feature.session.webview

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.laxotters.clipy.feature.session.DEFAULT_HOME_URL

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SessionWebView(
    initialUrl: String?,
    controller: SessionWebViewController,
    onPageStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeInitialUrl = initialUrl.takeUnless { it.isNullOrBlank() } ?: DEFAULT_HOME_URL

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        onPageStateChanged(
                            url.orEmpty().ifBlank { DEFAULT_HOME_URL },
                            view.canGoBack(),
                            view.canGoForward(),
                        )
                    }
                }
                controller.attach(this)
                loadUrl(safeInitialUrl)
            }
        },
        update = { webView ->
            controller.attach(webView)
        },
    )
}
