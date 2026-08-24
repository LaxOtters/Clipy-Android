package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.compose.material3.SnackbarHostState
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClipySnackbarManagerTest {
    @Test
    fun differentKeys_areShownInFifoOrder() = runTest {
        val manager = createManager()

        manager.showSnackbar(message = "First")
        manager.showSnackbar(message = "Second")
        runCurrent()

        assertEquals(
            "First",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )

        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        assertEquals(
            "Second",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    @Test
    fun sameKey_isNotAddedWhileItIsActiveOrQueued() = runTest {
        val manager = createManager()

        manager.showSnackbar(message = "Active")
        manager.showSnackbar(
            message = "Duplicated active",
            key = "Active",
        )
        manager.showSnackbar(
            message = "Queued",
            key = "Queued",
        )
        manager.showSnackbar(
            message = "Duplicated queued",
            key = "Queued",
        )
        runCurrent()

        assertEquals(
            "Active",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )

        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        assertEquals(
            "Queued",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )

        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        assertNull(manager.hostState.currentSnackbarData)
    }

    @Test
    fun dismissedKey_canBeAddedAgain() = runTest {
        val manager = createManager()

        manager.showSnackbar(
            message = "First",
            key = "Shared",
        )
        runCurrent()
        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        manager.showSnackbar(
            message = "Second",
            key = "Shared",
        )
        runCurrent()

        assertEquals(
            "Second",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    @Test
    fun actionPerformed_invokesActionAndShowsNextRequest() = runTest {
        var actionCount = 0
        val manager = createManager()

        manager.showSnackbar(
            message = "Action",
            action = ClipyTextAction(
                label = "Retry",
                onClick = { actionCount++ },
            ),
        )
        manager.showSnackbar(message = "Next")
        runCurrent()

        manager.hostState.currentSnackbarData?.performAction()
        runCurrent()

        assertEquals(
            1,
            actionCount,
        )
        assertEquals(
            "Next",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    @Test
    fun actionAndIcon_areRejected() = runTest {
        val manager = createManager()

        assertThrows(IllegalArgumentException::class.java) {
            manager.showSnackbar(
                message = "Invalid",
                icon = ClipySnackbarIcon.Error,
                action = ClipyTextAction(
                    label = "Retry",
                    onClick = {},
                ),
            )
        }
    }

    @Test
    fun routeCancellation_removesActiveAndQueuedRequests() = runTest {
        val manager = createManager()

        manager.showSnackbar(message = "Active")
        manager.showSnackbar(message = "Queued")
        runCurrent()

        manager.cancel()
        runCurrent()

        assertNull(manager.hostState.currentSnackbarData)

        manager.showSnackbar(message = "After cancel")
        runCurrent()

        assertNull(manager.hostState.currentSnackbarData)
    }

    private fun TestScope.createManager(): ClipySnackbarManager = ClipySnackbarManager(
        hostState = SnackbarHostState(),
        parentScope = backgroundScope,
    )
}
