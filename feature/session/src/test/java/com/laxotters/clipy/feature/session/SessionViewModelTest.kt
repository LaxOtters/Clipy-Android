package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.domain.model.BottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionViewModelTest {
    @Test
    fun initialState_createViewModel_hasInitialUiState() {
        val viewModel = SessionViewModel()
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
        assertNull(state.initialUrl)
    }

    @Test
    fun screenEntered_dispatchEvent_resolvesInitialUrl() {
        val viewModel = SessionViewModel()

        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))
        val state = viewModel.state.value

        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
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
    }

    @Test
    fun pageLoaded_matchingSessionId_updatesPageStateKeepsInitialUrl() {
        val viewModel = SessionViewModel()
        viewModel.dispatch(SessionUiEvent.ScreenEntered(sessionId = "session-1"))

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
        assertEquals(false, state.canGoBack)
        assertEquals(false, state.canGoForward)
    }

    @Test
    fun bottomSheetValueChanged_dispatchEvent_updatesBottomSheetState() {
        val viewModel = SessionViewModel()
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
            assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
        }
    }
}
