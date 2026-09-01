package com.laxotters.clipy.feature.session.webview

import com.laxotters.clipy.feature.session.ROOT_SCROLL_IDLE_DELAY_MILLIS
import kotlin.math.abs

/**
 * WebView root 이동 중 touch drag와 이어지는 fling만 사용자 scroll로 선별합니다.
 * JS, anchor, layout, 구성·크기 변경이 만든 이동은 제외합니다.
 */
internal class UserRootScrollTracker(
    touchSlopPx: Int,
    minimumFlingVelocityPxPerSecond: Int,
    private val idleTimeoutMillis: Long = ROOT_SCROLL_IDLE_DELAY_MILLIS,
) {
    private var touchSlopPx = touchSlopPx.coerceAtLeast(1)
    private var minimumFlingVelocityPxPerSecond = minimumFlingVelocityPxPerSecond.coerceAtLeast(1)
    private var initialTouchY = 0f
    private var isTouchActive = false
    private var hasExceededTouchSlop = false
    private var hasUserRootScroll = false
    private var lastRootScrollTimeMillis: Long? = null

    var isFlingActive: Boolean = false
        private set

    fun onTouchDown(y: Float) {
        reset()
        initialTouchY = y
        isTouchActive = true
    }

    fun onTouchMove(y: Float) {
        if (isTouchActive && abs(y - initialTouchY) > touchSlopPx) {
            hasExceededTouchSlop = true
        }
    }

    fun onTouchUp(
        yVelocityPxPerSecond: Float,
        eventTimeMillis: Long,
    ) {
        if (!isTouchActive) {
            return
        }

        isTouchActive = false
        // 실제 root 이동을 사용자 drag로 한 번 이상 판별한 경우에만
        // release 속도로 fling을 시작합니다.
        isFlingActive = hasUserRootScroll &&
            abs(yVelocityPxPerSecond) >= minimumFlingVelocityPxPerSecond

        if (isFlingActive) {
            lastRootScrollTimeMillis = eventTimeMillis
        } else {
            reset()
        }
    }

    fun onTouchCancel() {
        reset()
    }

    fun shouldForwardRootScroll(eventTimeMillis: Long): Boolean {
        val isUserDrag = isTouchActive && hasExceededTouchSlop
        val isActiveFling = isFlingActive && !hasFlingTimedOut(eventTimeMillis)

        if (!isUserDrag && !isActiveFling) {
            if (isFlingActive) {
                reset()
            }
            return false
        }

        hasUserRootScroll = true
        lastRootScrollTimeMillis = eventTimeMillis
        return true
    }

    fun onFlingTrackingTimeout() {
        if (!isTouchActive) {
            reset()
        }
    }

    fun updateConfiguration(
        touchSlopPx: Int,
        minimumFlingVelocityPxPerSecond: Int,
    ) {
        reset()
        this.touchSlopPx = touchSlopPx.coerceAtLeast(1)
        this.minimumFlingVelocityPxPerSecond = minimumFlingVelocityPxPerSecond.coerceAtLeast(1)
    }

    fun reset() {
        initialTouchY = 0f
        isTouchActive = false
        hasExceededTouchSlop = false
        hasUserRootScroll = false
        isFlingActive = false
        lastRootScrollTimeMillis = null
    }

    private fun hasFlingTimedOut(eventTimeMillis: Long): Boolean {
        val lastScrollTimeMillis = lastRootScrollTimeMillis ?: return true
        return eventTimeMillis - lastScrollTimeMillis > idleTimeoutMillis
    }
}
