package com.laxotters.clipy.core.designsystem.component.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.CoroutineScope

/**
 * [value]를 기준으로 BottomSheet 위치와 content를 표시합니다.
 *
 * 사용자가 drag를 끝내면 다음 상태를 [onValueChange]로 전달합니다.
 * 호출자는 전달받은 값을 다시 [value]에 반영해 상태를 확정합니다.
 */
@Composable
fun ClipyBottomSheetLayout(
    value: BottomSheetValue,
    onValueChange: (BottomSheetValue) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: @Composable ColumnScope.(BottomSheetValue) -> Unit = {},
    sheetContent: @Composable ColumnScope.(BottomSheetValue) -> Unit,
) {
    val dragController = remember { BottomSheetDragController() }
    // BottomSheet가 움직일 수 있는 부모 영역의 실제 px 높이입니다.
    var layoutHeightPx by remember { mutableFloatStateOf(0f) }
    val policy = rememberBottomSheetPolicy(layoutHeightPx)
    val targetOffset = policy.offsetFor(value)

    LaunchedEffect(layoutHeightPx, policy, value) {
        if (layoutHeightPx > 0f) {
            dragController.syncTo(targetOffset)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                layoutHeightPx = size.height.toFloat()
            },
    ) {
        if (layoutHeightPx > 0f) {
            ClipyBottomSheetSurface(
                presentation = BottomSheetPresentation(
                    value = value,
                    offsetY = dragController.displayedOffset(targetOffset),
                    isDragging = dragController.isDragging,
                    dragTranslationY = dragController.dragTranslationY,
                    settlingTargetValue = dragController.settlingTargetValue,
                ),
                policy = policy,
                dragCallbacks = BottomSheetDragCallbacks(
                    onDragStarted = { dragController.startDrag() },
                    onDrag = { delta ->
                        dragController.dragBy(
                            delta = delta,
                            policy = policy,
                        )
                    },
                    onDragStopped = { velocity ->
                        dragController.settle(
                            currentValue = value,
                            velocity = velocity,
                            policy = policy,
                            onValueChange = onValueChange,
                        )
                    },
                ),
                headerContent = headerContent,
                sheetContent = sheetContent,
            )
        }
    }
}

/**
 * dp로 정의된 visible height를 px offset anchor로 변환합니다.
 *
 * 상태별 anchor offsetY는 availableHeight - visibleHeight입니다.
 */
@Composable
private fun rememberBottomSheetPolicy(
    availableHeight: Float,
): BottomSheetPolicy {
    val density = LocalDensity.current
    val expandedTopOffset = WindowInsets.statusBars.getTop(density).toFloat()
    return with(density) {
        remember(density, availableHeight, expandedTopOffset) {
            val expanded = expandedTopOffset
            val hidden = (availableHeight - BottomSheetDefaults.HiddenHeight.toPx())
                .coerceAtLeast(expanded)
            BottomSheetPolicy(
                anchors = BottomSheetAnchors(
                    expanded = expanded,
                    peek = (availableHeight - BottomSheetDefaults.PeekHeight.toPx())
                        .coerceIn(expanded, hidden),
                    minimized = (availableHeight - BottomSheetDefaults.MinimizedHeight.toPx())
                        .coerceIn(expanded, hidden),
                    hidden = hidden,
                ),
                velocityThreshold = BottomSheetDefaults.VelocityThreshold.toPx(),
                retentionBand = BottomSheetDefaults.RetentionBand.toPx(),
            )
        }
    }
}

@Composable
private fun ClipyBottomSheetSurface(
    presentation: BottomSheetPresentation,
    policy: BottomSheetPolicy,
    dragCallbacks: BottomSheetDragCallbacks,
    modifier: Modifier = Modifier,
    headerContent: @Composable ColumnScope.(BottomSheetValue) -> Unit,
    sheetContent: @Composable ColumnScope.(BottomSheetValue) -> Unit,
) {
    val valueForContent = policy.valueForContent(
        BottomSheetContentContext(
            currentValue = presentation.value,
            offsetY = presentation.offsetY,
            translationY = presentation.dragTranslationY,
            isDragging = presentation.isDragging,
            settlingTargetValue = presentation.settlingTargetValue,
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .offsetY(presentation.offsetY)
            .shadow(
                elevation = BottomSheetDefaults.SheetElevation,
                shape = BottomSheetDefaults.SheetShape,
                clip = false,
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = BottomSheetDefaults.SheetShape,
            ),
    ) {
        if (presentation.value != BottomSheetValue.HIDDEN) {
            ClipyBottomSheetHandle(
                dragCallbacks = dragCallbacks,
                enabled = true,
            )
        }
        headerContent(valueForContent)
        ClipyBottomSheetAnimatedContent(
            valueForContent = valueForContent,
            sheetContent = sheetContent,
        )
        ClipyBottomSheetBottomSpacer(
            hiddenOffset = policy.offsetFor(BottomSheetValue.HIDDEN),
            offsetY = presentation.offsetY,
        )
    }
}

private data class BottomSheetPresentation(
    val value: BottomSheetValue,
    val offsetY: Float,
    val isDragging: Boolean,
    val dragTranslationY: Float,
    val settlingTargetValue: BottomSheetValue?,
)

private data class BottomSheetDragCallbacks(
    val onDragStarted: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragStopped: suspend CoroutineScope.(Float) -> Unit,
)

@Composable
private fun ClipyBottomSheetAnimatedContent(
    valueForContent: BottomSheetValue,
    modifier: Modifier = Modifier,
    sheetContent: @Composable ColumnScope.(BottomSheetValue) -> Unit,
) {
    AnimatedContent(
        targetState = valueForContent,
        modifier = modifier,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(BottomSheetDefaults.CONTENT_FADE_IN_MILLIS),
            ) togetherWith
                fadeOut(animationSpec = tween(BottomSheetDefaults.CONTENT_FADE_OUT_MILLIS))
        },
        label = "ClipyBottomSheetContent",
    ) { contentTargetValue ->
        Column {
            if (contentTargetValue != BottomSheetValue.HIDDEN) {
                sheetContent(contentTargetValue)
            }
        }
    }
}

