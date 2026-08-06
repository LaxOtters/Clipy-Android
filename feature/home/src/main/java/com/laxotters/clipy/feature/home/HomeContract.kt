package com.laxotters.clipy.feature.home

import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState

data class HomeUiState(
    val isStartingSession: Boolean = false,
) : UiState

sealed interface HomeUiEvent : UiEvent {
    data object StartNewSessionClicked : HomeUiEvent
}

sealed interface HomeUiSideEffect : UiSideEffect {
    data class NavigateToSession(
        val sessionId: String,
    ) : HomeUiSideEffect
}
