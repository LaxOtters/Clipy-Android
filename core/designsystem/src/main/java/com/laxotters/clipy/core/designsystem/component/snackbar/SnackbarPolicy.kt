package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.compose.material3.SnackbarData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal enum class SnackbarActionLayout {
    Inline,
    Stacked,
}

/** 현재 Snackbar의 영역과 바깥 터치 dismiss 가능 상태를 보관합니다. */
internal data class SnackbarOutsideTapState(
    val target: SnackbarData? = null,
    val bounds: Rect? = null,
    val canDismiss: Boolean = false,
) {
    /** 현재 표시 중인 Snackbar의 바깥 터치 판정 영역을 갱신합니다. */
    fun updateBounds(
        activeSnackbarData: SnackbarData?,
        snackbarData: SnackbarData,
        bounds: Rect,
    ): SnackbarOutsideTapState {
        if (activeSnackbarData !== snackbarData) {
            return this
        }

        if (target !== snackbarData) {
            return SnackbarOutsideTapState(
                target = snackbarData,
                bounds = bounds,
            )
        }

        return if (this.bounds == bounds) {
            this
        } else {
            copy(bounds = bounds)
        }
    }

    /** 현재 표시 중인 Snackbar의 바깥 터치 dismiss를 활성화합니다. */
    fun enableDismiss(
        activeSnackbarData: SnackbarData?,
        snackbarData: SnackbarData,
    ): SnackbarOutsideTapState {
        if (activeSnackbarData !== snackbarData) {
            return this
        }

        if (target !== snackbarData) {
            return SnackbarOutsideTapState(
                target = snackbarData,
                canDismiss = true,
            )
        }

        return if (canDismiss) {
            this
        } else {
            copy(canDismiss = true)
        }
    }

    /** 종료된 Snackbar가 현재 판정 대상일 때 상태를 초기화합니다. */
    fun clearIfMatches(snackbarData: SnackbarData): SnackbarOutsideTapState =
        if (target === snackbarData) {
            SnackbarOutsideTapState()
        } else {
            this
        }

    /** 현재 Snackbar가 바깥 터치로 닫혀야 하는지 판단합니다. */
    fun shouldDismiss(
        currentSnackbarData: SnackbarData?,
        tapPosition: Offset,
    ): Boolean =
        canDismiss &&
            target === currentSnackbarData &&
            SnackbarPolicy.isOutsideSnackbar(
                snackbarBounds = bounds,
                tapPosition = tapPosition,
            )
}

internal object SnackbarPolicy {
    const val DISPLAY_DURATION_MILLIS = 2_000L
    const val BACKDROP_CAPTURE_TIMEOUT_MILLIS = 150L

    /** 메시지와 action의 합산 폭으로 action 배치를 결정합니다. */
    fun resolveActionLayout(
        messageWidth: Int,
        actionWidth: Int,
        messageActionSpacing: Int,
        availableWidth: Int,
    ): SnackbarActionLayout = if (
        messageWidth + actionWidth + messageActionSpacing > availableWidth
    ) {
        SnackbarActionLayout.Stacked
    } else {
        SnackbarActionLayout.Inline
    }

    /** 터치 위치가 Snackbar 영역 밖인지 판단합니다. */
    fun isOutsideSnackbar(
        snackbarBounds: Rect?,
        tapPosition: Offset,
    ): Boolean = snackbarBounds != null && !snackbarBounds.contains(tapPosition)
}
