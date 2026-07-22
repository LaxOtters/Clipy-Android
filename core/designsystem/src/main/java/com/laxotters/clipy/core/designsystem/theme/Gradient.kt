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

private val linearBrush = diagonal45GradientBrush(
    colors = listOf(
        Color(0xFF4A40E0),
        Color(0xFFC3C0FF),
    ),
    colorStops = listOf(0f, 1f),
)

private val linearMintBrush = diagonal45GradientBrush(
    colors = listOf(
        Color(0xFF4051E0),
        Color(0xFF8680EF),
        DefaultClipyColors.accent.mint100,
    ),
    colorStops = listOf(0f, 0.45f, 1f),
)

internal val DefaultClipyGradients = ClipyGradients(
    linear = linearBrush,
    linearMint = linearMintBrush,
)

/**
 * 좌상단에서 우하단으로 향하는 45도 LinearGradient를 만듭니다.
 *
 * 시작점과 종료점은 `width + height` 기준 축의 -12%, 52%입니다.
 * 시작점이 음수이므로 뷰의 좌상단 밖에서 gradient가 시작됩니다.
 */
private fun diagonal45GradientBrush(
    colors: List<Color>,
    colorStops: List<Float>,
) = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val axisLength = size.width + size.height
        val start = axisLength * -0.12f
        val end = axisLength * 0.52f

        return LinearGradientShader(
            from = Offset(start, start),
            to = Offset(end, end),
            colors = colors,
            colorStops = colorStops,
        )
    }
}
