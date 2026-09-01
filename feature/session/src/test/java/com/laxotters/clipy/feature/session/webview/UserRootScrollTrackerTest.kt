package com.laxotters.clipy.feature.session.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRootScrollTrackerTest {
    @Test
    fun tap_withRootMovement_doesNotForwardScroll() {
        val tracker = tracker()

        tracker.onTouchDown(y = 100f)

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))

        tracker.onTouchUp(yVelocityPxPerSecond = 0f, eventTimeMillis = 20L)
        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 21L))
    }

    @Test
    fun movementBelowTouchSlop_withRootMovement_doesNotForwardScroll() {
        val tracker = tracker()

        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 91f)

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))
    }

    @Test
    fun programmaticRootMovement_withoutTouch_doesNotForwardScroll() {
        val tracker = tracker()

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))
    }

    @Test
    fun dragBeyondTouchSlop_withRootMovement_forwardsScroll() {
        val tracker = tracker()

        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 89f)

        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))
    }

    @Test
    fun fling_afterTouchRelease_forwardsUntilIdle() {
        val tracker = tracker()
        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 80f)
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))

        tracker.onTouchUp(yVelocityPxPerSecond = 101f, eventTimeMillis = 20L)

        assertTrue(tracker.isFlingActive)
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 170L))
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 320L))
    }

    @Test
    fun rootMovement_afterFlingIdle_doesNotForwardScroll() {
        val tracker = tracker()
        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 80f)
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))
        tracker.onTouchUp(yVelocityPxPerSecond = 101f, eventTimeMillis = 20L)

        tracker.onFlingTrackingTimeout()

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 21L))
    }

    @Test
    fun configurationUpdate_duringFling_ignoresFollowingCorrectionMovement() {
        val tracker = tracker()
        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 80f)
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))
        tracker.onTouchUp(yVelocityPxPerSecond = 101f, eventTimeMillis = 20L)

        tracker.updateConfiguration(
            touchSlopPx = 20,
            minimumFlingVelocityPxPerSecond = 200,
        )

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 21L))
    }

    @Test
    fun repeatedReset_keepsFollowingMovementIgnored() {
        val tracker = tracker()
        tracker.onTouchDown(y = 100f)
        tracker.onTouchMove(y = 80f)
        assertTrue(tracker.shouldForwardRootScroll(eventTimeMillis = 10L))

        tracker.reset()
        tracker.reset()

        assertFalse(tracker.shouldForwardRootScroll(eventTimeMillis = 11L))
    }

    private fun tracker() = UserRootScrollTracker(
        touchSlopPx = 10,
        minimumFlingVelocityPxPerSecond = 100,
    )
}
