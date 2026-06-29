package com.laxotters.clipy.feature.home

import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState

data class HomeUiState(
    val sessionId: String = "00000000-0000-0000-0000-000000000001",
) : UiState

sealed interface HomeUiEvent : UiEvent

sealed interface HomeUiSideEffect : UiSideEffect
