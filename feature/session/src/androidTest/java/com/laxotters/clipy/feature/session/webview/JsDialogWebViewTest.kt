package com.laxotters.clipy.feature.session.webview

import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogHost
import com.laxotters.clipy.core.designsystem.component.dialog.rememberClipyJsDialogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class JsDialogWebViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var server: MockWebServer
    private lateinit var dispatcher: RecordingDispatcher

    @Before
    fun setUp() {
        server = MockWebServer()
        dispatcher = RecordingDispatcher()
        server.dispatcher = dispatcher
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun javascriptAlert_displaysDialogAndConfirmsResult() {
        dispatcher.respond(
            "/alert",
            htmlResponse(
                """
                <button id="alert" onclick="alert('Alert message'); document.title='confirmed'">
                    Show alert
                </button>
                """.trimIndent(),
            ),
        )
        launchWebView("/alert")

        startJavascript("alert")

        assertDialogText(
            "Request from localhost",
            "Message from this site",
            "Alert message",
            "Confirm",
        )
        composeRule.onNodeWithText("Confirm").performClick()

        waitForDialogToClose("Message from this site")
        waitForDocumentTitle("confirmed")
    }

    @Test
    fun javascriptAlert_confirmImmediatelyShowsNextDialog() {
        dispatcher.respond(
            "/sequential-alerts",
            htmlResponse(
                """
                <button id="alerts" onclick="alert('First message'); alert('Second message'); document.title='completed'">
                    Show alerts
                </button>
                """.trimIndent(),
            ),
        )
        launchWebView("/sequential-alerts")

        startJavascript("alerts")
        assertDialogText("First message")

        composeRule.onNodeWithText("Confirm").performClick()

        assertDialogText("Second message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Second message")
        waitForDocumentTitle("completed")
    }

    @Test
    fun javascriptConfirm_mapsCancelAndConfirmResults() {
        dispatcher.respond(
            "/confirm",
            htmlResponse(
                """
                <button id="cancel" onclick="document.title=confirm('Cancel message') ? 'unexpected' : 'cancelled'">
                    Cancel case
                </button>
                <button id="confirm" onclick="document.title=confirm('Confirm message') ? 'confirmed' : 'unexpected'">
                    Confirm case
                </button>
                """.trimIndent(),
            ),
        )
        launchWebView("/confirm")

        startJavascript("cancel")
        assertDialogText("Continue?", "Cancel message")
        composeRule.onNodeWithText("Cancel").performClick()
        waitForDialogToClose("Cancel message")
        waitForDocumentTitle("cancelled")

        startJavascript("confirm")
        assertDialogText("Confirm message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Confirm message")
        waitForDocumentTitle("confirmed")
    }

    @Test
    fun javascriptPrompt_confirmsLatestInputAndCancels() {
        dispatcher.respond(
            "/prompt",
            htmlResponse(
                """
                <button id="confirm" onclick="document.title=prompt('Prompt message', 'Clipy') ?? 'cancelled'">
                    Confirm case
                </button>
                <button id="cancel" onclick="document.title=prompt('Cancel prompt', 'Initial') ?? 'cancelled'">
                    Cancel case
                </button>
                """.trimIndent(),
            ),
        )
        launchWebView("/prompt")

        startJavascript("confirm")
        assertDialogText("Enter information", "Prompt message", "Clipy")
        composeRule.onNodeWithText("Clipy").performTextReplacement("Updated")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Prompt message")
        waitForDocumentTitle("Updated")

        startJavascript("cancel")
        assertDialogText("Cancel prompt", "Initial")
        composeRule.onNodeWithText("Cancel").performClick()
        waitForDialogToClose("Cancel prompt")
        waitForDocumentTitle("cancelled")
    }

    @Test
    fun javascriptBeforeUnload_stayCancelsNavigation() {
        dispatcher.respond("/before-unload", beforeUnloadResponse())
        dispatcher.respond("/target", htmlResponse("Target"))
        val testState = launchWebView("/before-unload")

        clickWebView()
        assertDialogText("Leave this page?", "Stay", "Leave")
        composeRule.onNodeWithText("Stay").performClick()

        waitForDialogToClose("Leave this page?")
        assertTrue(testState.pageUrl.get().endsWith("/before-unload"))
    }

    @Test
    fun javascriptBeforeUnload_leaveConfirmsNavigation() {
        dispatcher.respond("/before-unload", beforeUnloadResponse())
        dispatcher.respond("/target", htmlResponse("Target"))
        val testState = launchWebView("/before-unload")

        clickWebView()
        assertDialogText("Leave this page?", "Stay", "Leave")
        composeRule.onNodeWithText("Leave").performClick()

        waitForPage(testState, "/target")
    }

    @Test
    fun webViewRelease_cancelsActiveDialogAndClearsManagerSlot() {
        dispatcher.respond(
            "/alert",
            htmlResponse(
                """
                <button id="alert" onclick="alert('Pending message')">Show alert</button>
                """.trimIndent(),
            ),
        )
        val testState = launchWebView("/alert")
        startJavascript("alert")
        assertDialogText("Pending message")

        composeRule.runOnIdle { testState.isWebViewVisible.value = false }

        waitForDialogToClose("Pending message")
        var accepted = false
        var cancelCount = 0
        composeRule.runOnIdle {
            accepted = testState.dialogCoordinator.get().showAlert(
                url = server.url("/next").toString(),
                message = "Next message",
                onConfirm = {},
                onCancel = { cancelCount++ },
            )
        }
        assertFalse(accepted)
        assertEquals(1, cancelCount)

        composeRule.runOnIdle {
            testState.pageUrl.set("")
            testState.isWebViewVisible.value = true
        }
        waitForPage(testState, "/alert")
        val replacement = currentCoordinator()
        assertNotSame(testState.dialogCoordinator.get(), replacement)
        startJavascript("alert")
        assertDialogText("Pending message")
        composeRule.runOnIdle { testState.dialogCoordinator.get().close() }
        assertDialogText("Pending message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Pending message")
    }

    @Test
    fun backgroundAndResume_keepsPendingDialogAndOwner() {
        dispatcher.respond(
            "/background",
            htmlResponse(
                """<button id="alert" onclick="alert('Background message'); document.title='completed'">Alert</button>""",
            ),
        )
        val state = launchWebView("/background")
        startJavascript("alert")
        assertDialogText("Background message")

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        assertDialogText("Background message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Background message")
        waitForDocumentTitle("completed")
        assertSame(state.dialogCoordinator.get(), currentCoordinator())
    }

    private fun launchWebView(path: String): JsDialogTestState {
        val testState = JsDialogTestState()
        val initialUrl = server.url(path).toString()

        composeRule.setContent {
            val controller = rememberSessionWebViewController()
            val dialogManager = rememberClipyJsDialogManager()
            if (testState.isWebViewVisible.value) {
                SessionWebView(
                    url = initialUrl,
                    controller = controller,
                    dialogManager = dialogManager,
                    onPageStateChanged = { url, _, _ -> testState.pageUrl.set(url) },
                    onRootScrolled = { _, _, _, _ -> },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ClipyJsDialogHost(manager = dialogManager)
        }

        waitForPage(testState, path)
        testState.dialogCoordinator.set(currentCoordinator())
        return testState
    }

    private fun currentCoordinator(): SessionWebDialogCoordinator {
        val coordinator = AtomicReference<SessionWebDialogCoordinator>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("capture WebView dialog owner") { webView ->
                coordinator.set((webView.webChromeClient as SessionWebChromeClient).dialogCoordinator)
            },
        )
        return checkNotNull(coordinator.get())
    }

    private fun startJavascript(elementId: String) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("start JavaScript from a page element") { webView ->
                webView.evaluateJavascript(
                    "document.getElementById('$elementId').click()",
                    null,
                )
            },
        )
    }

    private fun clickWebView() {
        onView(isAssignableFrom(WebView::class.java)).perform(click())
    }

    private fun assertDialogText(vararg values: String) {
        values.forEach { value ->
            composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
                composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText(value).assertIsDisplayed()
        }
    }

    private fun waitForDialogToClose(title: String) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun waitForDocumentTitle(expected: String) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            currentWebViewTitle() == expected
        }
    }

    private fun currentWebViewTitle(): String? {
        val title = AtomicReference<String?>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("read the document title") { webView ->
                title.set(webView.title)
            },
        )
        return title.get()
    }

    private fun waitForPage(
        testState: JsDialogTestState,
        path: String,
    ) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.pageUrl.get().endsWith(path)
        }
    }

    private fun beforeUnloadResponse(): MockResponse = htmlResponse(
        """
        <script>window.onbeforeunload = function() { return 'Unsaved changes'; };</script>
        <a id="navigate" class="tap-target" href="/target">Navigate</a>
        """.trimIndent(),
    )

    private fun htmlResponse(body: String): MockResponse =
        MockResponse.Builder()
            .addHeader("Content-Type", "text/html; charset=utf-8")
            .addHeader("Cache-Control", "no-store")
            .body(
                """
                <!doctype html>
                <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <link rel="icon" href="data:,">
                        <style>
                            html, body { width: 100%; height: 100%; margin: 0; }
                            .tap-target { display: block; width: 100%; height: 100%; }
                        </style>
                    </head>
                    <body>$body</body>
                </html>
                """.trimIndent(),
            )
            .build()

    private fun webViewAction(
        description: String,
        block: (WebView) -> Unit,
    ): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

        override fun getDescription(): String = description

        override fun perform(
            uiController: UiController,
            view: View,
        ) {
            block(view as WebView)
        }
    }

    private class RecordingDispatcher : Dispatcher() {
        private val responses = ConcurrentHashMap<String, MockResponse>()

        fun respond(path: String, response: MockResponse) {
            responses[path] = response
        }

        override fun dispatch(request: RecordedRequest): MockResponse =
            responses[request.url.encodedPath]
                ?: MockResponse.Builder().code(404).build()
    }

    private class JsDialogTestState {
        val isWebViewVisible = mutableStateOf(true)
        val pageUrl = AtomicReference("")
        val dialogCoordinator = AtomicReference<SessionWebDialogCoordinator>()
    }

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
