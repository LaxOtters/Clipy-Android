package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.domain.model.BottomSheetState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_routeIsInjected_hasMatchingSessionIdAndInitialUrl() {
        val viewModel = sessionViewModel(sessionId = "session-1")
        val state = viewModel.state.value

        assertEquals("session-1", state.sessionId)
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals("google.com", state.currentUrlLabel)
        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun differentRoutes_createIndependentViewModelState() {
        val firstViewModel = sessionViewModel(sessionId = "session-1")
        firstViewModel.dispatch(
            SessionUiEvent.PageLoaded(
                sessionId = "session-1",
                url = "https://example.com/page",
                canGoBack = true,
                canGoForward = false,
            ),
        )
        val secondViewModel = sessionViewModel(sessionId = "session-2")

        assertEquals("https://example.com/page", firstViewModel.state.value.currentUrl)
        assertEquals("session-2", secondViewModel.state.value.sessionId)
        assertEquals(DEFAULT_HOME_URL, secondViewModel.state.value.initialUrl)
        assertEquals("", secondViewModel.state.value.currentUrl)
        assertFalse(secondViewModel.state.value.canGoBack)
        assertFalse(secondViewModel.state.value.canGoForward)
    }

    @Test
    fun pageLoaded_matchingSessionId_updatesPageStateAndRestoresBrowsingChrome() {
        val viewModel = sessionViewModel(sessionId = "session-1")
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.HIDDEN))

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
        val viewModel = sessionViewModel(sessionId = "session-1")
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
    fun bottomSheetValueChanged_updatesBottomSheetStateAndPreservesTopBar() {
        val viewModel = sessionViewModel()
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val cases = listOf(
            BottomSheetValue.HIDDEN to BottomSheetState.HIDDEN,
            BottomSheetValue.MINIMIZED to BottomSheetState.MINIMIZED,
            BottomSheetValue.PEEK to BottomSheetState.PEEK,
            BottomSheetValue.EXPANDED to BottomSheetState.EXPANDED,
        )

        cases.forEach { (value, expectedState) ->
            viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(value))
            val state = viewModel.state.value

            assertEquals(expectedState, state.bottomSheetState)
            assertEquals(SessionTopBarState.FOLDED, state.topBarState)
        }
    }

    @Test
    fun topBarFoldClicked_updatesChromeState() {
        val viewModel = sessionViewModel()
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.HIDDEN))

        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val state = viewModel.state.value

        assertEquals(BottomSheetState.MINIMIZED, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun topBarFoldClicked_whileWebViewRootScrolling_keepsChromeState() {
        val viewModel = sessionViewModel()
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
    fun webViewRootScrolled_updatesBrowsingChromeState() {
        val viewModel = sessionViewModel()
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.MINIMIZED))

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
        val viewModel = sessionViewModel()
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.MINIMIZED))

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

    @Test
    fun systemBackPressed_expandedSheet_collapsesToPeek() {
        val viewModel = sessionViewModel()
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.EXPANDED))

        viewModel.dispatch(SessionUiEvent.SystemBackPressed)
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun systemBackPressed_withHistory_postsWebViewBackSideEffect() = runTest {
        val viewModel = sessionViewModel()
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
        val viewModel = sessionViewModel()
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(SessionUiEvent.SystemBackPressed)

        assertEquals(SessionUiSideEffect.NavigateToHome, effect.await())
    }

    private fun sessionViewModel(sessionId: String = "session-1") =
        SessionViewModel(Route.Session(sessionId))
}
