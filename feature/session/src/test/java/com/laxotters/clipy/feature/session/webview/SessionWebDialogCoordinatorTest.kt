package com.laxotters.clipy.feature.session.webview

import com.laxotters.clipy.core.designsystem.component.dialog.JsDialogRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionWebDialogCoordinatorTest {
    @Test
    fun alert_buildsExpectedRequestAndConfirmsExactlyOnce() {
        val fixture = CoordinatorFixture()
        var confirmCount = 0
        var cancelCount = 0

        assertTrue(
            fixture.coordinator.showAlert(
                url = "https://www.example.com/path",
                message = "Alert message",
                onConfirm = { confirmCount++ },
                onCancel = { cancelCount++ },
            ),
        )
        val request = fixture.request<JsDialogRequest.Alert>()

        assertEquals("Request from example.com", request.source)
        assertEquals("Message from this site", request.title)
        assertEquals("Alert message", request.description)
        assertEquals("Confirm", request.confirmAction.label)
        request.confirmAction.onClick()
        request.confirmAction.onClick()
        request.onCancel()

        assertEquals(1, confirmCount)
        assertEquals(0, cancelCount)
    }

    @Test
    fun rejectedAlert_isCancelledImmediately() {
        val fixture = CoordinatorFixture(acceptRequest = false)
        var confirmCount = 0
        var cancelCount = 0

        assertFalse(
            fixture.coordinator.showAlert(
                url = "about:blank",
                message = "Blocked",
                onConfirm = { confirmCount++ },
                onCancel = { cancelCount++ },
            ),
        )
        fixture.request<JsDialogRequest.Alert>().onCancel()

        assertEquals(0, confirmCount)
        assertEquals(1, cancelCount)
    }

    @Test
    fun confirm_mapsPrimaryAndCancelActions() {
        val confirmFixture = CoordinatorFixture()
        var confirmCount = 0
        var cancelCount = 0
        confirmFixture.coordinator.showConfirm(
            url = "custom:source",
            message = "Continue the action?",
            onConfirm = { confirmCount++ },
            onCancel = { cancelCount++ },
        )
        val confirmRequest = confirmFixture.request<JsDialogRequest.Confirm>()

        assertEquals("Request from custom:source", confirmRequest.source)
        assertEquals("Continue?", confirmRequest.title)
        assertEquals("Confirm", confirmRequest.confirmAction.label)
        assertEquals("Cancel", confirmRequest.cancelAction.label)
        confirmRequest.confirmAction.onClick()

        val cancelFixture = CoordinatorFixture()
        cancelFixture.coordinator.showConfirm(
            url = "https://example.com",
            message = "Continue the action?",
            onConfirm = { confirmCount++ },
            onCancel = { cancelCount++ },
        )
        cancelFixture.request<JsDialogRequest.Confirm>().cancelAction.onClick()

        assertEquals(1, confirmCount)
        assertEquals(1, cancelCount)
    }

    @Test
    fun prompt_usesEmptyDefaultAndConfirmsLatestValue() {
        val fixture = CoordinatorFixture()
        var confirmedValue: String? = null
        var cancelCount = 0

        fixture.coordinator.showPrompt(
            url = "https://example.com",
            message = "Enter a value",
            defaultValue = null,
            onConfirm = { confirmedValue = it },
            onCancel = { cancelCount++ },
        )
        val request = fixture.request<JsDialogRequest.Prompt>()

        assertEquals("Enter information", request.title)
        assertEquals("", request.initialValue)
        assertEquals("Confirm", request.confirmAction.label)
        assertEquals("Cancel", request.cancelAction.label)
        request.confirmAction.onClick("Updated")
        request.cancelAction.onClick()

        assertEquals("Updated", confirmedValue)
        assertEquals(0, cancelCount)
    }

    @Test
    fun beforeUnload_mapsLeaveToConfirmAndStayToCancel() {
        var leaveCount = 0
        var stayCount = 0
        val leaveFixture = CoordinatorFixture()
        leaveFixture.coordinator.showBeforeUnload(
            url = "https://example.com",
            message = "Changes may not be saved.",
            onLeave = { leaveCount++ },
            onStay = { stayCount++ },
        )
        val leaveRequest = leaveFixture.request<JsDialogRequest.Confirm>()

        assertEquals("Leave this page?", leaveRequest.title)
        assertEquals("Leave", leaveRequest.confirmAction.label)
        assertEquals("Stay", leaveRequest.cancelAction.label)
        leaveRequest.confirmAction.onClick()

        val stayFixture = CoordinatorFixture()
        stayFixture.coordinator.showBeforeUnload(
            url = "https://example.com",
            message = "Changes may not be saved.",
            onLeave = { leaveCount++ },
            onStay = { stayCount++ },
        )
        stayFixture.request<JsDialogRequest.Confirm>().cancelAction.onClick()

        assertEquals(1, leaveCount)
        assertEquals(1, stayCount)
    }

    @Test
    fun close_cancelsOwnedRequestOnceAndRejectsLaterRequests() {
        val fixture = CoordinatorFixture()
        var cancellations = 0
        fixture.coordinator.showAlert("https://example.com", "first", {}, { cancellations++ })

        fixture.coordinator.close()
        fixture.coordinator.close()

        assertEquals(1, fixture.cancelActiveCount)
        assertEquals(1, cancellations)
        assertFalse(
            fixture.coordinator.showAlert("https://example.com", "late", {}, { cancellations++ }),
        )
        assertEquals(2, cancellations)
    }

    @Test
    fun close_rejectsRequestFromCancellationCallback() {
        val fixture = CoordinatorFixture()
        var nestedCancellations = 0
        fixture.coordinator.showAlert(
            "https://example.com",
            "first",
            {},
            {
                fixture.coordinator.close()
                assertFalse(
                    fixture.coordinator.showAlert(
                        "https://example.com",
                        "nested",
                        {},
                        { nestedCancellations++ },
                    ),
                )
            },
        )

        fixture.coordinator.close()

        assertEquals(1, nestedCancellations)
        assertEquals(1, fixture.cancelActiveCount)
    }

    @Test
    fun closeAfterCompletion_doesNotCancelAnotherOwnersRequest() {
        var active: JsDialogRequest? = null
        val show: (JsDialogRequest) -> Boolean = { request ->
            if (active == null) {
                active = request
                true
            } else {
                false
            }
        }
        val cancel: (JsDialogRequest) -> Unit = { request ->
            if (active === request) {
                request.onCancel()
                active = null
            }
        }
        val oldOwner = SessionWebDialogCoordinator(show, cancel, TEST_STRINGS)
        val newOwner = SessionWebDialogCoordinator(show, cancel, TEST_STRINGS)
        oldOwner.showAlert("https://example.com", "old", {}, {})
        val oldRequest = active as JsDialogRequest.Alert
        active = null
        oldRequest.confirmAction.onClick()
        var newCancellations = 0
        newOwner.showAlert("https://example.com", "new", {}, { newCancellations++ })

        oldOwner.close()

        assertEquals("new", active?.description)
        assertEquals(0, newCancellations)
        newOwner.close()
        assertEquals(1, newCancellations)
    }

    @Test
    fun cancellationFailure_leavesOwnerClosed() {
        val fixture = CoordinatorFixture()
        fixture.coordinator.showAlert("https://example.com", "first", {}, { error("cancel failed") })

        assertTrue(runCatching { fixture.coordinator.close() }.isFailure)
        fixture.coordinator.close()
        var cancellations = 0
        assertFalse(
            fixture.coordinator.showAlert("https://example.com", "late", {}, { cancellations++ }),
        )

        assertEquals(1, cancellations)
        assertEquals(1, fixture.cancelActiveCount)
    }

    private class CoordinatorFixture(
        private val acceptRequest: Boolean = true,
    ) {
        private var activeRequest: JsDialogRequest? = null
        var cancelActiveCount = 0
            private set

        val coordinator = SessionWebDialogCoordinator(
            showDialog = { request ->
                activeRequest = request
                acceptRequest
            },
            cancelActiveDialog = { request ->
                cancelActiveCount++
                request.onCancel()
            },
            strings = TEST_STRINGS,
        )

        inline fun <reified T : JsDialogRequest> request(): T =
            requireNotNull(activeRequest) as T
    }

    private companion object {
        val TEST_STRINGS = SessionWebDialogStrings(
            sourcePrefix = "Request from ",
            alertTitle = "Message from this site",
            confirmTitle = "Continue?",
            promptTitle = "Enter information",
            beforeUnloadTitle = "Leave this page?",
            confirmAction = "Confirm",
            cancelAction = "Cancel",
            stayAction = "Stay",
            leaveAction = "Leave",
        )
    }
}
