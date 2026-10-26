package com.laxotters.clipy.feature.session.browser.webview

import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
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
import androidx.test.filters.SdkSuppress
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogCoordinator
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.browser.dialog.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.browser.webview.client.SessionWebChromeClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.sameInstance
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
                <button id="confirm" onclick="var result=prompt('Prompt message', 'Clipy'); document.title=result === null ? 'cancelled' : result">
                    Confirm case
                </button>
                <button id="cancel" onclick="var result=prompt('Cancel prompt', 'Initial'); document.title=result === null ? 'cancelled' : result">
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
    fun webViewRelease_cancelsDialogAndAcceptsReplacementRequest() {
        dispatcher.respond("/alert", pendingAlertResponse())
        val testState = launchWebView("/alert")
        val original = currentWebView()
        startJavascript("alert")
        assertDialogText("Pending message")

        composeRule.runOnIdle { testState.webViewIds.value = emptyList() }
        waitForDialogToClose("Pending message")

        composeRule.runOnIdle {
            testState.pageUrl.set("")
            testState.webViewIds.value = listOf(1)
        }
        waitForPage(testState, "/alert")
        assertNotSame(original, currentWebView())
        startJavascript("alert")
        assertDialogText("Pending message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Pending message")
        waitForDocumentTitle("completed")
    }

    @Test
    fun previousWebViewRelease_keepsReplacementDialogAndResult() {
        dispatcher.respond("/alert", pendingAlertResponse())
        val testState = launchWebView("/alert")
        val original = currentWebView()
        startJavascript("alert")
        assertDialogText("Pending message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Pending message")
        waitForDocumentTitle("completed")

        // 새 요청이 표시된 뒤 이전 WebView를 해제하는 순서를 실제 onRelease로 검증합니다.
        composeRule.runOnIdle {
            testState.pageUrl.set("")
            testState.webViewIds.value = listOf(0, 1)
        }
        waitForPage(testState, "/alert")
        onView(allOf(isAssignableFrom(WebView::class.java), not(sameInstance(original)))).perform(
            webViewAction("start the replacement WebView request") { webView ->
                webView.evaluateJavascript("document.getElementById('alert').click()", null)
            },
        )
        assertDialogText("Pending message")

        composeRule.runOnIdle { testState.webViewIds.value = listOf(1) }

        assertDialogText("Pending message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Pending message")
        waitForDocumentTitle("completed")
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun closedCoordinator_rejectsRequestsAndDoesNotCancelReplacement() {
        dispatcher.respond("/alert", pendingAlertResponse())
        val testState = launchWebView("/alert")
        val coordinator = currentCoordinator()
        startJavascript("alert")
        assertDialogText("Pending message")

        composeRule.runOnIdle { testState.webViewIds.value = emptyList() }

        waitForDialogToClose("Pending message")
        var accepted = false
        var cancelCount = 0
        composeRule.runOnIdle {
            accepted = coordinator.showAlert(
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
            testState.webViewIds.value = listOf(1)
        }
        waitForPage(testState, "/alert")
        val replacement = currentCoordinator()
        assertNotSame(coordinator, replacement)
        startJavascript("alert")
        assertDialogText("Pending message")
        composeRule.runOnIdle { coordinator.close() }
        assertDialogText("Pending message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Pending message")
    }

    @Test
    fun backgroundAndResume_keepsPendingDialogAndOwner() {
        dispatcher.respond(
            "/background",
            htmlResponse(
                """
                <button id="alert" onclick="alert('Background message'); document.title='completed'">Alert</button>
                """.trimIndent(),
            ),
        )
        launchWebView("/background")
        val webView = currentWebView()
        startJavascript("alert")
        assertDialogText("Background message")

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        assertDialogText("Background message")
        composeRule.onNodeWithText("Confirm").performClick()
        waitForDialogToClose("Background message")
        waitForDocumentTitle("completed")
        assertSame(webView, currentWebView())
    }

    private fun launchWebView(path: String): JsDialogTestState {
        val testState = JsDialogTestState()
        val initialUrl = server.url(path).toString()

        composeRule.setContent {
            val dialogState = rememberSessionWebDialogState()
            testState.webViewIds.value.forEach { id ->
                key(id) {
                    val controller = rememberSessionWebViewController()
                    SessionWebView(
                        url = initialUrl,
                        controller = controller,
                        dialogState = dialogState,
                        callbacks = SessionWebViewCallbacks(
                            onPageFinished = { pageInfo -> testState.pageUrl.set(pageInfo.url) },
                            onRootScrolled = {},
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            SessionWebDialogHosts(state = dialogState)
        }

        waitForPage(testState, path)
        return testState
    }

    private fun currentWebView(): WebView {
        val result = AtomicReference<WebView>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("capture the current WebView") { result.set(it) },
        )
        return checkNotNull(result.get())
    }

    // WebChromeClient 조회 API는 26 이상에서 제공하므로 소유권 비교 테스트에만 사용합니다.
    @RequiresApi(26)
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

    private fun pendingAlertResponse(): MockResponse = htmlResponse(
        """
        <button id="alert" onclick="alert('Pending message'); document.title='completed'">Show alert</button>
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
        val webViewIds = mutableStateOf(listOf(0))
        val pageUrl = AtomicReference("")
    }

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
