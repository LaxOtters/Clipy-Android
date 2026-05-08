package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState

data class SessionUiState(val title: String = "Session") : UiState

sealed interface SessionUiEvent : UiEvent

sealed interface SessionUiSideEffect : UiSideEffect
