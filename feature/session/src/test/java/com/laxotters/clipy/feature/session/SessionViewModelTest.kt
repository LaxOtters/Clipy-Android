package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.domain.model.BottomSheetState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun differentRoutes_createIndependentChromeState() {
        val firstViewModel = sessionViewModel(sessionId = "session-1")
        firstViewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        val secondViewModel = sessionViewModel(sessionId = "session-2")

        assertEquals(SessionTopBarState.FOLDED, firstViewModel.state.value.topBarState)
        assertEquals("session-2", secondViewModel.state.value.sessionId)
        assertEquals(DEFAULT_HOME_URL, secondViewModel.state.value.initialUrl)
        assertEquals(SessionTopBarState.UNFOLDED, secondViewModel.state.value.topBarState)
    }

    @Test
    fun pageLoaded_matchingSessionId_restoresBrowsingChromeAndKeepsInitialUrl() {
        val viewModel = sessionViewModel(sessionId = "session-1")
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.HIDDEN))

        viewModel.dispatch(SessionUiEvent.PageLoaded(sessionId = "session-1"))

        val state = viewModel.state.value
        assertEquals(DEFAULT_HOME_URL, state.initialUrl)
        assertEquals(BottomSheetState.MINIMIZED, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun pageLoaded_staleSessionId_keepsChromeState() {
        val viewModel = sessionViewModel(sessionId = "session-1")
        viewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.HIDDEN))
        val previousState = viewModel.state.value

        viewModel.dispatch(SessionUiEvent.PageLoaded(sessionId = "session-0"))

        assertEquals(previousState, viewModel.state.value)
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

        viewModel.dispatch(SessionUiEvent.SystemBackPressed(canGoBack = true))
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
            ),
        )
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(SessionUiEvent.SystemBackPressed(canGoBack = true))

        assertEquals(SessionUiSideEffect.GoBackInWebView, effect.await())
    }

    @Test
    fun systemBackPressed_withoutHistory_postsNavigateToHomeSideEffect() = runTest {
        val viewModel = sessionViewModel()
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(SessionUiEvent.SystemBackPressed(canGoBack = false))

        assertEquals(SessionUiSideEffect.NavigateToHome, effect.await())
    }

    @Test
    fun systemBackPressed_usesCurrentHistoryInputInsteadOfPreviousPress() = runTest {
        val viewModel = sessionViewModel()
        val webBack = async { viewModel.effect.first() }
        viewModel.dispatch(SessionUiEvent.SystemBackPressed(canGoBack = true))
        assertEquals(SessionUiSideEffect.GoBackInWebView, webBack.await())

        val home = async { viewModel.effect.first() }
        viewModel.dispatch(SessionUiEvent.SystemBackPressed(canGoBack = false))
        assertEquals(SessionUiSideEffect.NavigateToHome, home.await())
    }

    private fun sessionViewModel(sessionId: String = "session-1") =
        SessionViewModel(Route.Session(sessionId))
}
