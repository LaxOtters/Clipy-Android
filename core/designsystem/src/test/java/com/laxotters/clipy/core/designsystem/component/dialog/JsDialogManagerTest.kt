package com.laxotters.clipy.core.designsystem.component.dialog

import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JsDialogManagerTest {
    @Test
    fun firstRequest_isAccepted() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()

        assertTrue(manager.show(request))
        assertSame(request, manager.activeRequestEntry?.request)
    }

    @Test
    fun requestWhileDialogIsActive_isRejectedImmediately() {
        val manager = ClipyJsDialogManager()
        val firstRequest = alertRequest(title = "First")
        manager.show(firstRequest)

        assertFalse(manager.show(alertRequest(title = "Second")))
        assertSame(firstRequest, manager.activeRequestEntry?.request)
    }

    @Test
    fun activeRequestEntry_isClearedAfterActionCompletes() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        var actionCount = 0

        manager.show(request)
        manager.complete(requireNotNull(manager.activeRequestEntry)) { actionCount++ }

        assertEquals(1, actionCount)
        assertNull(manager.activeRequestEntry)
        assertTrue(manager.show(alertRequest(title = "Second")))
    }

    @Test
    fun requestFromActiveAction_isAcceptedAfterSlotIsCleared() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        val nextRequest = alertRequest(title = "Second")
        var nestedRequestAccepted = false

        manager.show(request)
        manager.complete(requireNotNull(manager.activeRequestEntry)) {
            nestedRequestAccepted = manager.show(nextRequest)
        }

        assertTrue(nestedRequestAccepted)
        assertSame(nextRequest, manager.activeRequestEntry?.request)
    }

    @Test
    fun repeatedActionsForSameRequest_onlyFirstActionRuns() {
        val manager = ClipyJsDialogManager()
        var primaryActionCount = 0
        var secondaryActionCount = 0

        manager.show(alertRequest())
        val requestEntry = requireNotNull(manager.activeRequestEntry)
        manager.complete(requestEntry) { primaryActionCount++ }
        manager.complete(requestEntry) { secondaryActionCount++ }

        assertEquals(1, primaryActionCount)
        assertEquals(0, secondaryActionCount)
    }

    @Test
    fun activeRequestEntry_isClearedWhenActionThrows() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()

        manager.show(request)
        runCatching {
            manager.complete(requireNotNull(manager.activeRequestEntry)) { error("Failed action") }
        }

        assertNull(manager.activeRequestEntry)
    }

    @Test
    fun cancelActive_invokesCancellationAndClearsRequest() {
        val manager = ClipyJsDialogManager()
        var cancelCount = 0
        manager.show(alertRequest(onCancel = { cancelCount++ }))

        manager.cancelActive()
        manager.cancelActive()

        assertEquals(1, cancelCount)
        assertNull(manager.activeRequestEntry)
    }

    @Test
    fun requestFromLifecycleCancellation_isRejectedUntilCancellationCompletes() {
        val manager = ClipyJsDialogManager()
        val nextRequest = alertRequest(title = "Second")
        var nestedRequestAccepted = true
        manager.show(
            alertRequest(
                onCancel = {
                    nestedRequestAccepted = manager.show(nextRequest)
                },
            ),
        )

        manager.cancelActive()

        assertFalse(nestedRequestAccepted)
        assertNull(manager.activeRequestEntry)
    }

    @Test
    fun cancelActive_clearsRequestWhenCancellationThrows() {
        val manager = ClipyJsDialogManager()
        manager.show(alertRequest(onCancel = { error("Failed cancellation") }))

        runCatching(manager::cancelActive)

        assertNull(manager.activeRequestEntry)
    }

    @Test
    fun cancelActive_withoutActiveRequest_doesNothing() {
        ClipyJsDialogManager().cancelActive()
    }

    @Test
    fun sameRequest_isWrappedInNewEntryForEachAcceptedShow() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()

        manager.show(request)
        val firstRequestEntry = requireNotNull(manager.activeRequestEntry)
        manager.complete(firstRequestEntry) {}
        manager.show(request)
        val secondRequestEntry = requireNotNull(manager.activeRequestEntry)

        assertTrue(firstRequestEntry !== secondRequestEntry)
        assertSame(request, secondRequestEntry.request)
    }

    @Test
    fun staleRequestEntry_cannotCompleteReusedRequestEntry() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest(title = "Same request")
        manager.show(request)
        val staleRequestEntry = requireNotNull(manager.activeRequestEntry)
        manager.complete(staleRequestEntry) {}

        manager.show(request)
        val activeRequestEntry = requireNotNull(manager.activeRequestEntry)
        var staleActionCount = 0
        manager.complete(staleRequestEntry) { staleActionCount++ }

        assertEquals(0, staleActionCount)
        assertSame(activeRequestEntry, manager.activeRequestEntry)
    }

    private fun alertRequest(
        title: String = "First",
        onCancel: () -> Unit = {},
    ): JsDialogRequest.Alert =
        JsDialogRequest.Alert(
            source = "Request from example.com",
            title = title,
            description = "Description",
            confirmAction = ClipyTextAction(
                label = "Confirm",
                onClick = {},
            ),
            onCancel = onCancel,
        )

    @Test
    fun cancellation_rejectsReentrantCancelAndResponse() {
        val manager = ClipyJsDialogManager()
        var cancellations = 0
        var responses = 0
        manager.show(
            alertRequest(
                onCancel = {
                    cancellations++
                    manager.cancelActive()
                    manager.complete(requireNotNull(manager.activeRequestEntry)) { responses++ }
                },
            ),
        )

        manager.cancelActive()

        assertEquals(1, cancellations)
        assertEquals(0, responses)
        assertNull(manager.activeRequestEntry)
        assertTrue(manager.show(alertRequest()))
    }

    @Test
    fun targetedCancellation_ignoresEqualButDifferentRequest() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        manager.show(request)

        manager.cancelActive(request.copy())

        assertSame(request, manager.activeRequestEntry?.request)
        manager.cancelActive(request)
        assertNull(manager.activeRequestEntry)
    }

    @Test
    fun cancellationFailure_allowsNextRequest() {
        val manager = ClipyJsDialogManager()
        manager.show(alertRequest(onCancel = { error("cancel failed") }))

        runCatching(manager::cancelActive)

        assertTrue(manager.show(alertRequest()))
    }
}
