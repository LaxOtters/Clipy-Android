package com.laxotters.clipy.feature.session.webview

import com.laxotters.clipy.core.designsystem.component.dialog.JsDialogRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionWebDialogGateTest {
    @Test
    fun externalDialogVisible_jsDialogIsRejectedWithoutQueue() {
        val gate = SessionWebDialogGate()
        val externalManager = SessionExternalActionDialogManager(gate)
        assertTrue(externalManager.show(Any(), externalRequest()))
        var jsCancellationCount = 0
        val coordinator = jsCoordinator(gate)

        assertFalse(
            coordinator.showAlert(
                url = "https://example.com",
                message = "Later JS request",
                onConfirm = {},
                onCancel = { jsCancellationCount++ },
            ),
        )

        assertEquals(1, jsCancellationCount)
        assertEquals("External", externalManager.activeEntry?.request?.title)
    }

    @Test
    fun jsDialogVisible_externalDialogIsRejectedWithoutQueue() {
        val gate = SessionWebDialogGate()
        val externalManager = SessionExternalActionDialogManager(gate)
        var jsRequest: JsDialogRequest? = null
        val coordinator = jsCoordinator(
            gate = gate,
            showDialog = { request ->
                jsRequest = request
                true
            },
        )
        assertTrue(coordinator.showAlert("https://example.com", "JS", {}, {}))

        assertFalse(externalManager.show(Any(), externalRequest()))
        assertEquals("JS", jsRequest?.description)
        assertEquals(null, externalManager.activeEntry)
    }

    @Test
    fun completedJsDialog_releasesGateForNextExternalDialog() {
        val gate = SessionWebDialogGate()
        val externalManager = SessionExternalActionDialogManager(gate)
        lateinit var jsRequest: JsDialogRequest.Alert
        val coordinator = jsCoordinator(
            gate = gate,
            showDialog = { request ->
                jsRequest = request as JsDialogRequest.Alert
                true
            },
        )
        coordinator.showAlert("https://example.com", "JS", {}, {})

        jsRequest.confirmAction.onClick()

        assertTrue(externalManager.show(Any(), externalRequest()))
    }

    private fun jsCoordinator(
        gate: SessionWebDialogGate,
        showDialog: (JsDialogRequest) -> Boolean = { true },
    ): SessionWebDialogCoordinator = SessionWebDialogCoordinator(
        showDialog = showDialog,
        cancelActiveDialog = { request -> request.onCancel() },
        strings = TEST_JS_STRINGS,
        displayGate = gate,
    )

    private fun externalRequest() = SessionExternalActionDialogRequest(
        title = "External",
        description = "Description",
        continueLabel = "Continue",
        cancelLabel = "Cancel",
        onContinue = {},
    )

    private companion object {
        val TEST_JS_STRINGS = SessionWebDialogStrings(
            sourcePrefix = "Request from ",
            alertTitle = "Message",
            confirmTitle = "Continue?",
            promptTitle = "Enter",
            beforeUnloadTitle = "Leave?",
            confirmAction = "Confirm",
            cancelAction = "Cancel",
            stayAction = "Stay",
            leaveAction = "Leave",
        )
    }
}
