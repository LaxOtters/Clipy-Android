package com.laxotters.clipy.core.ui.extension

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

private const val DEFAULT_THROTTLE_INTERVAL_MILLIS = 500L

/**
 * 첫 클릭은 즉시 처리하고, 이후 [intervalMillis] 동안 발생한 클릭은 무시합니다.
 */
@Composable
fun rememberThrottledClick(
    intervalMillis: Long = DEFAULT_THROTTLE_INTERVAL_MILLIS,
    onClick: () -> Unit,
): () -> Unit {
    val currentOnClick = rememberUpdatedState(onClick)
    val clickThrottle = remember(intervalMillis) {
        ClickThrottle(intervalMillis)
    }

    return remember(clickThrottle) {
        {
            if (clickThrottle.tryAccept()) {
                currentOnClick.value()
            }
        }
    }
}

internal class ClickThrottle(
    private val intervalMillis: Long,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastClickMillis: Long? = null

    init {
        require(intervalMillis >= 0) {
            "intervalMillis must not be negative."
        }
    }

    fun tryAccept(): Boolean {
        val now = nowMillis()
        val lastClick = lastClickMillis

        if (lastClick != null && now - lastClick < intervalMillis) {
            return false
        }

        lastClickMillis = now
        return true
    }
}
