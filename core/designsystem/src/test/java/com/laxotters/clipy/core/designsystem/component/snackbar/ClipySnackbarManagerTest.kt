package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.compose.material3.SnackbarHostState
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClipySnackbarManagerTest {
    @Test
    fun differentKeys_areShownInFifoOrder() = runTest {
        val manager = createManager()
        val controller = createController(manager)

        launchSnackbar(controller, message = "First")
        launchSnackbar(controller, message = "Second")
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
        val controller = createController(manager)

        launchSnackbar(controller, message = "Active")
        launchSnackbar(
            controller = controller,
            message = "Duplicated active",
            key = "Active",
        )
        launchSnackbar(
            controller = controller,
            message = "Queued",
            key = "Queued",
        )
        launchSnackbar(
            controller = controller,
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
        val controller = createController(manager)

        launchSnackbar(
            controller = controller,
            message = "First",
            key = "Shared",
        )
        runCurrent()
        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        launchSnackbar(
            controller = controller,
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
        val controller = createController(manager)

        launchSnackbar(
            controller = controller,
            message = "Action",
            action = ClipyTextAction(
                label = "Retry",
                onClick = { actionCount++ },
            ),
        )
        launchSnackbar(controller, message = "Next")
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
    fun actionCallback_canEnqueueSameKeyAgain() = runTest {
        val manager = createManager()
        val controller = createController(manager)

        launchSnackbar(
            controller = controller,
            message = "First",
            key = "Shared",
            action = ClipyTextAction(
                label = "Retry",
                onClick = {
                    launchSnackbar(
                        controller = controller,
                        message = "Retried",
                        key = "Shared",
                    )
                },
            ),
        )
        runCurrent()

        manager.hostState.currentSnackbarData?.performAction()
        runCurrent()

        assertEquals(
            "Retried",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    @Test
    fun sameKeyFromAction_isQueuedAfterExistingRequest() = runTest {
        val manager = createManager()
        val controller = createController(manager)

        launchSnackbar(
            controller = controller,
            message = "First",
            key = "Shared",
            action = ClipyTextAction(
                label = "Retry",
                onClick = {
                    launchSnackbar(
                        controller = controller,
                        message = "Retried",
                        key = "Shared",
                    )
                },
            ),
        )
        launchSnackbar(
            controller = controller,
            message = "Already queued",
            key = "Queued",
        )
        runCurrent()

        manager.hostState.currentSnackbarData?.performAction()
        runCurrent()

        assertEquals(
            "Already queued",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )

        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        assertEquals(
            "Retried",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    @Test
    fun actionAndIcon_areRejected() = runTest {
        val controller = createController(createManager())

        val exception = try {
            controller.showSnackbar(
                message = "Invalid",
                icon = ClipySnackbarIcon.Error,
                action = ClipyTextAction(
                    label = "Retry",
                    onClick = {},
                ),
            )
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertNotNull(exception)
    }

    @Test
    fun callerScopeCancellation_removesOnlyItsActiveAndQueuedRequests() = runTest {
        val manager = createManager()
        val controller = createController(manager)
        val canceledCallerScope = createCallerScope()
        val otherCallerScope = createCallerScope()

        canceledCallerScope.scope.launch {
            controller.showSnackbar(message = "Canceled scope active")
        }
        otherCallerScope.scope.launch {
            controller.showSnackbar(message = "Other scope queued")
        }
        canceledCallerScope.scope.launch {
            controller.showSnackbar(message = "Canceled scope queued")
        }
        runCurrent()

        canceledCallerScope.scope.cancel()
        runCurrent()

        assertEquals(
            "Other scope queued",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )

        manager.hostState.currentSnackbarData?.dismiss()
        runCurrent()

        assertNull(manager.hostState.currentSnackbarData)
    }

    @Test
    fun canceledRequest_doesNotInvokeActionCallback() = runTest {
        var actionCount = 0
        val manager = createManager()
        val controller = createController(manager)

        val requestJob = launchSnackbar(
            controller = controller,
            message = "Canceled action",
            action = ClipyTextAction(
                label = "Retry",
                onClick = { actionCount++ },
            ),
        )
        runCurrent()

        requestJob.cancel()
        runCurrent()

        assertEquals(
            0,
            actionCount,
        )
        assertNull(manager.hostState.currentSnackbarData)
    }

    @Test
    fun canceledRequestKey_canBeAddedByAnotherCaller() = runTest {
        val manager = createManager()
        val controller = createController(manager)

        val canceledRequest = launchSnackbar(
            controller = controller,
            message = "Canceled scope",
            key = "Shared",
        )
        runCurrent()

        canceledRequest.cancel()
        runCurrent()
        launchSnackbar(
            controller = controller,
            message = "Other scope",
            key = "Shared",
        )
        runCurrent()

        assertEquals(
            "Other scope",
            manager.hostState.currentSnackbarData?.visuals?.message,
        )
    }

    private fun createManager(): ClipySnackbarManager = ClipySnackbarManager(
        hostState = SnackbarHostState(),
    )

    private fun createController(manager: ClipySnackbarManager): ClipySnackbarController =
        ClipySnackbarController(manager)

    private fun TestScope.createCallerScope(): CallerScope {
        val parentJob = SupervisorJob(backgroundScope.coroutineContext[Job])
        val parentScope = CoroutineScope(backgroundScope.coroutineContext + parentJob)

        return CallerScope(
            scope = parentScope,
        )
    }

    private fun TestScope.launchSnackbar(
        controller: ClipySnackbarController,
        message: String,
        icon: ClipySnackbarIcon? = null,
        action: ClipyTextAction? = null,
        key: String = message,
    ): Job = backgroundScope.launch {
        controller.showSnackbar(
            message = message,
            icon = icon,
            action = action,
            key = key,
        )
    }

    private data class CallerScope(
        val scope: CoroutineScope,
    )
}
