package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState
import com.laxotters.clipy.domain.model.BottomSheetState

data class SessionUiState(
    val sessionId: String = "",
    val currentUrl: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val bottomSheetState: BottomSheetState = BottomSheetState.PEEK,
) : UiState {
    companion object {
        fun newSession(
            sessionId: String,
        ) = SessionUiState(
            sessionId = sessionId,
        )
    }
}

sealed interface SessionUiEvent : UiEvent {
    data class ScreenEntered(
        val sessionId: String,
        val initialUrl: String,
    ) : SessionUiEvent

    data class PageLoaded(
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : SessionUiEvent

    data class BottomSheetValueChanged(
        val value: ClipyBottomSheetValue,
    ) : SessionUiEvent
}

sealed interface SessionUiSideEffect : UiSideEffect
