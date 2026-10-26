package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
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
    val isWebViewRootScrolling: Boolean = false,
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
    data object TopBarFoldClicked : SessionUiEvent

    data class BottomSheetValueChanged(
        val value: BottomSheetValue,
    ) : SessionUiEvent

    data class PageLoaded(
        val sessionId: String,
    ) : SessionUiEvent

    data class WebViewRootScrolled(
        val deltaY: Int,
        val scrollableDistance: Int,
        val viewportHeight: Int,
        val touchSlopPx: Int,
    ) : SessionUiEvent

    data class SystemBackPressed(
        val canGoBack: Boolean,
    ) : SessionUiEvent
}

sealed interface SessionUiSideEffect : UiSideEffect {
    data object GoBackInWebView : SessionUiSideEffect
    data object NavigateToHome : SessionUiSideEffect
}
