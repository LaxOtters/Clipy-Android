package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.domain.model.BottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewModelTest {
    @Test
    fun initialState_createViewModel_hasInitialChromeState() {
        val viewModel = SessionViewModel()
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
    }

    @Test
    fun changedState_newSessionEntered_resetsToInitialChromeState() {
        val viewModel = SessionViewModel()

        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.EXPANDED))
        viewModel.dispatch(
            SessionUiEvent.ScreenEntered(
                sessionId = "session-1",
                initialUrl = "https://example.com",
            ),
        )
        val state = viewModel.state.value

        assertEquals(BottomSheetState.PEEK, state.bottomSheetState)
        assertEquals(SessionTopBarState.UNFOLDED, state.topBarState)
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
