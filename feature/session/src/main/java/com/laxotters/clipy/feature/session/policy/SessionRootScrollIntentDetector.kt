package com.laxotters.clipy.feature.session.policy

import kotlin.math.abs

internal enum class SessionRootScrollDirection {
    UP,
    DOWN,
}

/**
 * WebView root scroll 콜백에서 들어오는 작은 delta를 누적해 사용자의 스크롤 방향을 확정합니다.
 *
 * thresholdPx는 호출자가 전달합니다.
 * Detector는 누적 delta가 thresholdPx에 도달하기 전에는 방향을 내지 않습니다.
 * 방향이 바뀌면 이전 누적값을 버리고, 상태 전이는 [SessionChromeStatePolicy]에서 판단합니다.
 */
internal class SessionRootScrollIntentDetector {
    private var accumulatedDeltaY = 0

    fun detectDirection(
        deltaY: Int,
        thresholdPx: Int,
    ): SessionRootScrollDirection? {
        if (deltaY == 0) {
            return null
        }

        val effectiveThresholdPx = thresholdPx.coerceAtLeast(1)

        if (accumulatedDeltaY.isOppositeDirectionOf(deltaY)) {
            accumulatedDeltaY = 0
        }

        accumulatedDeltaY += deltaY

        if (abs(accumulatedDeltaY) < effectiveThresholdPx) {
            return null
        }

        val direction = if (accumulatedDeltaY > 0) {
            SessionRootScrollDirection.DOWN
        } else {
            SessionRootScrollDirection.UP
        }

        accumulatedDeltaY = 0
        return direction
    }

    fun reset() {
        accumulatedDeltaY = 0
    }
}

private fun Int.isOppositeDirectionOf(other: Int): Boolean =
    this != 0 && other != 0 && this.sign != other.sign

private val Int.sign: Int
    get() = when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }
