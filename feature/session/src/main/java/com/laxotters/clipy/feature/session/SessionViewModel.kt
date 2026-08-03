package com.laxotters.clipy.feature.session

import androidx.lifecycle.viewModelScope
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.core.ui.base.BaseViewModel
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.policy.SessionChromeSnapshot
import com.laxotters.clipy.feature.session.policy.SessionChromeStatePolicy
import com.laxotters.clipy.feature.session.policy.SessionRootScrollIntentDetector
import com.laxotters.clipy.feature.session.util.formatUrlLabel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SessionViewModel.Factory::class)
class SessionViewModel @AssistedInject constructor(
    @Assisted route: Route.Session,
) :
    BaseViewModel<SessionUiState, SessionUiEvent, SessionUiSideEffect>(
        SessionUiState.newSession(
            sessionId = route.sessionId,
            initialUrl = DEFAULT_HOME_URL,
        ),
    ) {
    @AssistedFactory
    interface Factory {
        fun create(route: Route.Session): SessionViewModel
    }

    private val rootScrollIntentDetector = SessionRootScrollIntentDetector()
    private var rootScrollIdleJob: Job? = null

    override fun handleEvent(event: SessionUiEvent) {
        when (event) {
            SessionUiEvent.TopBarFoldClicked -> applyTopBarFoldPolicy()
            is SessionUiEvent.BottomSheetValueChanged -> syncBottomSheetValue(event.value)
            is SessionUiEvent.PageLoaded -> updatePageState(
                sessionId = event.sessionId,
                url = event.url,
                canGoBack = event.canGoBack,
                canGoForward = event.canGoForward,
            )

            is SessionUiEvent.WebViewRootScrolled -> applyRootScrollChromePolicy(
                deltaY = event.deltaY,
                scrollableDistance = event.scrollableDistance,
                viewportHeight = event.viewportHeight,
                touchSlopPx = event.touchSlopPx,
            )

            SessionUiEvent.SystemBackPressed -> resolveSystemBack()
        }
    }

    private fun updatePageState(
        sessionId: String,
        url: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) {
        if (sessionId != currentState.sessionId) {
            return
        }

        resetWebViewRootScrollTracking()
        updateState {
            copy(
                currentUrl = url,
                currentUrlLabel = formatUrlLabel(url),
                canGoBack = canGoBack,
                canGoForward = canGoForward,
            ).withChromeSnapshot(
                SessionChromeStatePolicy.onPageLoaded(toChromeSnapshot()),
            )
        }
    }

    private fun syncBottomSheetValue(value: BottomSheetValue) {
        resetWebViewRootScrollTracking()
        updateState {
            copy(bottomSheetState = value.toBottomSheetState())
        }
    }

    private fun applyTopBarFoldPolicy() {
        if (currentState.isWebViewRootScrolling) {
            // root scroll 진행 중에는 fold 버튼 입력을 무시합니다.
            return
        }

        resetWebViewRootScrollTracking()
        updateState {
            withChromeSnapshot(
                SessionChromeStatePolicy.onTopBarFoldClicked(toChromeSnapshot()),
            )
        }
    }

    /**
     * WebView root scroll 입력을 Session chrome 상태 전이에 반영합니다.
     */
    private fun applyRootScrollChromePolicy(
        deltaY: Int,
        scrollableDistance: Int,
        viewportHeight: Int,
        touchSlopPx: Int,
    ) {
        updateWebViewRootScrollState()
        val direction = rootScrollIntentDetector.detectDirection(
            deltaY = deltaY,
            thresholdPx = touchSlopPx,
        ) ?: return

        updateState {
            withChromeSnapshot(
                SessionChromeStatePolicy.onRootScrollIntent(
                    current = toChromeSnapshot(),
                    direction = direction,
                    scrollableDistance = scrollableDistance,
                    viewportHeight = viewportHeight,
                ),
            )
        }
    }

    private fun resolveSystemBack() {
        resetWebViewRootScrollTracking()
        val state = currentState
        val sheetTarget = SessionChromeStatePolicy.collapseSheetForBack(
            state.bottomSheetState,
        )

        when {
            sheetTarget != null -> updateState {
                copy(bottomSheetState = sheetTarget)
            }

            state.canGoBack -> postSideEffect(SessionUiSideEffect.GoBackInWebView)
            else -> postSideEffect(SessionUiSideEffect.NavigateToHome)
        }
    }

    /**
     * root scroll callback 수신 시 스크롤 진행 상태를 갱신하고 idle 복귀를 예약합니다.
     */
    private fun updateWebViewRootScrollState() {
        rootScrollIdleJob?.cancel()
        if (!currentState.isWebViewRootScrolling) {
            updateState { copy(isWebViewRootScrolling = true) }
        }

        rootScrollIdleJob = viewModelScope.launch {
            delay(ROOT_SCROLL_IDLE_DELAY_MILLIS)
            rootScrollIntentDetector.reset()
            updateState { copy(isWebViewRootScrolling = false) }
        }
    }

    /**
     * ViewModel이 추적 중인 root scroll 진행 상태와 방향 누적값을 초기화합니다.
     */
    private fun resetWebViewRootScrollTracking() {
        rootScrollIdleJob?.cancel()
        rootScrollIdleJob = null
        rootScrollIntentDetector.reset()
        if (currentState.isWebViewRootScrolling) {
            updateState { copy(isWebViewRootScrolling = false) }
        }
    }
}

private fun SessionUiState.toChromeSnapshot(): SessionChromeSnapshot =
    SessionChromeSnapshot(
        bottomSheetState = bottomSheetState,
        topBarState = topBarState,
    )

private fun SessionUiState.withChromeSnapshot(snapshot: SessionChromeSnapshot): SessionUiState =
    copy(
        bottomSheetState = snapshot.bottomSheetState,
        topBarState = snapshot.topBarState,
    )

private fun BottomSheetValue.toBottomSheetState(): BottomSheetState =
    when (this) {
        BottomSheetValue.HIDDEN -> BottomSheetState.HIDDEN
        BottomSheetValue.MINIMIZED -> BottomSheetState.MINIMIZED
        BottomSheetValue.PEEK -> BottomSheetState.PEEK
        BottomSheetValue.EXPANDED -> BottomSheetState.EXPANDED
    }

// root scroll 진행 상태를 idle로 되돌리기 전 기다리는 시간입니다.
private const val ROOT_SCROLL_IDLE_DELAY_MILLIS = 150L
