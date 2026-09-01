package com.laxotters.clipy.feature.session.policy

import kotlin.math.abs

internal enum class SessionRootScrollDirection {
    UP,
    DOWN,
}

/**
 * 사용자 root scroll delta를 누적해 chrome 전이에 사용할 방향을 확정합니다.
 * 입력 출처 판별은 WebView adapter가 담당합니다.
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
