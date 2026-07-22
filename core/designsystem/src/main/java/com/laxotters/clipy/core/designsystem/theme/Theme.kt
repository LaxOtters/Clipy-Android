package com.laxotters.clipy.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalClipyColors = staticCompositionLocalOf {
    DefaultClipyColors
}

internal val LocalClipyGradients = staticCompositionLocalOf {
    DefaultClipyGradients
}

internal val LocalClipyTypography = staticCompositionLocalOf {
    DefaultClipyTypography
}

@Composable
fun ClipyTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalClipyColors provides DefaultClipyColors,
        LocalClipyGradients provides DefaultClipyGradients,
        LocalClipyTypography provides DefaultClipyTypography,
    ) {
        // TODO: 다크 모드 도입 시 Clipy 색상 제공 방식을 결정
        MaterialTheme(content = content)
    }
}

object ClipyTheme {
    val colors: ClipyColors
        @Composable get() = LocalClipyColors.current

    val gradients: ClipyGradients
        @Composable get() = LocalClipyGradients.current

    val typography: ClipyTypography
        @Composable get() = LocalClipyTypography.current
}
