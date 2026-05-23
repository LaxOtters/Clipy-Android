package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState

data class SessionUiState(
    val sessionId: String = "",
    val currentUrl: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
) : UiState

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
}

sealed interface SessionUiSideEffect : UiSideEffect
