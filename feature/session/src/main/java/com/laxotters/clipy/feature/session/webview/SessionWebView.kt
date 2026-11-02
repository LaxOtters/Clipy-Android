package com.laxotters.clipy.feature.session.webview

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
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogManager
import com.laxotters.clipy.feature.session.webview.client.SessionWebChromeClient
import com.laxotters.clipy.feature.session.webview.client.SessionWebViewClient
import com.laxotters.clipy.feature.session.webview.scroll.RootScrollWebView

/**
 * Session의 단일 Main WebView를 렌더링하고 페이지·방문 기록·root scroll 상태를 호출자에게 전달합니다.
 * 실제 WebView release 시 대기 중인 JavaScript Dialog 결과를 취소합니다.
 */
@Composable
internal fun SessionWebView(
    url: String?,
    controller: SessionWebViewController,
    dialogManager: ClipyJsDialogManager,
    onPageStateChanged: (url: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onRootScrolled: (deltaY: Int, scrollableDistance: Int, viewportHeight: Int, touchSlopPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPageStateChanged by rememberUpdatedState(onPageStateChanged)
    val currentOnRootScrolled by rememberUpdatedState(onRootScrolled)
    val dialogStrings = sessionWebDialogStrings()
    val loadState = remember { SessionWebViewLoadState() }
    val callbacks = remember {
        SessionWebViewCallbacks(
            onPageFinished = { pageInfo ->
                currentOnPageStateChanged(
                    pageInfo.url,
                    pageInfo.canGoBack,
                    pageInfo.canGoForward,
                )
            },
            onRootScrolled = { metrics ->
                currentOnRootScrolled(
                    metrics.deltaY,
                    metrics.scrollableDistance,
                    metrics.viewportHeight,
                    metrics.touchSlopPx,
                )
            },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val coordinator = SessionWebDialogCoordinator(dialogManager, dialogStrings)
            SessionWebViewContainer(context, coordinator, controller).apply {
                webView.configureSessionWebView(
                    callbacks = callbacks,
                    dialogCoordinator = coordinator,
                )
                webView.onRootScrolled = callbacks.onRootScrolled
            }
        },
        update = { container ->
            container.dialogCoordinator.updateStrings(dialogStrings)
            controller.attach(container.webView)

            // 전달된 URL이 바뀔 때만 새 탐색을 시작해 기존 WebView 상태를 유지합니다.
            if (url != null && loadState.loadedUrl != url) {
                loadState.loadedUrl = url
                container.webView.loadUrl(url)
            }
        },
        onRelease = { container ->
            container.releaseAction.release()
        },
    )
}

private class SessionWebViewLoadState {
    var loadedUrl: String? = null
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSessionWebView(
    callbacks: SessionWebViewCallbacks,
    dialogCoordinator: SessionWebDialogCoordinator,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.setSupportMultipleWindows(false)
    settings.javaScriptCanOpenWindowsAutomatically = false
    webViewClient = SessionWebViewClient(callbacks)
    webChromeClient = SessionWebChromeClient(dialogCoordinator)
}

// AndroidView.factory에서만 생성하며, WebView별 종료에 필요한 의존성을 함께 받습니다.
@SuppressLint("ViewConstructor")
private class SessionWebViewContainer(
    context: Context,
    val dialogCoordinator: SessionWebDialogCoordinator,
    controller: SessionWebViewController,
) : FrameLayout(context) {
    val webView: RootScrollWebView = RootScrollWebView(context)
    val releaseAction = SessionWebViewRelease(
        closeDialog = dialogCoordinator::close,
        detachController = { controller.detach(webView) },
        releaseCallbacks = {
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
