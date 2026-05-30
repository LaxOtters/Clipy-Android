package com.laxotters.clipy.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipyBottomSheetPolicyTest {
    private val policy = ClipyBottomSheetPolicy(
        anchors = ClipyBottomSheetAnchors(
            expanded = 0f,
            peek = 400f,
            minimized = 700f,
            hidden = 1_000f,
        ),
        velocityThreshold = VELOCITY_THRESHOLD,
        retentionBand = RETENTION_BAND,
    )

    @Test
    fun offsetFor_returnsAnchorForState() {
        assertEquals(400f, policy.offsetFor(ClipyBottomSheetValue.PEEK), 0f)
    }

    @Test
    fun offsetY_clampOffsetY_clampsToAnchorRange() {
        assertEquals(0f, policy.clampOffsetY(-100f), 0f)
        assertEquals(1_000f, policy.clampOffsetY(1_200f), 0f)
    }

    @Test
    fun offsetInsideRetentionBand_slowDragRelease_keepsCurrentValue() {
        val targetValue = targetValue(
            currentValue = ClipyBottomSheetValue.PEEK,
            endOffsetY = 410f,
            translationY = 10f,
        )

        assertEquals(ClipyBottomSheetValue.PEEK, targetValue)
    }

    @Test
    fun minimizedDragUp_insideExpandedAnchorBand_targetsExpanded() {
        val targetValue = targetValue(
            currentValue = ClipyBottomSheetValue.MINIMIZED,
            endOffsetY = 20f,
            translationY = -680f,
        )

        assertEquals(ClipyBottomSheetValue.EXPANDED, targetValue)
    }

    @Test
    fun minimizedDragUp_beforeExpandedAnchorBand_targetsPeek() {
        val targetValue = targetValue(
            currentValue = ClipyBottomSheetValue.MINIMIZED,
            endOffsetY = 380f,
            translationY = -320f,
        )

        assertEquals(ClipyBottomSheetValue.PEEK, targetValue)
    }

    @Test
    fun slowDragRelease_targetValue_resolvesByAnchorAndDirection() {
        val cases = listOf(
            DragCase(
                currentValue = ClipyBottomSheetValue.EXPANDED,
                endOffsetY = 390f,
                translationY = 390f,
                expectedValue = ClipyBottomSheetValue.PEEK,
            ),
            DragCase(
                currentValue = ClipyBottomSheetValue.EXPANDED,
                endOffsetY = 690f,
                translationY = 690f,
                expectedValue = ClipyBottomSheetValue.MINIMIZED,
            ),
            DragCase(
                currentValue = ClipyBottomSheetValue.MINIMIZED,
                endOffsetY = 730f,
                translationY = 30f,
                expectedValue = ClipyBottomSheetValue.HIDDEN,
            ),
            DragCase(
                currentValue = ClipyBottomSheetValue.PEEK,
                endOffsetY = 370f,
                translationY = -30f,
                expectedValue = ClipyBottomSheetValue.EXPANDED,
            ),
            DragCase(
                currentValue = ClipyBottomSheetValue.PEEK,
                endOffsetY = 970f,
                translationY = 570f,
                expectedValue = ClipyBottomSheetValue.MINIMIZED,
            ),
        )

        cases.forEach { case ->
            assertEquals(
                case.expectedValue,
                targetValue(
                    currentValue = case.currentValue,
                    endOffsetY = case.endOffsetY,
                    translationY = case.translationY,
                ),
            )
        }
    }

    @Test
    fun fastUpwardVelocity_targetValue_resolvesByDirectionPolicy() {
        val cases = listOf(
            ClipyBottomSheetValue.HIDDEN to ClipyBottomSheetValue.HIDDEN,
            ClipyBottomSheetValue.MINIMIZED to ClipyBottomSheetValue.EXPANDED,
            ClipyBottomSheetValue.PEEK to ClipyBottomSheetValue.EXPANDED,
            ClipyBottomSheetValue.EXPANDED to ClipyBottomSheetValue.EXPANDED,
        )

        cases.forEach { (currentValue, expectedValue) ->
            assertEquals(
                expectedValue,
                targetValue(
                    currentValue = currentValue,
                    endOffsetY = policy.offsetFor(currentValue),
                    velocityY = -1_500f,
                ),
            )
        }
    }

    @Test
    fun fastDownwardVelocity_targetValue_resolvesByDirectionPolicy() {
        val cases = listOf(
            ClipyBottomSheetValue.HIDDEN to ClipyBottomSheetValue.HIDDEN,
            ClipyBottomSheetValue.EXPANDED to ClipyBottomSheetValue.MINIMIZED,
            ClipyBottomSheetValue.PEEK to ClipyBottomSheetValue.MINIMIZED,
            ClipyBottomSheetValue.MINIMIZED to ClipyBottomSheetValue.HIDDEN,
        )

        cases.forEach { (currentValue, expectedValue) ->
            assertEquals(
                expectedValue,
                targetValue(
                    currentValue = currentValue,
                    endOffsetY = policy.offsetFor(currentValue),
                    velocityY = 1_500f,
                ),
            )
        }
    }

    @Test
    fun dragging_valueForContent_resolvesBySlowDragPolicy() {
        val valueForContent = policy.valueForContent(
            ClipyBottomSheetContentContext(
                currentValue = ClipyBottomSheetValue.MINIMIZED,
                offsetY = 380f,
                translationY = -320f,
                isDragging = true,
                settlingTargetValue = null,
            ),
        )

        assertEquals(ClipyBottomSheetValue.PEEK, valueForContent)
    }

    @Test
    fun settlingTargetValue_valueForContent_keepsSettlingTargetValue() {
        val valueForContent = policy.valueForContent(
            ClipyBottomSheetContentContext(
                currentValue = ClipyBottomSheetValue.MINIMIZED,
                offsetY = 710f,
                translationY = 0f,
                isDragging = false,
                settlingTargetValue = ClipyBottomSheetValue.PEEK,
            ),
        )

        assertEquals(ClipyBottomSheetValue.PEEK, valueForContent)
    }

    private fun targetValue(
        currentValue: ClipyBottomSheetValue,
        endOffsetY: Float,
        translationY: Float = 0f,
        velocityY: Float = 0f,
    ): ClipyBottomSheetValue =
        policy.targetValue(
            from = currentValue,
            dragEnd = ClipyBottomSheetDragEnd(
                translationY = translationY,
                velocityY = velocityY,
                endOffsetY = endOffsetY,
            ),
        )

    private data class DragCase(
        val currentValue: ClipyBottomSheetValue,
        val endOffsetY: Float,
        val translationY: Float,
        val expectedValue: ClipyBottomSheetValue,
    )

    private companion object {
        const val RETENTION_BAND = 20f
        const val VELOCITY_THRESHOLD = 1_400f
    }
}
