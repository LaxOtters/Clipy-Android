package com.laxotters.clipy.core.designsystem.component

import kotlin.math.abs

internal data class ClipyBottomSheetAnchors(
    val expanded: Float,
    val peek: Float,
    val minimized: Float,
    val hidden: Float,
) {
    fun offsetFor(value: ClipyBottomSheetValue): Float =
        when (value) {
            ClipyBottomSheetValue.HIDDEN -> hidden
            ClipyBottomSheetValue.MINIMIZED -> minimized
            ClipyBottomSheetValue.PEEK -> peek
            ClipyBottomSheetValue.EXPANDED -> expanded
        }
}

internal data class ClipyBottomSheetDragEnd(
    val translationY: Float,
    val velocityY: Float,
    val endOffsetY: Float,
)

internal data class ClipyBottomSheetContentContext(
    val currentValue: ClipyBottomSheetValue,
    val offsetY: Float,
    val translationY: Float,
    val isDragging: Boolean,
    val settlingTargetValue: ClipyBottomSheetValue?,
)

internal data class ClipyBottomSheetPolicy(
    val anchors: ClipyBottomSheetAnchors,
    val velocityThreshold: Float,
    val retentionBand: Float,
) {
    fun offsetFor(value: ClipyBottomSheetValue): Float =
        anchors.offsetFor(value)

    fun clampOffsetY(offsetY: Float): Float =
        offsetY.coerceIn(
            minimumValue = anchors.expanded,
            maximumValue = anchors.hidden,
        )

    fun targetValue(
        from: ClipyBottomSheetValue,
        dragEnd: ClipyBottomSheetDragEnd,
    ): ClipyBottomSheetValue {
        if (from == ClipyBottomSheetValue.HIDDEN) {
            return ClipyBottomSheetValue.HIDDEN
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

    fun valueForContent(context: ClipyBottomSheetContentContext): ClipyBottomSheetValue =
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
        from: ClipyBottomSheetValue,
        translationY: Float,
        endOffsetY: Float,
    ): ClipyBottomSheetValue {
        if (from == ClipyBottomSheetValue.HIDDEN) {
            return ClipyBottomSheetValue.HIDDEN
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
        from: ClipyBottomSheetValue,
        direction: DragDirection,
    ): ClipyBottomSheetValue =
        when (from) {
            ClipyBottomSheetValue.HIDDEN -> ClipyBottomSheetValue.HIDDEN
            ClipyBottomSheetValue.MINIMIZED -> when (direction) {
                DragDirection.UPWARD -> ClipyBottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> ClipyBottomSheetValue.HIDDEN
            }
            ClipyBottomSheetValue.PEEK -> when (direction) {
                DragDirection.UPWARD -> ClipyBottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> ClipyBottomSheetValue.MINIMIZED
            }
            ClipyBottomSheetValue.EXPANDED -> when (direction) {
                DragDirection.UPWARD -> ClipyBottomSheetValue.EXPANDED
                DragDirection.DOWNWARD -> ClipyBottomSheetValue.MINIMIZED
            }
        }

    private fun slowTargetValue(
        from: ClipyBottomSheetValue,
        direction: DragDirection,
        endOffset: Float,
    ): ClipyBottomSheetValue {
        val metrics = SlowDragMetrics(
            currentOffset = anchors.offsetFor(from),
            endOffset = endOffset,
            expandedOffset = anchors.expanded,
            minimizedOffset = anchors.minimized,
            retentionBand = retentionBand.coerceAtLeast(0f),
        )

        return when (from) {
            ClipyBottomSheetValue.HIDDEN -> ClipyBottomSheetValue.HIDDEN
            ClipyBottomSheetValue.EXPANDED -> slowExpandedTarget(direction, metrics)
            ClipyBottomSheetValue.PEEK -> slowPeekTarget(direction, metrics)
            ClipyBottomSheetValue.MINIMIZED -> slowMinimizedTarget(direction, metrics)
        }
    }

    private fun slowExpandedTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): ClipyBottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> ClipyBottomSheetValue.EXPANDED
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> ClipyBottomSheetValue.EXPANDED
                metrics.isNearMinimized() -> ClipyBottomSheetValue.MINIMIZED
                else -> ClipyBottomSheetValue.PEEK
            }
        }

    private fun slowPeekTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): ClipyBottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> when {
                metrics.isNearCurrentWhenDraggingUp() -> ClipyBottomSheetValue.PEEK
                else -> ClipyBottomSheetValue.EXPANDED
            }
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> ClipyBottomSheetValue.PEEK
                else -> ClipyBottomSheetValue.MINIMIZED
            }
        }

    private fun slowMinimizedTarget(
        direction: DragDirection,
        metrics: SlowDragMetrics,
    ): ClipyBottomSheetValue =
        when (direction) {
            DragDirection.UPWARD -> when {
                metrics.isNearCurrentWhenDraggingUp() -> ClipyBottomSheetValue.MINIMIZED
                metrics.isNearExpanded() -> ClipyBottomSheetValue.EXPANDED
                else -> ClipyBottomSheetValue.PEEK
            }
            DragDirection.DOWNWARD -> when {
                metrics.isNearCurrentWhenDraggingDown() -> ClipyBottomSheetValue.MINIMIZED
                else -> ClipyBottomSheetValue.HIDDEN
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
