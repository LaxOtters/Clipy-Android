package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.domain.model.BottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewModelTest {
    @Test
    fun initialState_createViewModel_hasPeekBottomSheetState() {
        val viewModel = SessionViewModel()

        assertEquals(BottomSheetState.PEEK, viewModel.state.value.bottomSheetState)
    }

    @Test
    fun changedBottomSheetState_newSessionEntered_resetsBottomSheetStateToPeek() {
        val viewModel = SessionViewModel()

        viewModel.dispatch(SessionUiEvent.BottomSheetValueChanged(ClipyBottomSheetValue.EXPANDED))
        viewModel.dispatch(
            SessionUiEvent.ScreenEntered(
                sessionId = "session-1",
                initialUrl = "https://example.com",
            ),
        )

        assertEquals(BottomSheetState.PEEK, viewModel.state.value.bottomSheetState)
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

            assertEquals(expectedState, viewModel.state.value.bottomSheetState)
        }
    }
}
