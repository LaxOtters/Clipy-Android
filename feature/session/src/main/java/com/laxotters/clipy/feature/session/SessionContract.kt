package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState
import com.laxotters.clipy.domain.model.BottomSheetState

enum class SessionTopBarState {
    FOLDED,
    UNFOLDED,
}

data class SessionUiState(
    val sessionId: String = "",
    val bottomSheetState: BottomSheetState = BottomSheetState.PEEK,
    val topBarState: SessionTopBarState = SessionTopBarState.UNFOLDED,
    val initialUrl: String? = null,
    val currentUrl: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
) : UiState {
    companion object {
        fun newSession(
            sessionId: String,
            initialUrl: String,
        ) = SessionUiState(
            sessionId = sessionId,
            initialUrl = initialUrl,
        )
    }
}

sealed interface SessionUiEvent : UiEvent {
    data class ScreenEntered(
        val sessionId: String,
    ) : SessionUiEvent

    data class PageLoaded(
        val sessionId: String,
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : SessionUiEvent

    data class BottomSheetValueChanged(
        val value: ClipyBottomSheetValue,
    ) : SessionUiEvent
}

sealed interface SessionUiSideEffect : UiSideEffect
