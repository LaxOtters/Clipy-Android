package com.laxotters.clipy.feature.session.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.laxotters.clipy.feature.session.ROOT_SCROLL_IDLE_DELAY_MILLIS

/**
 * Session의 단일 Main WebView를 렌더링하고 페이지·방문 기록·root scroll 상태를 호출자에게 전달합니다.
 */
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
            SessionWebViewContainer(context).apply {
                webView.configureSessionWebView(callbacks)
                webView.onRootScrolled = callbacks.onRootScrolled
            }
        },
        update = { container ->
            controller.attach(container.webView)

            // 전달된 URL이 바뀔 때만 새 탐색을 시작해 기존 WebView 상태를 유지합니다.
            if (url != null && loadState.loadedUrl != url) {
                loadState.loadedUrl = url
                container.webView.loadUrl(url)
            }
        },
        onRelease = { container ->
            controller.detach(container.webView)
            container.webView.onRootScrolled = null
            container.webView.release()
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
    callbacks: SessionWebViewCallbacks,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.setSupportMultipleWindows(false)
    settings.javaScriptCanOpenWindowsAutomatically = false
    webViewClient = SessionWebViewClient(callbacks)
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

private typealias RootScrollCallback = (RootScrollMetrics) -> Unit

/** touch event와 root 이동을 연결해 사용자 drag·fling의 delta만 전달합니다. */
private class RootScrollWebView(context: Context) : WebView(context) {
    var onRootScrolled: RootScrollCallback? = null
    private var touchSlopPx = currentViewConfiguration().scaledTouchSlop
    private val userRootScrollTracker = UserRootScrollTracker(
        touchSlopPx = touchSlopPx,
        minimumFlingVelocityPxPerSecond = currentViewConfiguration().scaledMinimumFlingVelocity,
    )
    private var velocityTracker: VelocityTracker? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private val flingTrackingTimeout = Runnable {
        userRootScrollTracker.onFlingTrackingTimeout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        resetUserRootScrollTracking()
        super.onConfigurationChanged(newConfig)
        refreshInputConfiguration()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int,
    ) {
        resetUserRootScrollTracking()
        super.onSizeChanged(width, height, oldWidth, oldHeight)
    }

    override fun onDetachedFromWindow() {
        resetUserRootScrollTracking()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startTouchTracking(event)
            else -> velocityTracker?.addMovement(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            userRootScrollTracker.onTouchMove(event.y)
        }

        // 사용자 scroll 판별에 필요한 입력만 관찰합니다.
        // WebView의 touch 처리 결과는 그대로 반환합니다.
        val handled = super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> finishTouchTracking(event.eventTime)
            MotionEvent.ACTION_CANCEL -> resetUserRootScrollTracking()
        }

        return handled
    }

    override fun onScrollChanged(
        left: Int,
        top: Int,
        oldLeft: Int,
        oldTop: Int,
    ) {
        super.onScrollChanged(left, top, oldLeft, oldTop)
        val deltaY = top - oldTop

        if (
            deltaY != 0 &&
            userRootScrollTracker.shouldForwardRootScroll(SystemClock.uptimeMillis())
        ) {
            val viewportHeight = computeVerticalScrollExtent().coerceAtLeast(0)
            val scrollableDistance = (computeVerticalScrollRange() - viewportHeight)
                .coerceAtLeast(0)
            onRootScrolled?.invoke(
                RootScrollMetrics(
                    deltaY = deltaY,
                    scrollableDistance = scrollableDistance,
                    viewportHeight = viewportHeight,
                    touchSlopPx = touchSlopPx,
                ),
            )

            if (userRootScrollTracker.isFlingActive) {
                scheduleFlingTrackingTimeout()
            }
        }
    }

    fun release() {
        resetUserRootScrollTracking()
    }

    private fun startTouchTracking(event: MotionEvent) {
        resetUserRootScrollTracking()
        activePointerId = event.getPointerId(0)
        velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
        userRootScrollTracker.onTouchDown(event.y)
    }

    private fun finishTouchTracking(eventTimeMillis: Long) {
        velocityTracker?.computeCurrentVelocity(MILLIS_PER_SECOND)
        val yVelocity = velocityTracker?.getYVelocity(activePointerId) ?: 0f
        velocityTracker?.recycle()
        velocityTracker = null
        activePointerId = MotionEvent.INVALID_POINTER_ID

        userRootScrollTracker.onTouchUp(
            yVelocityPxPerSecond = yVelocity,
            eventTimeMillis = eventTimeMillis,
        )
        if (userRootScrollTracker.isFlingActive) {
            scheduleFlingTrackingTimeout()
        }
    }

    private fun scheduleFlingTrackingTimeout() {
        // WebView가 fling 종료를 알리지 않습니다.
        // 마지막 root scroll 후 기준 시간이 지나면 fling 귀속을 종료합니다.
        removeCallbacks(flingTrackingTimeout)
        postDelayed(flingTrackingTimeout, ROOT_SCROLL_IDLE_DELAY_MILLIS)
    }

    private fun resetUserRootScrollTracking() {
        removeCallbacks(flingTrackingTimeout)
        velocityTracker?.recycle()
        velocityTracker = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
        userRootScrollTracker.reset()
    }

    private fun refreshInputConfiguration() {
        val viewConfiguration = currentViewConfiguration()
        touchSlopPx = viewConfiguration.scaledTouchSlop
        userRootScrollTracker.updateConfiguration(
            touchSlopPx = touchSlopPx,
            minimumFlingVelocityPxPerSecond = viewConfiguration.scaledMinimumFlingVelocity,
        )
    }

    private fun currentViewConfiguration(): ViewConfiguration =
        ViewConfiguration.get(context)

    private companion object {
        const val MILLIS_PER_SECOND = 1_000
    }
}
