package com.laxotters.clipy.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : UiState, Event : UiEvent, SideEffect : UiSideEffect>(
    initialState: State,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<SideEffect>(Channel.BUFFERED)
    val effect: Flow<SideEffect> = _effect.receiveAsFlow()

    protected val currentState: State
        get() = _state.value

    fun dispatch(event: Event) {
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: Event)

    protected fun updateState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun postSideEffect(sideEffect: SideEffect) {
        viewModelScope.launch {
            _effect.send(sideEffect)
        }
    }
}
