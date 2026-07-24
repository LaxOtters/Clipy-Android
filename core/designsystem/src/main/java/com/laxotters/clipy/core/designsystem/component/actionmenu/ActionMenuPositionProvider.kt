package com.laxotters.clipy.core.designsystem.component.actionmenu

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuPlacement

/**
 * 요청한 [placement]와 화면 경계를 기준으로 메뉴 위치를 결정합니다.
 *
 * 요청한 가로·세로 방향을 우선하고, 들어가지 않는 축은 반대 방향을 사용합니다.
 * 한 축이라도 두 방향 모두 배치할 수 없으면 요청 위치에서 화면 경계 안쪽으로 보정합니다.
 */
internal class ActionMenuPositionProvider(
    density: Density,
    private val placement: ActionMenuPlacement,
) : PopupPositionProvider {
    private val verticalMargin = with(density) { ActionMenuVerticalMargin.roundToPx() }

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val endAlignedX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.right - popupContentSize.width
            LayoutDirection.Rtl -> anchorBounds.left
        }
        val startAlignedX = when (layoutDirection) {
            LayoutDirection.Ltr -> anchorBounds.left
            LayoutDirection.Rtl -> anchorBounds.right - popupContentSize.width
        }
        // 가로 우선순위: 요청한 Start/End 정렬 → 반대 정렬
        val (preferredX, alternativeX) = when (placement) {
            ActionMenuPlacement.BottomEnd,
            ActionMenuPlacement.TopEnd,
            -> endAlignedX to startAlignedX

            ActionMenuPlacement.BottomStart,
            ActionMenuPlacement.TopStart,
            -> startAlignedX to endAlignedX
        }

        val belowAnchorY = anchorBounds.bottom
        val aboveAnchorY = anchorBounds.top - popupContentSize.height
        // 세로 우선순위: 요청한 Top/Bottom 방향 → 반대 방향
        val (preferredY, alternativeY) = when (placement) {
            ActionMenuPlacement.BottomEnd,
            ActionMenuPlacement.BottomStart,
            -> belowAnchorY to aboveAnchorY

            ActionMenuPlacement.TopEnd,
            ActionMenuPlacement.TopStart,
            -> aboveAnchorY to belowAnchorY
        }

        val resolvedX = resolveHorizontalPosition(
            preferredX = preferredX,
            alternativeX = alternativeX,
            windowWidth = windowSize.width,
            popupWidth = popupContentSize.width,
        )
        val resolvedY = resolveVerticalPosition(
            preferredY = preferredY,
            alternativeY = alternativeY,
            windowHeight = windowSize.height,
            popupHeight = popupContentSize.height,
        )

        // 한 축이라도 두 후보가 모두 불가능하면 최초 요청 좌표를 화면 경계 안쪽으로 보정합니다.
        return if (resolvedX != null && resolvedY != null) {
            IntOffset(x = resolvedX, y = resolvedY)
        } else {
            correctPreferredPosition(
                preferredPosition = IntOffset(
                    x = preferredX,
                    y = preferredY,
                ),
                windowSize = windowSize,
                popupContentSize = popupContentSize,
            )
        }
    }

    private fun resolveHorizontalPosition(
        preferredX: Int,
        alternativeX: Int,
        windowWidth: Int,
        popupWidth: Int,
    ): Int? = when {
        fitsHorizontally(preferredX, windowWidth, popupWidth) -> preferredX
        fitsHorizontally(alternativeX, windowWidth, popupWidth) -> alternativeX
        else -> null
    }

    private fun resolveVerticalPosition(
        preferredY: Int,
        alternativeY: Int,
        windowHeight: Int,
        popupHeight: Int,
    ): Int? = when {
        fitsVertically(preferredY, windowHeight, popupHeight) -> preferredY
        fitsVertically(alternativeY, windowHeight, popupHeight) -> alternativeY
        else -> null
    }

    private fun correctPreferredPosition(
        preferredPosition: IntOffset,
        windowSize: IntSize,
        popupContentSize: IntSize,
    ): IntOffset {
        val correctedX = when {
            popupContentSize.width >= windowSize.width -> {
                (windowSize.width - popupContentSize.width) / 2
            }

            else -> preferredPosition.x.coerceIn(
                minimumValue = 0,
                maximumValue = windowSize.width - popupContentSize.width,
            )
        }
        val availableHeight = windowSize.height - verticalMargin * 2
        val correctedY = when {
            popupContentSize.height >= availableHeight -> {
                (windowSize.height - popupContentSize.height) / 2
            }

            else -> preferredPosition.y.coerceIn(
                minimumValue = verticalMargin,
                maximumValue = windowSize.height - verticalMargin - popupContentSize.height,
            )
        }

        return IntOffset(x = correctedX, y = correctedY)
    }

    private fun fitsHorizontally(
        x: Int,
        windowWidth: Int,
        popupWidth: Int,
    ): Boolean = x >= 0 && x + popupWidth <= windowWidth

    private fun fitsVertically(
        y: Int,
        windowHeight: Int,
        popupHeight: Int,
    ): Boolean =
        y >= verticalMargin &&
            y + popupHeight <= windowHeight - verticalMargin
}

private val ActionMenuVerticalMargin = 48.dp
