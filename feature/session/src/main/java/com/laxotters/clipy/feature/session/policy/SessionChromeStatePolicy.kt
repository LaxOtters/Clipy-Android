package com.laxotters.clipy.feature.session.policy

import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.SessionTopBarState

internal data class SessionChromeSnapshot(
    val bottomSheetState: BottomSheetState,
    val topBarState: SessionTopBarState,
)

/**
 * Top Bar와 Bottom Sheet를 함께 다루는 Session chrome 전이 정책입니다.
 *
 * Browsing chrome은 Hidden / Minimized에서 WebView 탐색에 맞춰 움직입니다.
 * Comparing chrome인 Peek / Expanded는 사용자가 열어둔 비교 문맥이므로 자동 전이하지 않습니다.
 */
internal object SessionChromeStatePolicy {
    fun onTopBarFoldClicked(current: SessionChromeSnapshot): SessionChromeSnapshot =
        when (current.bottomSheetState) {
            BottomSheetState.HIDDEN,
            BottomSheetState.MINIMIZED,
            -> when (current.topBarState) {
                SessionTopBarState.FOLDED -> current.toBrowsingMinimizedChrome()
                SessionTopBarState.UNFOLDED -> current.toBrowsingHiddenChrome()
            }

            BottomSheetState.PEEK -> current.copy(
                topBarState = current.topBarState.toggle(),
            )

            BottomSheetState.EXPANDED -> current
        }

    fun onPageLoaded(current: SessionChromeSnapshot): SessionChromeSnapshot =
        when (current.bottomSheetState) {
            BottomSheetState.HIDDEN,
            BottomSheetState.MINIMIZED,
            -> current.toBrowsingMinimizedChrome()

            BottomSheetState.PEEK,
            BottomSheetState.EXPANDED,
            -> current
        }

    fun onRootScrollIntent(
        current: SessionChromeSnapshot,
        direction: SessionRootScrollDirection,
        scrollableDistance: Int,
        viewportHeight: Int,
    ): SessionChromeSnapshot =
        when {
            !current.bottomSheetState.isBrowsingChrome() -> current
            direction == SessionRootScrollDirection.DOWN &&
                current.hasEnoughRootScrollAreaForHidden(
                    scrollableDistance = scrollableDistance,
                    viewportHeight = viewportHeight,
                ) -> current.toBrowsingHiddenChrome()

            direction == SessionRootScrollDirection.UP -> current.toBrowsingMinimizedChrome()
            else -> current
        }

    fun collapseSheetForBack(bottomSheetState: BottomSheetState): BottomSheetState? =
        when (bottomSheetState) {
            BottomSheetState.EXPANDED -> BottomSheetState.PEEK
            BottomSheetState.HIDDEN,
            BottomSheetState.MINIMIZED,
            BottomSheetState.PEEK,
            -> null
        }
}

private fun SessionTopBarState.toggle(): SessionTopBarState =
    when (this) {
        SessionTopBarState.FOLDED -> SessionTopBarState.UNFOLDED
        SessionTopBarState.UNFOLDED -> SessionTopBarState.FOLDED
    }

private fun BottomSheetState.isBrowsingChrome(): Boolean =
    this == BottomSheetState.HIDDEN || this == BottomSheetState.MINIMIZED

private fun SessionChromeSnapshot.hasEnoughRootScrollAreaForHidden(
    scrollableDistance: Int,
    viewportHeight: Int,
): Boolean {
    if (bottomSheetState == BottomSheetState.HIDDEN) {
        // 이미 Hidden이면 같은 방향 입력이 다시 들어와도 상태 변화가 없어 면적 제한을 적용하지 않습니다.
        return true
    }

    if (viewportHeight <= 0) {
        return false
    }

    return scrollableDistance.toFloat() >= viewportHeight * MIN_SCROLL_AREA_RATIO_FOR_HIDDEN
}

private fun SessionChromeSnapshot.toBrowsingHiddenChrome(): SessionChromeSnapshot =
    if (
        bottomSheetState == BottomSheetState.HIDDEN &&
        topBarState == SessionTopBarState.FOLDED
    ) {
        this
    } else {
        copy(
            bottomSheetState = BottomSheetState.HIDDEN,
            topBarState = SessionTopBarState.FOLDED,
        )
    }

private fun SessionChromeSnapshot.toBrowsingMinimizedChrome(): SessionChromeSnapshot =
    if (
        bottomSheetState == BottomSheetState.MINIMIZED &&
        topBarState == SessionTopBarState.UNFOLDED
    ) {
        this
    } else {
        copy(
            bottomSheetState = BottomSheetState.MINIMIZED,
            topBarState = SessionTopBarState.UNFOLDED,
        )
    }

// Hidden 자동 전이는 root scroll 가능 영역이 viewport height의 20% 이상일 때만 허용합니다.
private const val MIN_SCROLL_AREA_RATIO_FOR_HIDDEN = 0.2f
