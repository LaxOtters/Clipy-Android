package com.laxotters.clipy.feature.home

import com.laxotters.clipy.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() :
    BaseViewModel<HomeUiState, HomeUiEvent, HomeUiSideEffect>(HomeUiState()) {
    override fun handleEvent(event: HomeUiEvent) = Unit
}
