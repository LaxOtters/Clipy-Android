package com.laxotters.clipy.feature.session.browser.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDocumentTrackerTest {
    @Test
    fun firstFailure_finishedCallback_doesNotProveSuccessfulDocument() {
        val tracker = SessionDocumentTracker(1)
        tracker.start("failed")
        tracker.fail("failed")
        tracker.finish("failed")
        assertFalse(tracker.hasVisibleDocument)
        assertFalse(tracker.isSuccessful)
    }

    @Test
    fun normalDocument_commitAndFinishInEitherOrder_proveSuccess() {
        val tracker = SessionDocumentTracker(1)
        tracker.start("first")
        tracker.finish("first")
        assertFalse(tracker.isSuccessful)
        tracker.commit("first")
        assertTrue(tracker.isSuccessful)
        tracker.start("second")
        tracker.commit("second")
        assertFalse(tracker.isSuccessful)
        tracker.finish("second")
        assertTrue(tracker.isSuccessful)
    }

    @Test
    fun nextNavigation_startAndFailure_keepPreviousDocumentUntilReplacement() {
        val tracker = SessionDocumentTracker(1)
        tracker.start("first")
        tracker.commit("first")
        tracker.finish("first")
        tracker.markDocumentVisible(checkNotNull(tracker.navigation))
        tracker.start("failed")
        assertTrue(tracker.hasVisibleDocument)
        tracker.fail("failed")
        assertTrue(tracker.hasVisibleDocument)
        tracker.commit("failed")
        assertFalse(tracker.hasVisibleDocument)
        tracker.finish("failed")
        assertFalse(tracker.isSuccessful)
    }

    @Test
    fun newDocument_failureAfterCommit_doesNotReuseOldDocument() {
        val tracker = SessionDocumentTracker(1)
        tracker.start("new")
        tracker.commit("new")
        tracker.fail("new")
        assertFalse(tracker.hasVisibleDocument)
    }

    @Test
    fun newerOrReloadedNavigation_doesNotReuseOldCallbacksOrSuccess() {
        val tracker = SessionDocumentTracker(1)
        val old = tracker.start("old")
        val current = tracker.start("current")
        assertNotEquals(old, current)
        assertFalse(tracker.fail("old"))
        assertFalse(tracker.commit("old"))
        assertFalse(tracker.finish("old"))
        tracker.commit("current")
        tracker.finish("current")
        assertTrue(tracker.isSuccessful)
        val reload = tracker.start("current")
        assertNotEquals(current, reload)
        assertFalse(tracker.isSuccessful)
    }

    @Test
    fun actualRelease_discardsDocumentEvidence() {
        val tracker = SessionDocumentTracker(1)
        tracker.start("page")
        tracker.commit("page")
        tracker.close()
        assertFalse(tracker.hasVisibleDocument)
        assertNull(tracker.navigation)
        assertFalse(SessionDocumentTracker(2).hasVisibleDocument)
    }
}
