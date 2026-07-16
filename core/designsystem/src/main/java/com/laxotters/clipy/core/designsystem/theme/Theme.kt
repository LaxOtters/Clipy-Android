package com.laxotters.clipy.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
)

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalClipyColors provides DefaultClipyColors,
        LocalClipyGradients provides DefaultClipyGradients,
        LocalClipyTypography provides DefaultClipyTypography,
    ) {
        MaterialTheme(
            colorScheme = resolveMaterialColorScheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
            ),
            content = content,
        )
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

@Composable
private fun resolveMaterialColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
) = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
