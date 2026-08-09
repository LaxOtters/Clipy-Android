package com.laxotters.clipy.feature.home

import androidx.lifecycle.viewModelScope
import com.laxotters.clipy.core.ui.base.BaseViewModel
import com.laxotters.clipy.domain.usecase.CreateSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createSessionUseCase: CreateSessionUseCase,
) : BaseViewModel<HomeUiState, HomeUiEvent, HomeUiSideEffect>(HomeUiState()) {
    override fun handleEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.StartNewSessionClicked -> startNewSession()
        }
    }

    private fun startNewSession() {
        if (currentState.isStartingSession) {
            return
        }

        updateState { copy(isStartingSession = true) }
        viewModelScope.launch {
            try {
                val sessionId = createSessionUseCase().toString()
                updateState { copy(isStartingSession = false) }
                postSideEffect(HomeUiSideEffect.NavigateToSession(sessionId))
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                updateState { copy(isStartingSession = false) }
            }
        }
    }
}
