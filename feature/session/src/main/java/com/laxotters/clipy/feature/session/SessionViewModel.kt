package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.ui.base.BaseViewModel
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.util.formatUrlLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor() :
    BaseViewModel<SessionUiState, SessionUiEvent, SessionUiSideEffect>(SessionUiState()) {
    override fun handleEvent(event: SessionUiEvent) {
        when (event) {
            is SessionUiEvent.ScreenEntered -> updateState {
                if (sessionId == event.sessionId && initialUrl != null) {
                    this
                } else {
                    // TODO: 저장된 세션 lastWebUrl 조회로 DEFAULT_HOME_URL 대체
                    SessionUiState.newSession(
                        sessionId = event.sessionId,
                        initialUrl = DEFAULT_HOME_URL,
                    )
                }
            }

            is SessionUiEvent.PageLoaded -> updateState {
                // 이전 세션 WebView의 지연 콜백이면 현재 세션 상태를 덮지 않습니다.
                if (event.sessionId != sessionId) {
                    this
                } else {
                    copy(
                        currentUrl = event.url,
                        currentUrlLabel = formatUrlLabel(event.url),
                        canGoBack = event.canGoBack,
                        canGoForward = event.canGoForward,
                    )
                }
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
