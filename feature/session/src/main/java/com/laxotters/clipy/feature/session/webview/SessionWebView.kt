package com.laxotters.clipy.feature.session.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SessionWebView(
    url: String?,
    controller: SessionWebViewController,
    onPageStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPageStateChanged by rememberUpdatedState(onPageStateChanged)
    val loadState = remember { SessionWebViewLoadState() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SessionWebViewContainer(context).apply {
                webView.configureSessionWebView(
                    onPageStateChanged = { pageUrl, canGoBack, canGoForward ->
                        currentOnPageStateChanged(pageUrl, canGoBack, canGoForward)
                    },
                )
            }
        },
        update = { container ->
            controller.attach(container.webView)

            // update는 recomposition마다 호출될 수 있어 같은 URL 중복 로딩을 막습니다.
            if (url != null && loadState.loadedUrl != url) {
                loadState.loadedUrl = url
                container.webView.loadUrl(url)
            }
        },
        onRelease = { container ->
            controller.detach(container.webView)
            container.removeAllViews()
            container.webView.destroy()
        },
    )
}

private class SessionWebViewLoadState {
    var loadedUrl: String? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSessionWebView(
    onPageStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            onPageStateChanged(
                url.orEmpty(),
                view.canGoBack(),
                view.canGoForward(),
            )
        }
    }
}

private class SessionWebViewContainer(context: Context) : FrameLayout(context) {
    val webView: WebView = WebView(context)

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
