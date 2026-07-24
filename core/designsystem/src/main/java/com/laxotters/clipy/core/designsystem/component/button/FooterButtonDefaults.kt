package com.laxotters.clipy.core.designsystem.component.button

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.component.button.model.FooterButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

internal object FooterButtonDefaults {
    val shape = RoundedCornerShape(16.dp)

    val shadow = Shadow(
        radius = 5.dp,
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 4.dp),
        color = Color.Black.copy(alpha = 0.10f),
    )

    @Composable
    fun background(
        enabled: Boolean,
        type: FooterButtonType,
    ): Brush = if (enabled) {
        when (type) {
            FooterButtonType.Gradient -> ClipyTheme.gradients.linear
            FooterButtonType.Solid -> SolidColor(ClipyTheme.colors.primary.indigo400)
        }
    } else {
        SolidColor(ClipyTheme.colors.neutral.gray50)
    }

    @Composable
    fun contentColor(enabled: Boolean): Color =
        if (enabled) {
            ClipyTheme.colors.primary.indigo50
        } else {
            ClipyTheme.colors.neutral.gray800
        }
}
