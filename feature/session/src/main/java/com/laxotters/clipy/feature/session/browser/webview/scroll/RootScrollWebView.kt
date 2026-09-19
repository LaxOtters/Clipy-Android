package com.laxotters.clipy.feature.session.browser.webview.scroll

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.webkit.WebView
import com.laxotters.clipy.feature.session.ROOT_SCROLL_IDLE_DELAY_MILLIS
import com.laxotters.clipy.feature.session.browser.webview.RootScrollMetrics

/** 사용자 touch와 root 이동을 연결해 drag·fling으로 확인된 delta만 전달합니다. */
internal class RootScrollWebView(context: Context) : WebView(context) {
    var onRootScrolled: ((RootScrollMetrics) -> Unit)? = null
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

    // 별도 클릭 동작을 만들지 않으며 웹 콘텐츠의 클릭·접근성 처리는 WebView에 위임합니다.
    @SuppressLint("ClickableViewAccessibility")
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
