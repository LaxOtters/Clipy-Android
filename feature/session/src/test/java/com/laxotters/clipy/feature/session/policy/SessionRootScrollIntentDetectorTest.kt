package com.laxotters.clipy.feature.session.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRootScrollIntentDetectorTest {
    @Test
    fun detectDirection_sameDirectionDeltaAccumulatedToThreshold_returnsDirection() {
        val detector = SessionRootScrollIntentDetector()

        assertNull(detector.detectDirection(deltaY = 4, thresholdPx = 10))

        val direction = detector.detectDirection(deltaY = 6, thresholdPx = 10)

        assertEquals(SessionRootScrollDirection.DOWN, direction)
    }

    @Test
    fun detectDirection_oppositeDirection_discardPreviousDelta() {
        val detector = SessionRootScrollIntentDetector()

        assertNull(detector.detectDirection(deltaY = 8, thresholdPx = 10))
        assertNull(detector.detectDirection(deltaY = -3, thresholdPx = 10))

        val direction = detector.detectDirection(deltaY = -7, thresholdPx = 10)

        assertEquals(SessionRootScrollDirection.UP, direction)
    }

    @Test
    fun reset_afterAccumulatedDelta_discardsPreviousDelta() {
        val detector = SessionRootScrollIntentDetector()

        assertNull(detector.detectDirection(deltaY = 6, thresholdPx = 10))

        detector.reset()

        assertNull(detector.detectDirection(deltaY = 4, thresholdPx = 10))
    }
}
