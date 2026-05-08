package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor() :
    BaseViewModel<SessionUiState, SessionUiEvent, SessionUiSideEffect>(SessionUiState()) {
    override fun handleEvent(event: SessionUiEvent) = Unit
}
