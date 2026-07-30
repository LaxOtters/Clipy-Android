package com.laxotters.clipy.core.ui.extension

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickThrottleTest {
    @Test
    fun withinInterval_tryAccept_returnsFalse() {
        var nowMillis = 0L
        val clickThrottle = ClickThrottle(
            intervalMillis = 500L,
            nowMillis = { nowMillis },
        )

        assertTrue(clickThrottle.tryAccept())

        nowMillis = 499L

        assertFalse(clickThrottle.tryAccept())
    }

    @Test
    fun afterInterval_tryAccept_returnsTrue() {
        var nowMillis = 0L
        val clickThrottle = ClickThrottle(
            intervalMillis = 500L,
            nowMillis = { nowMillis },
        )

        assertTrue(clickThrottle.tryAccept())

        nowMillis = 500L

        assertTrue(clickThrottle.tryAccept())
    }

    @Test
    fun suppressedClick_tryAccept_doesNotExtendInterval() {
        var nowMillis = 0L
        val clickThrottle = ClickThrottle(
            intervalMillis = 500L,
            nowMillis = { nowMillis },
        )

        assertTrue(clickThrottle.tryAccept())

        nowMillis = 300L
        assertFalse(clickThrottle.tryAccept())

        nowMillis = 500L
        assertTrue(clickThrottle.tryAccept())
    }
}
