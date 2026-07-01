package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val HandleColor = Color(0xFFE4E4E7)
    const val CONTENT_FADE_IN_MILLIS = 500
    const val CONTENT_FADE_OUT_MILLIS = 500
}
