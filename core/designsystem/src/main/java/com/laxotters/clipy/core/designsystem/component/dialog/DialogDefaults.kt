package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

internal object DialogDefaults {
    val maxWidth = 384.dp
    val screenHorizontalPadding = 20.dp
    val screenVerticalPadding = 60.dp
    val contentPadding = 20.dp
    val contentSpacing = 24.dp
    val headingTopPadding = 12.dp
    val textSpacing = 8.dp
    val errorIconSpacing = 4.dp
    val actionSpacing = 12.dp
    val iconSize = 60.dp
    val shape = RoundedCornerShape(16.dp)
    val shadow = Shadow(
        radius = 10.dp,
        spread = 0.dp,
        offset = DpOffset(
            x = 0.dp,
            y = 10.dp,
        ),
        color = Color.Black.copy(alpha = 0.06f),
    )
}

internal object JsDialogDefaults {
    val bodySpacing = 20.dp
    val sourceSpacing = 6.dp
    val sourceIconSize = 24.dp
    val inputHeight = 50.dp
    val inputPadding = PaddingValues(horizontal = 16.dp)
    val inputShape = RoundedCornerShape(12.dp)
    val actionHeight = 50.dp
    val actionShape = RoundedCornerShape(12.dp)
}
