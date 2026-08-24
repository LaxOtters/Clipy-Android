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
        assertSame(request, manager.activeRequest)
    }

    @Test
    fun requestWhileDialogIsActive_isRejectedImmediately() {
        val manager = ClipyJsDialogManager()
        val firstRequest = alertRequest(title = "First")
        manager.show(firstRequest)

        assertFalse(manager.show(alertRequest(title = "Second")))
        assertSame(firstRequest, manager.activeRequest)
    }

    @Test
    fun activeRequest_isClearedAfterActionCompletes() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        var actionCount = 0

        manager.show(request)
        manager.complete(request) { actionCount++ }

        assertEquals(1, actionCount)
        assertNull(manager.activeRequest)
        assertTrue(manager.show(alertRequest(title = "Second")))
    }

    @Test
    fun requestFromActiveAction_isRejectedBeforeSlotIsCleared() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()
        var nestedRequestAccepted = true

        manager.show(request)
        manager.complete(request) {
            nestedRequestAccepted = manager.show(alertRequest(title = "Second"))
        }

        assertFalse(nestedRequestAccepted)
        assertNull(manager.activeRequest)
    }

    @Test
    fun activeRequest_isClearedWhenActionThrows() {
        val manager = ClipyJsDialogManager()
        val request = alertRequest()

        manager.show(request)
        runCatching {
            manager.complete(request) { error("Failed action") }
        }

        assertNull(manager.activeRequest)
    }

    @Test
    fun staleRequest_cannotCompleteNewRequest() {
        val manager = ClipyJsDialogManager()
        val staleRequest = alertRequest(title = "First")
        manager.show(staleRequest)
        manager.complete(staleRequest) {}

        val activeRequest = alertRequest(title = "Second")
        manager.show(activeRequest)
        var staleActionCount = 0
        manager.complete(staleRequest) { staleActionCount++ }

        assertEquals(0, staleActionCount)
        assertSame(activeRequest, manager.activeRequest)
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
