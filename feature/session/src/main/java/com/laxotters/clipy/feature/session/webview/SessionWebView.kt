package com.laxotters.clipy.feature.session.webview

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewConfiguration
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
    onRootScrolled: (deltaY: Int, scrollableDistance: Int, viewportHeight: Int, touchSlopPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPageStateChanged by rememberUpdatedState(onPageStateChanged)
    val currentOnRootScrolled by rememberUpdatedState(onRootScrolled)
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
                webView.onRootScrolled = { deltaY, scrollableDistance, viewportHeight, touchSlopPx ->
                    currentOnRootScrolled(deltaY, scrollableDistance, viewportHeight, touchSlopPx)
                }
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
            container.webView.onRootScrolled = null
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
    val webView: RootScrollWebView = RootScrollWebView(context)

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

private typealias RootScrollCallback = (
    deltaY: Int,
    scrollableDistance: Int,
    viewportHeight: Int,
    touchSlopPx: Int,
) -> Unit

private class RootScrollWebView(context: Context) : WebView(context) {
    var onRootScrolled: RootScrollCallback? = null
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop

    override fun onScrollChanged(
        left: Int,
        top: Int,
        oldLeft: Int,
        oldTop: Int,
    ) {
        super.onScrollChanged(left, top, oldLeft, oldTop)
        val deltaY = top - oldTop

        if (deltaY != 0) {
            val viewportHeight = computeVerticalScrollExtent().coerceAtLeast(0)
            val scrollableDistance = (computeVerticalScrollRange() - viewportHeight)
                .coerceAtLeast(0)
            onRootScrolled?.invoke(
                deltaY,
                scrollableDistance,
                viewportHeight,
                touchSlopPx,
            )
        }
    }
}
