package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.ui.base.BaseViewModel
import com.laxotters.clipy.domain.model.BottomSheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor() :
    BaseViewModel<SessionUiState, SessionUiEvent, SessionUiSideEffect>(SessionUiState()) {
    override fun handleEvent(event: SessionUiEvent) {
        when (event) {
            is SessionUiEvent.ScreenEntered -> updateState {
                if (sessionId == event.sessionId && currentUrl.isNotBlank()) {
                    this
                } else {
                    copy(
                        sessionId = event.sessionId,
                        currentUrl = event.initialUrl.ifBlank { DEFAULT_HOME_URL },
                        canGoBack = false,
                        canGoForward = false,
                        bottomSheetState = BottomSheetState.PEEK,
                    )
                }
            }

            is SessionUiEvent.PageLoaded -> updateState {
                copy(
                    currentUrl = event.url,
                    canGoBack = event.canGoBack,
                    canGoForward = event.canGoForward,
                )
            }

            is SessionUiEvent.BottomSheetValueChanged -> updateState {
                copy(bottomSheetState = event.value.toBottomSheetState())
            }
        }
    }
}

private fun ClipyBottomSheetValue.toBottomSheetState(): BottomSheetState =
    when (this) {
        ClipyBottomSheetValue.HIDDEN -> BottomSheetState.HIDDEN
        ClipyBottomSheetValue.MINIMIZED -> BottomSheetState.MINIMIZED
        ClipyBottomSheetValue.PEEK -> BottomSheetState.PEEK
        ClipyBottomSheetValue.EXPANDED -> BottomSheetState.EXPANDED
    }
