package com.laxotters.clipy.core.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonSize
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

internal object ButtonDefaults {
    val contentPadding = PaddingValues(horizontal = 20.dp)

    fun height(size: ButtonSize): Dp = when (size) {
        ButtonSize.Small -> 36.dp
        ButtonSize.Medium -> 50.dp
    }

    fun shape(size: ButtonSize): Shape = when (size) {
        ButtonSize.Small -> RoundedCornerShape(18.dp)
        ButtonSize.Medium -> RoundedCornerShape(12.dp)
    }

    @Composable
    fun colors(
        type: ButtonType,
        size: ButtonSize,
    ): ButtonColors = when (type) {
        ButtonType.Primary -> when (size) {
            ButtonSize.Small -> MaterialButtonDefaults.buttonColors(
                containerColor = ClipyTheme.colors.primary.indigo400,
                contentColor = ClipyTheme.colors.primary.indigo50,
                disabledContainerColor = ClipyTheme.colors.neutral.gray100,
                disabledContentColor = ClipyTheme.colors.neutral.gray800,
            )

            ButtonSize.Medium -> MaterialButtonDefaults.buttonColors(
                containerColor = ClipyTheme.colors.primary.indigo400,
                contentColor = ClipyTheme.colors.primary.indigo50,
                disabledContainerColor = ClipyTheme.colors.primary.indigo300,
                disabledContentColor = ClipyTheme.colors.primary.indigo100,
            )
        }

        ButtonType.Secondary -> MaterialButtonDefaults.buttonColors(
            containerColor = ClipyTheme.colors.neutral.gray100,
            contentColor = ClipyTheme.colors.neutral.gray800,
            disabledContainerColor = ClipyTheme.colors.neutral.gray100,
            disabledContentColor = ClipyTheme.colors.neutral.gray300,
        )
    }

    @Composable
    fun border(type: ButtonType): BorderStroke? = when (type) {
        ButtonType.Primary -> null
        ButtonType.Secondary -> BorderStroke(
            width = 1.dp,
            color = ClipyTheme.colors.neutral.gray200,
        )
    }

    @Composable
    fun textStyle(size: ButtonSize): TextStyle = when (size) {
        ButtonSize.Small -> ClipyTheme.typography.body2Medium
        ButtonSize.Medium -> ClipyTheme.typography.body1Medium
    }
}