/** sheet가 아래로 내려간 만큼 부족해지는 Column 하단 높이를 채웁니다. */
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

/**
 * BottomSheet의 현재 offsetY와 target anchor 이동을 관리합니다.
 */
private class BottomSheetDragController {
    private var currentOffsetY by mutableFloatStateOf(0f)
    private val offsetYAnimatable = Animatable(0f)
    private var isPositionInitialized by mutableStateOf(false)

    var isDragging by mutableStateOf(false)
        private set

    // drag 시작점부터 현재 지점까지 누적된 y축 이동량입니다.
    // 음수는 위로 끄는 방향이고, 양수는 아래로 끄는 방향입니다.
    var dragTranslationY by mutableFloatStateOf(0f)
        private set

    var isSettling by mutableStateOf(false)
        private set

    // settle 중에는 drag 종료 시점의 target content를 유지합니다.
    var settlingTargetValue by mutableStateOf<BottomSheetValue?>(null)
        private set

    /** 현재 프레임에서 화면에 적용할 offsetY를 반환합니다. */
    fun displayedOffset(targetOffset: Float): Float = when {
        !isPositionInitialized -> targetOffset
        isSettling -> offsetYAnimatable.value
        else -> currentOffsetY
    }

    fun startDrag() {
        settlingTargetValue = null
        dragTranslationY = 0f
        isDragging = true
    }

    fun dragBy(
        delta: Float,
        policy: BottomSheetPolicy,
    ) {
        dragTranslationY += delta
        currentOffsetY = policy.clampOffsetY(currentOffsetY + delta)
    }

    /** 외부에서 전달된 [targetOffset]으로 sheet 위치를 동기화합니다. */
    suspend fun syncTo(targetOffset: Float) {
        if (!isPositionInitialized) {
            offsetYAnimatable.snapTo(targetOffset)
            currentOffsetY = targetOffset
            isPositionInitialized = true
        } else if (currentOffsetY != targetOffset) {
            settlingTargetValue = null
            isSettling = true
            offsetYAnimatable.stop()
            offsetYAnimatable.snapTo(currentOffsetY)
            offsetYAnimatable.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(),
            )
            currentOffsetY = offsetYAnimatable.value
            isSettling = false
        }
    }

    /** drag 종료 결과를 target state로 변환하고 target anchor까지 이동합니다. */
    suspend fun settle(
        currentValue: BottomSheetValue,
        velocity: Float,
        policy: BottomSheetPolicy,
        onValueChange: (BottomSheetValue) -> Unit,
    ) {
        val targetValue = policy.targetValue(
            from = currentValue,
            dragEnd = BottomSheetDragEnd(
                translationY = dragTranslationY,
                velocityY = velocity,
                endOffsetY = currentOffsetY,
            ),
        )

        settlingTargetValue = targetValue
        isDragging = false
        dragTranslationY = 0f
        if (targetValue != currentValue) {
            onValueChange(targetValue)
        }
        isSettling = true
        offsetYAnimatable.snapTo(currentOffsetY)
        offsetYAnimatable.animateTo(
            targetValue = policy.offsetFor(targetValue),
            animationSpec = spring(),
        )
        currentOffsetY = offsetYAnimatable.value
        isSettling = false
        settlingTargetValue = null
    }
}

@Composable
private fun ClipyBottomSheetHandle(
    dragCallbacks: BottomSheetDragCallbacks,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomSheetDefaults.HandleHeight)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState(onDelta = dragCallbacks.onDrag),
                enabled = enabled,
                onDragStarted = { dragCallbacks.onDragStarted() },
                onDragStopped = dragCallbacks.onDragStopped,
            )
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = BottomSheetDefaults.HandleWidth,
                    height = BottomSheetDefaults.HandleIndicatorHeight,
                )
                .background(
                    color = BottomSheetDefaults.HandleColor,
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
    BottomSheetPreviewContent(value = BottomSheetValue.HIDDEN)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetMinimizedPreview() {
    BottomSheetPreviewContent(value = BottomSheetValue.MINIMIZED)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetPeekPreview() {
    BottomSheetPreviewContent(value = BottomSheetValue.PEEK)
}

@Preview(showBackground = true)
@Composable
private fun ClipyBottomSheetExpandedPreview() {
    BottomSheetPreviewContent(value = BottomSheetValue.EXPANDED)
}

@Composable
private fun BottomSheetPreviewContent(value: BottomSheetValue) {
    ClipyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            ClipyBottomSheetLayout(
                value = value,
                onValueChange = {},
                sheetContent = { targetValue ->
                    BottomSheetPreviewSheetContent(value = targetValue)
                },
            )
        }
    }
}

@Composable
private fun BottomSheetPreviewSheetContent(value: BottomSheetValue) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when (value) {
                    BottomSheetValue.HIDDEN -> 0.dp
                    BottomSheetValue.MINIMIZED -> 88.dp
                    BottomSheetValue.PEEK -> 254.dp
                    BottomSheetValue.EXPANDED -> 360.dp
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
