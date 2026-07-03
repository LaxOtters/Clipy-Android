package com.laxotters.clipy.feature.session

import com.laxotters.clipy.core.designsystem.component.ClipyBottomSheetValue
import com.laxotters.clipy.core.ui.base.UiEvent
import com.laxotters.clipy.core.ui.base.UiSideEffect
import com.laxotters.clipy.core.ui.base.UiState
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.util.formatUrlLabel

enum class SessionTopBarState {
    FOLDED,
    UNFOLDED,
}

data class SessionUiState(
    val sessionId: String = "",
    val bottomSheetState: BottomSheetState = BottomSheetState.PEEK,
    val topBarState: SessionTopBarState = SessionTopBarState.UNFOLDED,
    val initialUrl: String? = null,
    val currentUrl: String = "",
    // 화면에 표시할 URL source가 바뀌면 함께 갱신해야 합니다.
    val currentUrlLabel: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isWebViewRootScrolling: Boolean = false,
) : UiState {
    companion object {
        fun newSession(
            sessionId: String,
            initialUrl: String,
        ) = SessionUiState(
            sessionId = sessionId,
            initialUrl = initialUrl,
            currentUrlLabel = formatUrlLabel(initialUrl),
        )
    }
}

sealed interface SessionUiEvent : UiEvent {
    data class ScreenEntered(
        val sessionId: String,
    ) : SessionUiEvent

    data object TopBarFoldClicked : SessionUiEvent

    data class BottomSheetValueChanged(
        val value: ClipyBottomSheetValue,
    ) : SessionUiEvent

    data class PageLoaded(
        val sessionId: String,
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : SessionUiEvent

    data class WebViewRootScrolled(
        val deltaY: Int,
        val scrollableDistance: Int,
        val viewportHeight: Int,
        val touchSlopPx: Int,
    ) : SessionUiEvent

    data object SystemBackPressed : SessionUiEvent
}

sealed interface SessionUiSideEffect : UiSideEffect {
    data object GoBackInWebView : SessionUiSideEffect
    data object NavigateToHome : SessionUiSideEffect
}
