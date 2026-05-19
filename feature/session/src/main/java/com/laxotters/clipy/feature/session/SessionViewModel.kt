package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor() :
    BaseViewModel<SessionUiState, SessionUiEvent, SessionUiSideEffect>(SessionUiState()) {
    override fun handleEvent(event: SessionUiEvent) {
        when (event) {
            is SessionUiEvent.Entered -> updateState {
                if (sessionId == event.sessionId && currentUrl.isNotBlank()) {
                    this
                } else {
                    copy(
                        sessionId = event.sessionId,
                        currentUrl = event.initialUrl.ifBlank { DEFAULT_HOME_URL },
                        canGoBack = false,
                        canGoForward = false,
                    )
                }
            }

            is SessionUiEvent.WebPageStateChanged -> updateState {
                copy(
                    currentUrl = event.url,
                    canGoBack = event.canGoBack,
                    canGoForward = event.canGoForward,
                )
            }
        }
    }
}
