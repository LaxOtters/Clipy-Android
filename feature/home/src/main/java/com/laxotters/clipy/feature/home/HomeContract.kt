package com.laxotters.clipy.feature.home

import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState

data class HomeUiState(
    val title: String = "Home",
) : UiState

sealed interface HomeUiEvent : UiEvent

sealed interface HomeUiSideEffect : UiSideEffect
