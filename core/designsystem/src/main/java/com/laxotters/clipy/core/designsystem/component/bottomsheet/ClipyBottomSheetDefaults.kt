package com.laxotters.clipy.core.designsystem.component.bottomsheet

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

object ClipyBottomSheetDefaults {
    val HandleHeight: Dp = 32.dp
    val HeaderHeight: Dp = 44.dp
    val BottomPadding: Dp = 32.dp

    val HiddenHeight: Dp = 0.dp
    val MinimizedHeight: Dp = HandleHeight + HeaderHeight + BottomPadding
    val PeekContentHeight: Dp = 166.dp
    val PeekHeight: Dp = MinimizedHeight + PeekContentHeight

    val RetentionBand: Dp = 30.dp
    val VelocityThreshold: Dp = 1_400.dp
    val HandleWidth: Dp = 42.dp
    val HandleIndicatorHeight: Dp = 4.dp
    val SheetShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
    )
    val SheetElevation: Dp = 16.dp
    val HandleColor: Color
        @Composable get() = ClipyTheme.colors.neutral.gray200
    const val CONTENT_FADE_IN_MILLIS = 500
    const val CONTENT_FADE_OUT_MILLIS = 500
}
