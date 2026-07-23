package com.laxotters.clipy.core.designsystem.component.bottomsheet

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetPolicyTest {
    private val policy = BottomSheetPolicy(
        anchors = BottomSheetAnchors(
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
        assertEquals(400f, policy.offsetFor(BottomSheetValue.PEEK), 0f)
    }

    @Test
    fun offsetY_clampOffsetY_clampsToAnchorRange() {
        assertEquals(0f, policy.clampOffsetY(-100f), 0f)
        assertEquals(1_000f, policy.clampOffsetY(1_200f), 0f)
    }

    @Test
    fun offsetInsideRetentionBand_slowDragRelease_keepsCurrentValue() {
        val targetValue = targetValue(
            currentValue = BottomSheetValue.PEEK,
            endOffsetY = 410f,
            translationY = 10f,
        )

        assertEquals(BottomSheetValue.PEEK, targetValue)
    }

    @Test
    fun minimizedDragUp_insideExpandedAnchorBand_targetsExpanded() {
        val targetValue = targetValue(
            currentValue = BottomSheetValue.MINIMIZED,
            endOffsetY = 20f,
            translationY = -680f,
        )

        assertEquals(BottomSheetValue.EXPANDED, targetValue)
    }

    @Test
    fun minimizedDragUp_beforeExpandedAnchorBand_targetsPeek() {
        val targetValue = targetValue(
            currentValue = BottomSheetValue.MINIMIZED,
            endOffsetY = 380f,
            translationY = -320f,
        )

        assertEquals(BottomSheetValue.PEEK, targetValue)
    }

    @Test
    fun slowDragRelease_targetValue_resolvesByAnchorAndDirection() {
        val cases = listOf(
            DragCase(
                currentValue = BottomSheetValue.EXPANDED,
                endOffsetY = 390f,
                translationY = 390f,
                expectedValue = BottomSheetValue.PEEK,
            ),
            DragCase(
                currentValue = BottomSheetValue.EXPANDED,
                endOffsetY = 690f,
                translationY = 690f,
                expectedValue = BottomSheetValue.MINIMIZED,
            ),
            DragCase(
                currentValue = BottomSheetValue.MINIMIZED,
                endOffsetY = 730f,
                translationY = 30f,
                expectedValue = BottomSheetValue.HIDDEN,
            ),
            DragCase(
                currentValue = BottomSheetValue.PEEK,
                endOffsetY = 370f,
                translationY = -30f,
                expectedValue = BottomSheetValue.EXPANDED,
            ),
            DragCase(
                currentValue = BottomSheetValue.PEEK,
                endOffsetY = 970f,
                translationY = 570f,
                expectedValue = BottomSheetValue.MINIMIZED,
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
            BottomSheetValue.HIDDEN to BottomSheetValue.HIDDEN,
            BottomSheetValue.MINIMIZED to BottomSheetValue.EXPANDED,
            BottomSheetValue.PEEK to BottomSheetValue.EXPANDED,
            BottomSheetValue.EXPANDED to BottomSheetValue.EXPANDED,
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
            BottomSheetValue.HIDDEN to BottomSheetValue.HIDDEN,
            BottomSheetValue.EXPANDED to BottomSheetValue.MINIMIZED,
            BottomSheetValue.PEEK to BottomSheetValue.MINIMIZED,
            BottomSheetValue.MINIMIZED to BottomSheetValue.HIDDEN,
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
            BottomSheetContentContext(
                currentValue = BottomSheetValue.MINIMIZED,
                offsetY = 380f,
                translationY = -320f,
                isDragging = true,
                settlingTargetValue = null,
            ),
        )

        assertEquals(BottomSheetValue.PEEK, valueForContent)
    }

    @Test
    fun settlingTargetValue_valueForContent_keepsSettlingTargetValue() {
        val valueForContent = policy.valueForContent(
            BottomSheetContentContext(
                currentValue = BottomSheetValue.MINIMIZED,
                offsetY = 710f,
                translationY = 0f,
                isDragging = false,
                settlingTargetValue = BottomSheetValue.PEEK,
            ),
        )

        assertEquals(BottomSheetValue.PEEK, valueForContent)
    }

    private fun targetValue(
        currentValue: BottomSheetValue,
        endOffsetY: Float,
        translationY: Float = 0f,
        velocityY: Float = 0f,
    ): BottomSheetValue =
        policy.targetValue(
            from = currentValue,
            dragEnd = BottomSheetDragEnd(
                translationY = translationY,
                velocityY = velocityY,
                endOffsetY = endOffsetY,
            ),
        )

    private data class DragCase(
        val currentValue: BottomSheetValue,
        val endOffsetY: Float,
        val translationY: Float,
        val expectedValue: BottomSheetValue,
    )

    private companion object {
        const val RETENTION_BAND = 20f
        const val VELOCITY_THRESHOLD = 1_400f
    }
}
