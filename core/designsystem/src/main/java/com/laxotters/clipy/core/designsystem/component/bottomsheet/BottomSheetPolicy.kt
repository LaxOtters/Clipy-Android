package com.laxotters.clipy.core.designsystem.component.bottomsheet

import kotlin.math.abs

/** 상태별 BottomSheet top anchor(offsetY)입니다. */
internal data class BottomSheetAnchors(
    val expanded: Float,
    val peek: Float,
    val minimized: Float,
    val hidden: Float,
) {
    fun offsetFor(value: BottomSheetValue): Float =
        when (value) {
            BottomSheetValue.HIDDEN -> hidden
            BottomSheetValue.MINIMIZED -> minimized
            BottomSheetValue.PEEK -> peek
            BottomSheetValue.EXPANDED -> expanded
        }
}

/** drag 종료 후 target state를 판단할 때 사용하는 drag 결과입니다. */
internal data class BottomSheetDragEnd(
    val translationY: Float,
    val velocityY: Float,
    val endOffsetY: Float,
)

/** headerContent와 sheetContent에 전달할 value 판단에 필요한 현재 sheet 정보입니다. */
internal data class BottomSheetContentContext(
    val currentValue: BottomSheetValue,
    val offsetY: Float,
    val translationY: Float,
    val isDragging: Boolean,
    val settlingTargetValue: BottomSheetValue?,
)

/**
 * anchor와 drag 결과를 기준으로 target state를 결정합니다.
 * 또한 headerContent와 sheetContent에 전달할 value를 결정합니다.
 *
 * anchor는 BottomSheet top의 offsetY입니다.
 * offsetY가 작을수록 sheet는 위에 있고, 커질수록 아래로 내려갑니다.
 */
internal data class BottomSheetPolicy(
    val anchors: BottomSheetAnchors,
    val velocityThreshold: Float,
    val retentionBand: Float,
) {
    fun offsetFor(value: BottomSheetValue): Float =
        anchors.offsetFor(value)

    fun clampOffsetY(offsetY: Float): Float =
        offsetY.coerceIn(
            minimumValue = anchors.expanded,
            maximumValue = anchors.hidden,
        )

    fun targetValue(
        from: BottomSheetValue,
        dragEnd: BottomSheetDragEnd,
    ): BottomSheetValue {
        if (from == BottomSheetValue.HIDDEN) {
            return BottomSheetValue.HIDDEN
        }

        val fastDirection = fastDragDirection(dragEnd.velocityY)
        if (fastDirection != null) {
            return fastTargetValue(
                from = from,
                direction = fastDirection,
            )
        }

        return slowTargetValue(
            from = from,
            translationY = dragEnd.translationY,
            endOffsetY = dragEnd.endOffsetY,
        )
    }

    fun valueForContent(context: BottomSheetContentContext): BottomSheetValue =
        when {
            context.isDragging -> slowTargetValue(
                from = context.currentValue,
                translationY = context.translationY,
                endOffsetY = context.offsetY,
            )
            context.settlingTargetValue != null -> context.settlingTargetValue
            else -> context.currentValue
        }

    private fun slowTargetValue(
        from: BottomSheetValue,
        translationY: Float,
        endOffsetY: Float,
    ): BottomSheetValue {
        if (from == BottomSheetValue.HIDDEN) {
            return BottomSheetValue.HIDDEN
        }

        val endOffset = clampOffsetY(endOffsetY)
        val currentOffset = anchors.offsetFor(from)
        val direction = dragDirection(
            translationY = translationY,
            currentOffset = currentOffset,
            endOffset = endOffset,
        ) ?: return from

        return slowTargetValue(
            from = from,
            direction = direction,
            endOffset = endOffset,
        )
    }

    private fun fastDragDirection(velocityY: Float): DragDirection? {
        if (abs(velocityY) < velocityThreshold) {
            return null
        }

        return if (velocityY < 0f) {
            DragDirection.UPWARD
        } else {
            DragDirection.DOWNWARD
        }
    }

    private fun fastTargetValue(
        from: BottomSheetValue,
        direction: DragDirection,
    ): BottomSheetValue =
        when (from) {
            BottomSheetValue.HIDDEN -> BottomSheetValue.HIDDEN
            BottomSheetValue.MINIMIZED -> when (direction) {
                DragDirection.UPWARD -> BottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> BottomSheetValue.HIDDEN
            }
            BottomSheetValue.PEEK -> when (direction) {
                DragDirection.UPWARD -> BottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> BottomSheetValue.MINIMIZED
            }
            BottomSheetValue.EXPANDED -> when (direction) {
                DragDirection.UPWARD -> BottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> BottomSheetValue.MINIMIZED
            }
        }

    private fun slowTargetValue(
        from: BottomSheetValue,
        direction: DragDirection,
        endOffset: Float,
    ): BottomSheetValue {
        val metrics = SlowDragMetrics(
            currentOffset = anchors.offsetFor(from),
            endOffset = endOffset,
            expandedOffset = anchors.expanded,
            minimizedOffset = anchors.minimized,
            retentionBand = retentionBand.coerceAtLeast(0f),
        )

        return when (from) {
            BottomSheetValue.HIDDEN -> BottomSheetValue.HIDDEN
            BottomSheetValue.EXPANDED -> slowExpandedTarget(direction, metrics)
            BottomSheetValue.PEEK -> slowPeekTarget(direction, metrics)
            BottomSheetValue.MINIMIZED -> slowMinimizedTarget(direction, metrics)
        }
    }

    private fun slowExpandedTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): BottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> BottomSheetValue.EXPANDED
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> BottomSheetValue.EXPANDED
                metrics.isNearMinimized() -> BottomSheetValue.MINIMIZED
                else -> BottomSheetValue.PEEK
            }
        }

    private fun slowPeekTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): BottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> when {
                metrics.isNearCurrentWhenDraggingUp() -> BottomSheetValue.PEEK
                else -> BottomSheetValue.EXPANDED
            }
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> BottomSheetValue.PEEK
                else -> BottomSheetValue.MINIMIZED
            }
        }

    private fun slowMinimizedTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): BottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> when {
                metrics.isNearCurrentWhenDraggingUp() -> BottomSheetValue.MINIMIZED
                metrics.isNearExpanded() -> BottomSheetValue.EXPANDED
                else -> BottomSheetValue.PEEK
            }
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> BottomSheetValue.MINIMIZED
                else -> BottomSheetValue.HIDDEN
            }
        }

    private fun dragDirection(
        translationY: Float,
        currentOffset: Float,
        endOffset: Float,
    ): DragDirection? =
        when {
            translationY < 0f -> DragDirection.UPWARD
            translationY > 0f -> DragDirection.DOWNWARD
            endOffset < currentOffset -> DragDirection.UPWARD
            endOffset > currentOffset -> DragDirection.DOWNWARD
            else -> null
        }

    private enum class DragDirection {
        UPWARD,
        DOWNWARD,
    }

    private data class SlowDragMetrics(
        val currentOffset: Float,
        val endOffset: Float,
        val expandedOffset: Float,
        val minimizedOffset: Float,
        val retentionBand: Float,
    ) {
        fun isNearCurrentWhenDraggingUp(): Boolean =
            endOffset >= currentOffset - retentionBand

        fun isNearCurrentWhenDraggingDown(): Boolean =
            endOffset <= currentOffset + retentionBand

        fun isNearExpanded(): Boolean =
            endOffset <= expandedOffset + retentionBand

        fun isNearMinimized(): Boolean =
            endOffset >= minimizedOffset - retentionBand
    }
}
