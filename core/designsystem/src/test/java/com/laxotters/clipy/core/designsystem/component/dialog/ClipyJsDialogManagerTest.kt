package com.laxotters.clipy.core.designsystem.component.dialog

import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipyJsDialogManagerTest {
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
    fun requestFromActiveAction_isRejectedBeforeSlotIsCleared() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        var nestedRequestAccepted = true

        manager.show(request)
        manager.complete(requireNotNull(manager.activeRequestEntry)) {
            nestedRequestAccepted = manager.show(alertRequest(title = "Second"))
        }

        assertFalse(nestedRequestAccepted)
        assertNull(manager.activeRequestEntry)
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

    private fun alertRequest(title: String = "First"): ClipyJsDialogRequest.Alert =
        ClipyJsDialogRequest.Alert(
            source = "Request from example.com",
            title = title,
            description = "Description",
            confirmAction = ClipyTextAction(
                label = "Confirm",
                onClick = {},
            ),
        )
}
