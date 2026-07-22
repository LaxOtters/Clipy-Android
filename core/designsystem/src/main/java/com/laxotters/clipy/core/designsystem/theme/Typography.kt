package com.laxotters.clipy.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.laxotters.clipy.core.designsystem.R

internal val pretendardFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

@Suppress("LongParameterList")
@Immutable
data class ClipyTypography(
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val heading4: TextStyle,
    val body1SemiBold: TextStyle,
    val body1Medium: TextStyle,
    val body1Regular: TextStyle,
    val body2SemiBold: TextStyle,
    val body2Medium: TextStyle,
    val body2Regular: TextStyle,
    val body3Bold: TextStyle,
    val body3SemiBold: TextStyle,
    val body3Medium: TextStyle,
    val body3Regular: TextStyle,
    val tag1Bold: TextStyle,
    val tag2Bold: TextStyle,
)

private fun pretendardTextStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = pretendardFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
    letterSpacing = letterSpacing.sp,
)

internal val DefaultClipyTypography = ClipyTypography(
    heading1 = pretendardTextStyle(FontWeight.Bold, 28, 38, -0.6f),
    heading2 = pretendardTextStyle(FontWeight.Bold, 24, 34, -0.4f),
    heading3 = pretendardTextStyle(FontWeight.Bold, 20, 30, -0.2f),
    heading4 = pretendardTextStyle(FontWeight.SemiBold, 18, 28),
    body1SemiBold = pretendardTextStyle(FontWeight.SemiBold, 16, 24),
    body1Medium = pretendardTextStyle(FontWeight.Medium, 16, 24),
    body1Regular = pretendardTextStyle(FontWeight.Normal, 16, 24),
    body2SemiBold = pretendardTextStyle(FontWeight.SemiBold, 14, 20),
    body2Medium = pretendardTextStyle(FontWeight.Medium, 14, 20),
    body2Regular = pretendardTextStyle(FontWeight.Normal, 14, 20),
    body3Bold = pretendardTextStyle(FontWeight.Bold, 12, 16),
    body3SemiBold = pretendardTextStyle(FontWeight.SemiBold, 12, 16),
    body3Medium = pretendardTextStyle(FontWeight.Medium, 12, 16),
    body3Regular = pretendardTextStyle(FontWeight.Normal, 12, 16),
    tag1Bold = pretendardTextStyle(FontWeight.Bold, 10, 15, 0.8f),
    tag2Bold = pretendardTextStyle(FontWeight.Bold, 8, 10, 0.6f),
)
