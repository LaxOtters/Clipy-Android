package com.laxotters.clipy.feature.session.webview

import android.content.Intent
import com.laxotters.clipy.feature.session.webview.navigation.ExternalAppHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalActionCoordinatorTest {
    @Test
    fun withAppName_requestExternalApp_buildsNamedConfirmation() {
        val fixture = Fixture(appName = "Maps")

        fixture.coordinator.requestExternalApp("geo:37.5,127.0")

        val request = fixture.request()
        assertEquals("Open in Maps?", request.title)
        assertEquals("Clipy will open another app to continue.", request.description)
        assertEquals("Continue", request.continueLabel)
        assertEquals("Cancel", request.cancelLabel)
    }

    @Test
    fun withoutAppName_requestExternalApp_buildsGenericConfirmation() {
        val fixture = Fixture(appName = null)

        fixture.coordinator.requestExternalApp("clipy-auth://callback")

        assertEquals("Open external app?", fixture.request().title)
    }

    @Test
    fun cancelConfirmation_doesNotLaunchOrReportError() {
        val fixture = Fixture(appName = null)
        fixture.coordinator.requestExternalApp("clipy-auth://callback")
        val entry = requireNotNull(fixture.manager.activeEntry)

        fixture.manager.complete(entry, action = {})

        assertTrue(fixture.launchedIntents.isEmpty())
        assertEquals(null, fixture.manager.activeEntry)
        assertTrue(fixture.messages.isEmpty())
    }

    @Test
    fun continueConfirmation_launchesSanitizedIntent() {
        val fixture = Fixture(appName = "Auth")
        fixture.coordinator.requestExternalApp(
            "intent://callback/#Intent;scheme=clipy-auth;package=com.example.auth;S.token=kept;end",
        )

        fixture.confirm()

        val intent = fixture.launchedIntents.single()
        assertEquals("com.example.auth", intent.`package`)
        assertEquals("kept", intent.getStringExtra("token"))
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun failedLaunch_continue_reportsFailureMessage() {
        val fixture = Fixture(appName = null, launchResult = false)
        fixture.coordinator.requestExternalApp("clipy-auth://callback")

        fixture.confirm()

        assertEquals(listOf("Couldn't open external app"), fixture.messages.map { it.message })
    }

    @Test
    fun eligibleDownload_request_buildsBrowserConfirmation() {
        val fixture = Fixture(appName = null)

        fixture.coordinator.requestOpenInBrowser("http://localhost/file.zip")

        val request = fixture.request()
        assertEquals("Open in browser?", request.title)
        assertEquals("Downloads aren't supported in Clipy.", request.description)
        assertEquals("Continue", request.continueLabel)
    }

    @Test
    fun downloadWithoutHttpHost_request_reportsUnsupportedMessage() {
        val fixture = Fixture(appName = null)

        fixture.coordinator.requestOpenInBrowser("content://com.example/file/1")

        assertEquals(listOf("Downloads aren't supported"), fixture.messages.map { it.message })
        assertEquals(null, fixture.manager.activeEntry)
    }

    @Test
    fun close_cancelsOwnedDialogAndIgnoresLaterRequests() {
        val fixture = Fixture(appName = null)
        fixture.coordinator.requestExternalApp("clipy-auth://callback")

        fixture.coordinator.close()
        fixture.coordinator.requestExternalApp("mailto:hello@example.com")

        assertEquals(null, fixture.manager.activeEntry)
        assertTrue(fixture.launchedIntents.isEmpty())
        assertTrue(fixture.messages.isEmpty())
    }

    private class Fixture(
        appName: String?,
        private val launchResult: Boolean = true,
    ) {
        private val gate = SessionWebDialogGate()
        val manager = SessionExternalActionDialogManager(gate)
        val launchedIntents = mutableListOf<Intent>()
        val messages = mutableListOf<ErrorMessage>()
        val coordinator = ExternalActionCoordinator(
            dialogManager = manager,
            appHandler = ExternalAppHandler(
                resolveAppName = { appName },
                launch = { intent ->
                    launchedIntents += intent
                    launchResult
                },
            ),
            onErrorMessage = messages::add,
            strings = TEST_STRINGS,
        )

        fun request(): SessionExternalActionDialogRequest =
            requireNotNull(manager.activeEntry).request

        fun confirm() {
            val entry = requireNotNull(manager.activeEntry)
            manager.complete(entry, entry.request.onContinue)
        }
    }

    private companion object {
        val TEST_STRINGS = ExternalActionStrings(
            externalAppTitle = "Open external app?",
            namedAppTitle = "Open in %1\$s?",
            externalAppDescription = "Clipy will open another app to continue.",
            downloadTitle = "Open in browser?",
            downloadDescription = "Downloads aren't supported in Clipy.",
            cancelAction = "Cancel",
            continueAction = "Continue",
            externalAppFailureMessage = "Couldn't open external app",
            unsupportedDownloadMessage = "Downloads aren't supported",
        )
    }
}
