package com.laxotters.clipy.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class PrimaryPalette(
    val indigo50: Color,
    val indigo100: Color,
    val indigo150: Color,
    val indigo200: Color,
    val indigo300: Color,
    val indigo400: Color,
    val indigo500: Color,
    val indigo600: Color,
    val indigo700: Color,
    val indigo800: Color,
    val indigo900: Color,
)

@Immutable
data class AccentPalette(
    val mint50: Color,
    val mint100: Color,
    val mint200: Color,
    val mint300: Color,
    val mint400: Color,
    val mint500: Color,
    val mint600: Color,
    val mint700: Color,
    val mint800: Color,
    val mint900: Color,
)

@Immutable
data class NeutralPalette(
    val gray50: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray400: Color,
    val gray500: Color,
    val gray600: Color,
    val gray700: Color,
    val gray800: Color,
    val gray900: Color,
    val gray950: Color,
)

@Immutable
data class ErrorPalette(
    val error100: Color,
    val error700: Color,
)

@Immutable
data class AlphaPalette(
    val white10: Color,
    val white20: Color,
    val white70: Color,
    val whitePoint2: Color,
    val black15: Color,
    val black20: Color,
    val black60: Color,
)

@Immutable
data class ClipyColors(
    val primary: PrimaryPalette,
    val accent: AccentPalette,
    val neutral: NeutralPalette,
    val error: ErrorPalette,
    val alpha: AlphaPalette,
    val overlayBackground: Color,
)

private val black20 = Color.Black.copy(alpha = 0.20f)

internal val DefaultClipyColors = ClipyColors(
    primary = PrimaryPalette(
        indigo50 = Color(0xFFFFFFFF),
        indigo100 = Color(0xFFF9F9FE),
        indigo150 = Color(0xFFF4F2FD),
        indigo200 = Color(0xFFCFCCF8),
        indigo300 = Color(0xFFA49FF2),
        indigo400 = Color(0xFF7A73EB),
        indigo500 = Color(0xFF4F46E5),
        indigo600 = Color(0xFF291FD9),
        indigo700 = Color(0xFF2118AD),
        indigo800 = Color(0xFF181280),
        indigo900 = Color(0xFF100C53),
    ),
    accent = AccentPalette(
        mint50 = Color(0xFFFFFFFF),
        mint100 = Color(0xFFD6F7F3),
        mint200 = Color(0xFFACEEE6),
        mint300 = Color(0xFF82E5D9),
        mint400 = Color(0xFF57DDCC),
        mint500 = Color(0xFF2DD4BF),
        mint600 = Color(0xFF23AB9A),
        mint700 = Color(0xFF1A8174),
        mint800 = Color(0xFF12564E),
        mint900 = Color(0xFF092C28),
    ),
    neutral = NeutralPalette(
        gray50 = Color(0xFFFAFAFA),
        gray100 = Color(0xFFF4F4F5),
        gray200 = Color(0xFFE4E4E7),
        gray300 = Color(0xFFD4D4D8),
        gray400 = Color(0xFFA1A1AA),
        gray500 = Color(0xFF71717A),
        gray600 = Color(0xFF52525B),
        gray700 = Color(0xFF3F3F46),
        gray800 = Color(0xFF27272A),
        gray900 = Color(0xFF18181B),
        gray950 = Color(0xFF09090B),
    ),
    error = ErrorPalette(
        error100 = Color(0xFFFFDAD6),
        error700 = Color(0xFFBA1A1A),
    ),
    alpha = AlphaPalette(
        white10 = Color.White.copy(alpha = 0.10f),
        white20 = Color.White.copy(alpha = 0.20f),
        white70 = Color.White.copy(alpha = 0.70f),
        whitePoint2 = Color.White.copy(alpha = 0.002f),
        black15 = Color.Black.copy(alpha = 0.15f),
        black20 = black20,
        black60 = Color.Black.copy(alpha = 0.60f),
    ),
    overlayBackground = black20,
)
