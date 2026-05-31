package com.laxotters.clipy.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import kotlin.math.roundToInt

@Composable
fun ClipyBottomSheetLayout(
    value: ClipyBottomSheetValue,
    modifier: Modifier = Modifier,
    headerContent: @Composable ColumnScope.(ClipyBottomSheetValue) -> Unit = {},
    sheetContent: @Composable ColumnScope.(ClipyBottomSheetValue) -> Unit,
) {
    var layoutHeightPx by remember { mutableFloatStateOf(0f) }
    val policy = rememberClipyBottomSheetPolicy(layoutHeightPx)
    val offsetY = policy.offsetFor(value)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                layoutHeightPx = size.height.toFloat()
            },
    ) {
        if (layoutHeightPx > 0f) {
            ClipyBottomSheetSurface(
                value = value,
                offsetY = offsetY,
                hiddenOffset = policy.offsetFor(ClipyBottomSheetValue.HIDDEN),
                headerContent = headerContent,
                sheetContent = sheetContent,
            )
        }
    }
}

@Composable
private fun rememberClipyBottomSheetPolicy(
    availableHeight: Float,
): ClipyBottomSheetPolicy {
    val density = LocalDensity.current
    return with(density) {
        remember(density, availableHeight) {
            val expanded = 0f
            val hidden = (availableHeight - ClipyBottomSheetDefaults.HiddenHeight.toPx())
                .coerceAtLeast(expanded)
            ClipyBottomSheetPolicy(
                anchors = ClipyBottomSheetAnchors(
                    expanded = expanded,
                    peek = (availableHeight - ClipyBottomSheetDefaults.PeekHeight.toPx())
                        .coerceIn(expanded, hidden),
                    minimized = (availableHeight - ClipyBottomSheetDefaults.MinimizedHeight.toPx())
                        .coerceIn(expanded, hidden),
                    hidden = hidden,
                ),
                velocityThreshold = ClipyBottomSheetDefaults.VelocityThreshold.toPx(),
                retentionBand = ClipyBottomSheetDefaults.RetentionBand.toPx(),
            )
        }
    }
}

@Composable
private fun ClipyBottomSheetSurface(
    value: ClipyBottomSheetValue,
    offsetY: Float,
    hiddenOffset: Float,
    modifier: Modifier = Modifier,
    headerContent: @Composable ColumnScope.(ClipyBottomSheetValue) -> Unit,
    sheetContent: @Composable ColumnScope.(ClipyBottomSheetValue) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .offsetY(offsetY)
            .shadow(
                elevation = ClipyBottomSheetDefaults.SheetElevation,
                shape = ClipyBottomSheetDefaults.SheetShape,
                clip = false,
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = ClipyBottomSheetDefaults.SheetShape,
            ),
    ) {
        if (value != ClipyBottomSheetValue.HIDDEN) {
            ClipyBottomSheetHandle()
        }
        headerContent(value)
        ClipyBottomSheetAnimatedContent(
            valueForContent = value,
            sheetContent = sheetContent,
        )
        ClipyBottomSheetBottomSpacer(
            hiddenOffset = hiddenOffset,
            offsetY = offsetY,
        )
    }
}

@Composable
private fun ClipyBottomSheetAnimatedContent(
    valueForContent: ClipyBottomSheetValue,
    modifier: Modifier = Modifier,
    sheetContent: @Composable ColumnScope.(ClipyBottomSheetValue) -> Unit,
) {
    AnimatedContent(
        targetState = valueForContent,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(ClipyBottomSheetDefaults.CONTENT_FADE_IN_MILLIS),
            ) togetherWith
                fadeOut(animationSpec = tween(ClipyBottomSheetDefaults.CONTENT_FADE_OUT_MILLIS))
        },
        label = "ClipyBottomSheetContent",
    ) { contentTargetValue ->
        Column {
            if (contentTargetValue != ClipyBottomSheetValue.HIDDEN) {
                sheetContent(contentTargetValue)
            }
        }
    }
}

@Composable
private fun ClipyBottomSheetBottomSpacer(
    hiddenOffset: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier.height(
            with(LocalDensity.current) {
                (hiddenOffset - offsetY)
                    .coerceAtLeast(0f)
                    .toDp()
            },
        ),
    )
}

@Composable
private fun ClipyBottomSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ClipyBottomSheetDefaults.HandleHeight)
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = ClipyBottomSheetDefaults.HandleWidth,
                    height = ClipyBottomSheetDefaults.HandleIndicatorHeight,
                )
                .background(
                    color = ClipyBottomSheetDefaults.HandleColor,
                    shape = RoundedCornerShape(50),
                ),
        )
    }
}

private fun Modifier.offsetY(offset: Float): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(
                x = 0,
                y = offset.roundToInt(),
            )
        },
    )

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetHiddenPreview() {
    ClipyBottomSheetPreviewContent(value = ClipyBottomSheetValue.HIDDEN)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetMinimizedPreview() {
    ClipyBottomSheetPreviewContent(value = ClipyBottomSheetValue.MINIMIZED)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetPeekPreview() {
    ClipyBottomSheetPreviewContent(value = ClipyBottomSheetValue.PEEK)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetExpandedPreview() {
    ClipyBottomSheetPreviewContent(value = ClipyBottomSheetValue.EXPANDED)
}

@Composable
private fun ClipyBottomSheetPreviewContent(value: ClipyBottomSheetValue) {
    ClipyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            ClipyBottomSheetLayout(
                value = value,
                sheetContent = { targetValue ->
                    ClipyBottomSheetPreviewSheetContent(value = targetValue)
                },
            )
        }
    }
}

@Composable
private fun ClipyBottomSheetPreviewSheetContent(value: ClipyBottomSheetValue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when (value) {
                    ClipyBottomSheetValue.HIDDEN -> 0.dp
                    ClipyBottomSheetValue.MINIMIZED -> 88.dp
                    ClipyBottomSheetValue.PEEK -> 254.dp
                    ClipyBottomSheetValue.EXPANDED -> 360.dp
                },
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        androidx.compose.material3.Text(
            text = value.name,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
