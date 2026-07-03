package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.domain.model.BottomSheetState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Initial state
    @Test
    fun initialState_createViewModel_hasInitialUiState() {
        val viewModel = SessionViewModel()
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
        assertNull(state.initialUrl)
    }

    // Session entry
    @Test
    fun screenEntered_dispatchEvent_resolvesInitialUrl() {
        val viewModel = SessionViewModel()

        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        val state = viewModel.state.value

        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("google.com", state.currentUrlLabel)
    }

    @Test
    fun changedState_newSessionEntered_resetsToInitialUiState() {
        val viewModel = SessionViewModel()

        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.EXPANDED))
        viewModel.dispatch(
            SessionUiEvent.ScreenEntered(
                sessionId = "session-1",
            ),
        )
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("google.com", state.currentUrlLabel)
    }

    // Page load callback
    @Test
    fun pageLoaded_matchingSessionId_updatesPageStateAndRestoresBrowsingChrome() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.HIDDEN))

        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/page",
                canGoBack = true,
                canGoForward = true,
            ),
        )
        val state = viewModel.state.value

        assertEquals("https://example.com/page", state.currentUrl)
        assertEquals(true, state.canGoBack)
        assertEquals(true, state.canGoForward)
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("example.com", state.currentUrlLabel)
        assertEquals(BottomSheetState.MINIMIZED, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun pageLoaded_staleSessionId_keepsCurrentPageState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/current",
                canGoBack = true,
                canGoForward = false,
            ),
        )

        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-0",
                url = "https://stale.example.com/page",
                canGoBack = false,
                canGoForward = true,
            ),
        )
        val state = viewModel.state.value

        assertEquals("https://example.com/current", state.currentUrl)
        assertEquals("example.com", state.currentUrlLabel)
        assertEquals(true, state.canGoBack)
        assertEquals(false, state.canGoForward)
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
    }

    @Test
    fun sameSessionReentered_dispatchEvent_keepsResolvedState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/page",
                canGoBack = false,
                canGoForward = false,
            ),
        )

        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        val state = viewModel.state.value

        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("https://example.com/page", state.currentUrl)
        assertEquals("example.com", state.currentUrlLabel)
    }

    @Test
    fun differentSessionEntered_dispatchEvent_reResolvesState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/page",
                canGoBack = true,
                canGoForward = false,
            ),
        )

        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-2"))
        val state = viewModel.state.value

        assertEquals("session-2", state.sessionId)
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("", state.currentUrl)
        assertEquals("google.com", state.currentUrlLabel)
        assertEquals(false, state.canGoBack)
        assertEquals(false, state.canGoForward)
    }

    // Bottom Sheet direct manipulation
    @Test
    fun bottomSheetValueChanged_dispatchEvent_updatesBottomSheetStateAndPreservesTopBar() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val cases = listOf(
            ClipyBottomSheetValue.HIDDEN to BottomSheetState.HIDDEN,
            ClipyBottomSheetValue.MINIMIZED to BottomSheetState.MINIMIZED,
            ClipyBottomSheetValue.PEEK to BottomSheetState.PEEK,
            ClipyBottomSheetValue.EXPANDED to BottomSheetState.EXPANDED,
        )

        cases.forEach { (value, expectedState) ->
            viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(value))
            val state = viewModel.state.value

            assertEquals(expectedState, state.bottomSheetState)
            assertEquals(SessionTopBarState.FOLDED, state.topBarState)
        }
    }

    // Chrome transition events
    @Test
    fun topBarFoldClicked_dispatchEvent_updatesChromeState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.HIDDEN))

        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val state = viewModel.state.value

        assertEquals(BottomSheetState.MINIMIZED, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun topBarFoldClicked_whileWebViewRootScrolling_keepsChromeState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(
            SessionUiEvent.WebViewRootScrolled(
                deltaY = 12,
                scrollableDistance = 120,
                viewportHeight = 400,
                touchSlopPx = 12,
            ),
        )

        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun webViewRootScrolled_dispatchEvent_updatesBrowsingChromeState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.MINIMIZED))

        viewModel.dispatch(
            SessionUiEvent.WebViewRootScrolled(
                deltaY = 24,
                scrollableDistance = 120,
                viewportHeight = 400,
                touchSlopPx = 12,
            ),
        )
        val hiddenState = viewModel.state.value

        viewModel.dispatch(
            SessionUiEvent.WebViewRootScrolled(
                deltaY = -24,
                scrollableDistance = 40,
                viewportHeight = 400,
                touchSlopPx = 12,
            ),
        )
        val minimizedState = viewModel.state.value

        assertEquals(BottomSheetState.HIDDEN, hiddenState.bottomSheetState)
        assertEquals(SessionTopBarState.FOLDED, hiddenState.topBarState)
        assertEquals(BottomSheetState.MINIMIZED, minimizedState.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, minimizedState.topBarState)
    }

    @Test
    fun webViewRootScrolled_belowTouchSlop_keepsChromeState() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.MINIMIZED))

        viewModel.dispatch(
            SessionUiEvent.WebViewRootScrolled(
                deltaY = 11,
                scrollableDistance = 120,
                viewportHeight = 400,
                touchSlopPx = 12,
            ),
        )
        val state = viewModel.state.value

        assertEquals(BottomSheetState.MINIMIZED, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    // Android system back
    @Test
    fun systemBackPressed_expandedSheet_collapsesToPeek() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.EXPANDED))

        viewModel.dispatch(SessionUiEvent.SystemBackPressed)
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun systemBackPressed_withHistory_postsWebViewBackSideEffect() = runTest {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        viewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/page",
                canGoBack = true,
                canGoForward = false,
            ),
        )
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(SessionUiEvent.SystemBackPressed)

        assertEquals(SessionUiSideEffect.GoBackInWebView, effect.await())
    }

    @Test
    fun systemBackPressed_withoutHistory_postsNavigateToHomeSideEffect() = runTest {
        val viewModel = SessionViewModel()
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(SessionUiEvent.SystemBackPressed)

        assertEquals(SessionUiSideEffect.NavigateToHome, effect.await())
    }
}
