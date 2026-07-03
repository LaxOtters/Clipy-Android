package com.laxotters.clipy.feature.session.policy

import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.SessionTopBarState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SessionChromeStatePolicyTest {
    // Top Bar fold action
    @Test
    fun topBarFoldClicked_browsingStates_toggleHiddenAndMinimized() {
        val restoredSnapshot = SessionChromeStatePolicy.onTopBarFoldClicked(
            snapshot(BottomSheetState.HIDDEN, SessionTopBarState.FOLDED),
        )
        val hiddenSnapshot = SessionChromeStatePolicy.onTopBarFoldClicked(
            snapshot(BottomSheetState.MINIMIZED, SessionTopBarState.UNFOLDED),
        )

        assertEquals(BottomSheetState.MINIMIZED, restoredSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, restoredSnapshot.topBarState)
        assertEquals(BottomSheetState.HIDDEN, hiddenSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.FOLDED, hiddenSnapshot.topBarState)
    }

    @Test
    fun topBarFoldClicked_mixedBrowsingStates_followTopBarDirection() {
        val minimizedSnapshot = SessionChromeStatePolicy.onTopBarFoldClicked(
            snapshot(BottomSheetState.MINIMIZED, SessionTopBarState.FOLDED),
        )
        val hiddenSnapshot = SessionChromeStatePolicy.onTopBarFoldClicked(
            snapshot(BottomSheetState.HIDDEN, SessionTopBarState.UNFOLDED),
        )

        assertEquals(BottomSheetState.MINIMIZED, minimizedSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, minimizedSnapshot.topBarState)
        assertEquals(BottomSheetState.HIDDEN, hiddenSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.FOLDED, hiddenSnapshot.topBarState)
    }

    @Test
    fun topBarFoldClicked_peek_togglesOnlyTopBar() {
        val foldedPeek = SessionChromeStatePolicy.onTopBarFoldClicked(
            snapshot(BottomSheetState.PEEK, SessionTopBarState.UNFOLDED),
        )

        assertEquals(BottomSheetState.PEEK, foldedPeek.bottomSheetState)
        assertEquals(SessionTopBarState.FOLDED, foldedPeek.topBarState)
    }

    // Page load callback
    @Test
    fun pageLoaded_browsingStates_restoreMinimized() {
        val minimizedFromHidden = SessionChromeStatePolicy.onPageLoaded(
            snapshot(BottomSheetState.HIDDEN, SessionTopBarState.FOLDED),
        )
        val minimized = snapshot(BottomSheetState.MINIMIZED, SessionTopBarState.UNFOLDED)
        val keptMinimized = SessionChromeStatePolicy.onPageLoaded(minimized)

        assertEquals(BottomSheetState.MINIMIZED, minimizedFromHidden.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, minimizedFromHidden.topBarState)
        assertSame(minimized, keptMinimized)
    }

    @Test
    fun pageLoaded_comparingStates_doNotAutoChange() {
        val peek = snapshot(BottomSheetState.PEEK, SessionTopBarState.FOLDED)
        val expanded = snapshot(BottomSheetState.EXPANDED, SessionTopBarState.UNFOLDED)

        assertSame(peek, SessionChromeStatePolicy.onPageLoaded(peek))
        assertSame(expanded, SessionChromeStatePolicy.onPageLoaded(expanded))
    }

    // WebView root scroll
    @Test
    fun rootScrollIntent_browsingStates_followDirection() {
        val hiddenSnapshot = SessionChromeStatePolicy.onRootScrollIntent(
            current = snapshot(BottomSheetState.MINIMIZED, SessionTopBarState.UNFOLDED),
            direction = SessionRootScrollDirection.DOWN,
            scrollableDistance = 120,
            viewportHeight = 400,
        )
        val alreadyHidden = snapshot(BottomSheetState.HIDDEN, SessionTopBarState.FOLDED)
        val keptHiddenSnapshot = SessionChromeStatePolicy.onRootScrollIntent(
            current = alreadyHidden,
            direction = SessionRootScrollDirection.DOWN,
            scrollableDistance = 40,
            viewportHeight = 400,
        )
        val minimizedSnapshot = SessionChromeStatePolicy.onRootScrollIntent(
            current = snapshot(BottomSheetState.HIDDEN, SessionTopBarState.FOLDED),
            direction = SessionRootScrollDirection.UP,
            scrollableDistance = 40,
            viewportHeight = 400,
        )

        assertEquals(BottomSheetState.HIDDEN, hiddenSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.FOLDED, hiddenSnapshot.topBarState)
        assertSame(alreadyHidden, keptHiddenSnapshot)
        assertEquals(BottomSheetState.MINIMIZED, minimizedSnapshot.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, minimizedSnapshot.topBarState)
    }

    @Test
    fun rootScrollIntent_minimizedWithSmallScrollableArea_doesNotHide() {
        val minimized = snapshot(BottomSheetState.MINIMIZED, SessionTopBarState.UNFOLDED)

        val result = SessionChromeStatePolicy.onRootScrollIntent(
            current = minimized,
            direction = SessionRootScrollDirection.DOWN,
            scrollableDistance = 79,
            viewportHeight = 400,
        )

        assertSame(minimized, result)
    }

    @Test
    fun rootScrollIntent_comparingStates_doNotAutoChange() {
        val peek = snapshot(BottomSheetState.PEEK, SessionTopBarState.UNFOLDED)
        val expanded = snapshot(BottomSheetState.EXPANDED, SessionTopBarState.UNFOLDED)

        val peekResult = SessionChromeStatePolicy.onRootScrollIntent(
            current = peek,
            direction = SessionRootScrollDirection.DOWN,
            scrollableDistance = 120,
            viewportHeight = 400,
        )
        val expandedResult = SessionChromeStatePolicy.onRootScrollIntent(
            current = expanded,
            direction = SessionRootScrollDirection.DOWN,
            scrollableDistance = 120,
            viewportHeight = 400,
        )

        assertEquals(peek, peekResult)
        assertEquals(expanded, expandedResult)
    }

    // Android system back
    @Test
    fun collapseSheetForBack_expandedOnlyReturnsPeek() {
        assertEquals(
            BottomSheetState.PEEK,
            SessionChromeStatePolicy.collapseSheetForBack(BottomSheetState.EXPANDED),
        )
        assertNull(SessionChromeStatePolicy.collapseSheetForBack(BottomSheetState.PEEK))
        assertNull(SessionChromeStatePolicy.collapseSheetForBack(BottomSheetState.MINIMIZED))
        assertNull(SessionChromeStatePolicy.collapseSheetForBack(BottomSheetState.HIDDEN))
    }

    private fun snapshot(
        bottomSheetState: BottomSheetState,
        topBarState: SessionTopBarState,
    ): SessionChromeSnapshot =
        SessionChromeSnapshot(
            bottomSheetState = bottomSheetState,
            topBarState = topBarState,
        )
}
