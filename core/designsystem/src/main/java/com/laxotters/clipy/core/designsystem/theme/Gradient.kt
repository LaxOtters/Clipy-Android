@file:Suppress("MatchingDeclarationName")

package com.laxotters.clipy.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

@Immutable
data class ClipyGradients(
    val linear: Brush,
    val linearMint: Brush,
)

// Figma gradientTransform의 역행렬에서 얻은 normalized handle입니다.
private val linearStartHandle = Offset(0.20906431f, -1.2767857f)
private val linearEndHandle = Offset(0.79093564f, 2.2767858f)

// 방향은 유지하고 handle 길이만 줄여 Android surface에서 종료색이 더 일찍 보이게 합니다.
private const val LINEAR_HANDLE_LENGTH_FACTOR = 0.85f

private val linearBrush = figmaLinearGradientBrush(
    start = linearStartHandle,
    end = Offset(
        x = linearStartHandle.x +
            (linearEndHandle.x - linearStartHandle.x) * LINEAR_HANDLE_LENGTH_FACTOR,
        y = linearStartHandle.y +
            (linearEndHandle.y - linearStartHandle.y) * LINEAR_HANDLE_LENGTH_FACTOR,
    ),
    colors = listOf(
        Color(0xFF4A40E0),
        Color(0xFFC3C0FF),
    ),
    colorStops = listOf(0f, 1f),
)

private val linearMintBrush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val boundsSum = size.width + size.height
        val start = boundsSum * LINEAR_MINT_START_FACTOR
        val end = boundsSum * LINEAR_MINT_END_FACTOR

        return LinearGradientShader(
            from = Offset(start, start),
            to = Offset(end, end),
            colors = listOf(
                Color(0xFF4051E0),
                Color(0xFF8680EF),
                DefaultClipyColors.accent.mint100,
            ),
            colorStops = listOf(0f, 0.44150421f, 1f),
        )
    }
}

// Figma의 gradient handle처럼 영역 밖까지 축을 확장해 비율과 무관하게 45도 방향을 유지합니다.
private const val LINEAR_MINT_START_FACTOR = -0.12f
private const val LINEAR_MINT_END_FACTOR = 0.52f

internal val DefaultClipyGradients = ClipyGradients(
    linear = linearBrush,
    linearMint = linearMintBrush,
)

private fun figmaLinearGradientBrush(
    start: Offset,
    end: Offset,
    colors: List<Color>,
    colorStops: List<Float>,
) = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = LinearGradientShader(
        from = Offset(start.x * size.width, start.y * size.height),
        to = Offset(end.x * size.width, end.y * size.height),
        colors = colors,
        colorStops = colorStops,
    )
}
