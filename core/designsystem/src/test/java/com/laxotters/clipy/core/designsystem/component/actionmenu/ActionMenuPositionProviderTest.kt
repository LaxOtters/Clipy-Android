package com.laxotters.clipy.core.designsystem.component.actionmenu

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionMenuPositionProviderTest {
    @Test
    fun placementFits_returnsRequestedPosition() {
        val anchorBounds = IntRect(
            left = 150,
            top = 250,
            right = 250,
            bottom = 290,
        )

        assertEquals(
            IntOffset(x = 130, y = 290),
            calculatePosition(anchorBounds, ActionMenuPlacement.BottomEnd),
        )
        assertEquals(
            IntOffset(x = 150, y = 290),
            calculatePosition(anchorBounds, ActionMenuPlacement.BottomStart),
        )
        assertEquals(
            IntOffset(x = 130, y = 90),
            calculatePosition(anchorBounds, ActionMenuPlacement.TopEnd),
        )
        assertEquals(
            IntOffset(x = 150, y = 90),
            calculatePosition(anchorBounds, ActionMenuPlacement.TopStart),
        )
    }

    @Test
    fun horizontalOverflow_returnsAlternativeAlignment() {
        val endOverflowPosition = calculatePosition(
            anchorBounds = IntRect(
                left = 10,
                top = 100,
                right = 80,
                bottom = 140,
            ),
            placement = ActionMenuPlacement.BottomEnd,
        )
        val startOverflowPosition = calculatePosition(
            anchorBounds = IntRect(
                left = 220,
                top = 100,
                right = 290,
                bottom = 140,
            ),
            placement = ActionMenuPlacement.BottomStart,
        )

        assertEquals(IntOffset(x = 10, y = 140), endOverflowPosition)
        assertEquals(IntOffset(x = 170, y = 140), startOverflowPosition)
    }

    @Test
    fun verticalOverflow_returnsAlternativeDirection() {
        val bottomOverflowPosition = calculatePosition(
            anchorBounds = IntRect(
                left = 150,
                top = 500,
                right = 250,
                bottom = 540,
            ),
            placement = ActionMenuPlacement.BottomEnd,
        )
        val topOverflowPosition = calculatePosition(
            anchorBounds = IntRect(
                left = 150,
                top = 60,
                right = 250,
                bottom = 100,
            ),
            placement = ActionMenuPlacement.TopEnd,
        )

        assertEquals(IntOffset(x = 130, y = 340), bottomOverflowPosition)
        assertEquals(IntOffset(x = 130, y = 100), topOverflowPosition)
    }

    @Test
    fun bothAxesOverflow_returnsAlternativePosition() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 10,
                top = 500,
                right = 80,
                bottom = 540,
            ),
            placement = ActionMenuPlacement.BottomEnd,
        )

        assertEquals(IntOffset(x = 10, y = 340), position)
    }

    @Test
    fun horizontalCandidatesOverflow_clampsRequestedPosition() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = -100,
                top = 500,
                right = -50,
                bottom = 540,
            ),
            placement = ActionMenuPlacement.BottomEnd,
            popupContentSize = IntSize(width = 260, height = 160),
        )

        assertEquals(IntOffset(x = 0, y = 392), position)
    }

    @Test
    fun verticalCandidatesOverflow_clampsRequestedPosition() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 250,
                top = 0,
                right = 280,
                bottom = 40,
            ),
            placement = ActionMenuPlacement.TopStart,
        )

        assertEquals(IntOffset(x = 180, y = 48), position)
    }

    @Test
    fun rtl_usesLogicalStartAndEnd() {
        val anchorBounds = IntRect(
            left = 150,
            top = 250,
            right = 250,
            bottom = 290,
        )

        val endPosition = calculatePosition(
            anchorBounds = anchorBounds,
            placement = ActionMenuPlacement.BottomEnd,
            layoutDirection = LayoutDirection.Rtl,
        )
        val startPosition = calculatePosition(
            anchorBounds = anchorBounds,
            placement = ActionMenuPlacement.BottomStart,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertEquals(IntOffset(x = 150, y = 290), endPosition)
        assertEquals(IntOffset(x = 130, y = 290), startPosition)
    }

    @Test
    fun rtl_horizontalOverflow_returnsAlternativeAlignment() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 220,
                top = 250,
                right = 290,
                bottom = 290,
            ),
            placement = ActionMenuPlacement.BottomEnd,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertEquals(IntOffset(x = 170, y = 290), position)
    }

    @Test
    fun exactBoundaryFits_returnsRequestedPosition() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 200,
                top = 352,
                right = 300,
                bottom = 392,
            ),
            placement = ActionMenuPlacement.BottomEnd,
        )

        assertEquals(IntOffset(x = 180, y = 392), position)
    }

    @Test
    fun oversizedPopup_returnsCenter() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 100,
                top = 300,
                right = 200,
                bottom = 340,
            ),
            placement = ActionMenuPlacement.BottomEnd,
            popupContentSize = IntSize(width = 340, height = 520),
        )

        assertEquals(IntOffset(x = -20, y = 40), position)
    }

    @Test
    fun higherDensity_bottomOverflows_returnsAbove() {
        val position = calculatePosition(
            anchorBounds = IntRect(
                left = 150,
                top = 310,
                right = 250,
                bottom = 350,
            ),
            placement = ActionMenuPlacement.BottomEnd,
            density = Density(2f),
        )

        assertEquals(IntOffset(x = 130, y = 150), position)
    }

    private fun calculatePosition(
        anchorBounds: IntRect,
        placement: ActionMenuPlacement,
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        popupContentSize: IntSize = IntSize(width = 120, height = 160),
    ): IntOffset =
        ActionMenuPositionProvider(
            density = density,
            placement = placement,
        ).calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = IntSize(width = 300, height = 600),
            layoutDirection = layoutDirection,
            popupContentSize = popupContentSize,
        )
}
