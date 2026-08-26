package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarPolicyTest {
    @Test
    fun combinedWidthWithinAvailableWidth_keepsActionInline() {
        val result = SnackbarPolicy.resolveActionLayout(
            messageWidth = 100,
            actionWidth = 40,
            messageActionSpacing = 12,
            availableWidth = 152,
        )

        assertEquals(
            SnackbarActionLayout.Inline,
            result,
        )
    }

    @Test
    fun combinedWidthOverAvailableWidth_stacksAction() {
        val result = SnackbarPolicy.resolveActionLayout(
            messageWidth = 100,
            actionWidth = 41,
            messageActionSpacing = 12,
            availableWidth = 152,
        )

        assertEquals(
            SnackbarActionLayout.Stacked,
            result,
        )
    }

    @Test
    fun tapOutsideSnackbar_isOutsideSnackbar() {
        val bounds = Rect(
            left = 20f,
            top = 40f,
            right = 370f,
            bottom = 88f,
        )

        assertFalse(
            SnackbarPolicy.isOutsideSnackbar(
                snackbarBounds = bounds,
                tapPosition = Offset(
                    x = 30f,
                    y = 50f,
                ),
            ),
        )
        assertTrue(
            SnackbarPolicy.isOutsideSnackbar(
                snackbarBounds = bounds,
                tapPosition = Offset(
                    x = 30f,
                    y = 120f,
                ),
            ),
        )
    }

    @Test
    fun tapBeforeSnackbarIsPositioned_isNotOutsideSnackbar() {
        assertFalse(
            SnackbarPolicy.isOutsideSnackbar(
                snackbarBounds = null,
                tapPosition = Offset.Zero,
            ),
        )
    }

    @Test
    fun inactiveSnackbar_onBoundsChanged_keepsOutsideTapState() {
        val activeSnackbarData = FakeSnackbarData(message = "Active")
        val staleSnackbarData = FakeSnackbarData(message = "Stale")
        val state = SnackbarOutsideTapState()

        val result = state.updateBounds(
            activeSnackbarData = activeSnackbarData,
            snackbarData = staleSnackbarData,
            bounds = snackbarBounds,
        )

        assertEquals(
            state,
            result,
        )
    }

    @Test
    fun positionedSnackbar_beforeDismissEnabled_doesNotDismissForOutsideTap() {
        val snackbarData = FakeSnackbarData()
        val state = SnackbarOutsideTapState().updateBounds(
            activeSnackbarData = snackbarData,
            snackbarData = snackbarData,
            bounds = snackbarBounds,
        )

        assertFalse(
            state.shouldDismiss(
                currentSnackbarData = snackbarData,
                tapPosition = outsideTapPosition,
            ),
        )
    }

    @Test
    fun dismissEnabledSnackbar_onInsideTap_doesNotDismiss() {
        val snackbarData = FakeSnackbarData()
        val state = enabledOutsideTapState(snackbarData)

        assertFalse(
            state.shouldDismiss(
                currentSnackbarData = snackbarData,
                tapPosition = Offset(
                    x = 30f,
                    y = 50f,
                ),
            ),
        )
    }

    @Test
    fun dismissEnabledSnackbar_onOutsideTap_dismissesOnlyCurrentSnackbar() {
        val snackbarData = FakeSnackbarData(message = "Current")
        val otherSnackbarData = FakeSnackbarData(message = "Other")
        val state = enabledOutsideTapState(snackbarData)

        assertTrue(
            state.shouldDismiss(
                currentSnackbarData = snackbarData,
                tapPosition = outsideTapPosition,
            ),
        )
        assertFalse(
            state.shouldDismiss(
                currentSnackbarData = otherSnackbarData,
                tapPosition = outsideTapPosition,
            ),
        )
    }

    @Test
    fun staleSnackbar_onDisposed_keepsCurrentOutsideTapState() {
        val staleSnackbarData = FakeSnackbarData(message = "Stale")
        val currentSnackbarData = FakeSnackbarData(message = "Current")
        val state = enabledOutsideTapState(currentSnackbarData)

        val result = state.clearIfMatches(staleSnackbarData)

        assertEquals(
            state,
            result,
        )
    }

    @Test
    fun currentSnackbar_onDisposed_clearsOutsideTapState() {
        val snackbarData = FakeSnackbarData()
        val state = enabledOutsideTapState(snackbarData)

        val result = state.clearIfMatches(snackbarData)

        assertEquals(
            SnackbarOutsideTapState(),
            result,
        )
    }

    private fun enabledOutsideTapState(snackbarData: SnackbarData): SnackbarOutsideTapState =
        SnackbarOutsideTapState()
            .updateBounds(
                activeSnackbarData = snackbarData,
                snackbarData = snackbarData,
                bounds = snackbarBounds,
            )
            .enableDismiss(
                activeSnackbarData = snackbarData,
                snackbarData = snackbarData,
            )

    private companion object {
        val snackbarBounds = Rect(
            left = 20f,
            top = 40f,
            right = 370f,
            bottom = 88f,
        )
        val outsideTapPosition = Offset(
            x = 30f,
            y = 120f,
        )
    }
}

private class FakeSnackbarData(
    message: String = "Message",
) : SnackbarData {
    override val visuals: SnackbarVisuals = object : SnackbarVisuals {
        override val message: String = message
        override val actionLabel: String? = null
        override val withDismissAction: Boolean = false
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite
    }

    override fun performAction() = Unit

    override fun dismiss() = Unit
}
