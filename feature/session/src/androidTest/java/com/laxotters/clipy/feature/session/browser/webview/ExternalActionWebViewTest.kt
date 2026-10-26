package com.laxotters.clipy.feature.session.browser.webview

import android.content.Intent
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.browser.dialog.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
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
class ExternalActionWebViewTest {
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
    fun mainFrameIntentUri_showsNamedDialogAndLaunchesSanitizedIntent() {
        dispatcher.respond(
            "/external",
            htmlResponse(
                """
                <a id="open" class="tap-target"
                   href="intent://callback/#Intent;scheme=clipy-auth;package=com.example.auth;S.token=kept;S.browser_fallback_url=https%3A%2F%2Fexample.com;end">
                    Open
                </a>
                """.trimIndent(),
            ),
        )
        val testState = launchWebView("/external", appName = "Auth App")

        clickWebElement("open")

        assertDialogText("Open in Auth App?", "Clipy will open another app to continue.")
        composeRule.onNodeWithText("Continue").performClick()
        val launchedIntent = requireNotNull(testState.launchedIntent.get())
        assertEquals("com.example.auth", launchedIntent.`package`)
        assertEquals("kept", launchedIntent.getStringExtra("token"))
        assertFalse(launchedIntent.hasExtra("browser_fallback_url"))
        assertTrue(launchedIntent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(testState.pageUrl.get().endsWith("/external"))
    }

    @Test
    fun subframeAppUri_doesNotRequestExternalHandoff() {
        dispatcher.respond(
            "/subframe",
            htmlResponse(
                """
                <iframe src="clipy-auth://callback"></iframe>
                <script>setTimeout(function() { document.title = 'settled'; }, 300);</script>
                """.trimIndent(),
            ),
        )
        val testState = launchWebView("/subframe", appName = "Auth App")

        waitForDocumentTitle("settled", currentWebView())

        composeRule.onAllNodesWithText("Open in Auth App?").assertCountEquals(0)
        assertEquals(null, testState.launchedIntent.get())
        assertTrue(testState.messages.isEmpty())
    }

    @Test
    fun externalDialogCancel_keepsCurrentWebViewAndPage() {
        dispatcher.respond(
            "/external",
            htmlResponse("""<a id="open" class="tap-target" href="clipy-auth://callback">Open</a>"""),
        )
        val testState = launchWebView("/external", appName = null)
        val webView = currentWebView()

        clickWebElement("open")
        assertDialogText("Open external app?")
        composeRule.onNodeWithText("Cancel").performClick()

        waitForDialogToClose("Open external app?")
        assertSame(webView, currentWebView())
        assertTrue(testState.pageUrl.get().endsWith("/external"))
        assertEquals(null, testState.launchedIntent.get())
    }

    @Test
    fun failedLaunchOnContinue_reportsFailureAndKeepsCurrentWebView() {
        dispatcher.respond(
            "/external",
            htmlResponse("""<a id="open" class="tap-target" href="clipy-auth://callback">Open</a>"""),
        )
        val testState = launchWebView(
            path = "/external",
            appName = null,
            launchResult = false,
        )
        val webView = currentWebView()

        clickWebElement("open")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.messages.any { it.message == "Couldn't open external app" }
        }
        assertSame(webView, currentWebView())
        assertTrue(testState.pageUrl.get().endsWith("/external"))
    }

    @Test
    fun externalDialogVisible_laterJsDialogIsCancelledWithoutQueue() {
        dispatcher.respond(
            "/external",
            htmlResponse("""<a id="open" class="tap-target" href="clipy-auth://callback">Open</a>"""),
        )
        launchWebView("/external", appName = null)
        val webView = currentWebView()
        clickWebElement("open")
        assertDialogText("Open external app?")

        evaluateJavascript(webView, "alert('Later JS request'); document.title='js-completed'")

        assertDialogText("Open external app?")
        composeRule.onAllNodesWithText("Later JS request").assertCountEquals(0)
        waitForDocumentTitle("js-completed", webView)
    }

    @Test
    fun webViewRelease_cancelsOwnedExternalDialogAndAllowsReplacement() {
        dispatcher.respond(
            "/external",
            htmlResponse("""<a id="open" class="tap-target" href="clipy-auth://callback">Open</a>"""),
        )
        val testState = launchWebView("/external", appName = null)
        val original = currentWebView()
        clickWebElement("open")
        assertDialogText("Open external app?")

        composeRule.runOnIdle { testState.webViewIds.value = emptyList() }
        waitForDialogToClose("Open external app?")
        composeRule.runOnIdle {
            testState.pageUrl.set("")
            testState.webViewIds.value = listOf(1)
        }
        waitForPage(testState, "/external")

        assertNotSame(original, currentWebView())
        clickWebElement("open")
        assertDialogText("Open external app?")
    }

    @Test
    fun redirectedMainFrameDownload_showsBrowserConfirmation() {
        dispatcher.respond(
            "/start",
            htmlResponse("""<a id="download" class="tap-target" href="/redirect-download">Download</a>"""),
        )
        dispatcher.respond(
            "/redirect-download",
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", server.url("/file.bin"))
                .build(),
        )
        dispatcher.respond("/file.bin", downloadResponse())
        launchWebView("/start", appName = null)

        clickWebElement("download")

        assertDialogText(
            "Open in browser?",
            "Downloads aren't supported in Clipy.",
            "Continue",
        )
    }

    @Test
    fun downloadWithoutMainFrameNavigationEvidence_reportsUnsupported() {
        dispatcher.respond("/start", htmlResponse("Start"))
        dispatcher.respond("/file.bin", downloadResponse())
        val testState = launchWebView("/start", appName = null)

        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("load download without user navigation") { webView ->
                webView.loadUrl(server.url("/file.bin").toString())
            },
        )

        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.messages.any { it.message == "Downloads aren't supported" }
        }
        composeRule.onAllNodesWithText("Open in browser?").assertCountEquals(0)
    }

    @Test
    fun iframeDownload_reportsUnsupportedWithoutBrowserConfirmation() {
        dispatcher.respond(
            "/iframe-download",
            htmlResponse("""<iframe src="/file.bin"></iframe>"""),
        )
        dispatcher.respond("/file.bin", downloadResponse())
        val testState = launchWebView("/iframe-download", appName = null)

        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.messages.any { it.message == "Downloads aren't supported" }
        }
        composeRule.onAllNodesWithText("Open in browser?").assertCountEquals(0)
    }

    @Test
    fun iframeThenMainFrameSameUrl_reportsBothDownloadsAsAmbiguous() {
        dispatcher.respond(
            "/duplicate-download",
            htmlResponse(
                """
                <button id="iframe" onclick="
                    var frame = document.createElement('iframe');
                    frame.src = '/file.bin';
                    document.body.appendChild(frame);
                ">Iframe download</button>
                <a id="main" class="tap-target" href="/file.bin">Main download</a>
                """.trimIndent(),
            ),
        )
        dispatcher.respond("/file.bin", downloadResponse())
        val testState = launchWebView("/duplicate-download", appName = null)

        clickWebElement("iframe")
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.messages.count { it.message == "Downloads aren't supported" } == 1
        }
        clickWebElement("main")

        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.messages.count { it.message == "Downloads aren't supported" } >= 2
        }
        composeRule.onAllNodesWithText("Open in browser?").assertCountEquals(0)
    }

    private fun launchWebView(
        path: String,
        appName: String?,
        launchResult: Boolean = true,
    ): ExternalWebViewTestState {
        val testState = ExternalWebViewTestState(
            appName = appName,
            launchResult = launchResult,
        )
        val initialUrl = server.url(path).toString()

        composeRule.setContent {
            val dialogState = rememberSessionWebDialogState()
            testState.webViewIds.value.forEach { id ->
                key(id) {
                    SessionWebView(
                        url = initialUrl,
                        controller = rememberSessionWebViewController(),
                        dialogState = dialogState,
                        callbacks = SessionWebViewCallbacks(
                            onPageFinished = { pageInfo -> testState.pageUrl.set(pageInfo.url) },
                            onRootScrolled = {},
                            onErrorMessage = testState.messages::add,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        externalAppHandlerFactory = {
                            ExternalAppHandler(
                                resolveAppName = { testState.appName.get() },
                                launch = { intent ->
                                    testState.launchedIntent.set(intent)
                                    testState.launchResult
                                },
                            )
                        },
                    )
                }
            }
            SessionWebDialogHosts(state = dialogState)
        }

        waitForPage(testState, path)
        return testState
    }

    private fun clickWebElement(id: String) {
        onWebView()
            .withElement(findElement(Locator.ID, id))
            .perform(webClick())
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

    private fun waitForPage(
        testState: ExternalWebViewTestState,
        path: String,
    ) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.pageUrl.get().endsWith(path)
        }
    }

    private fun waitForDocumentTitle(
        expected: String,
        webView: WebView,
    ) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            readWebViewTitle(webView) == expected
        }
    }

    private fun readWebViewTitle(webView: WebView): String? {
        val result = AtomicReference<String?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(webView.title)
        }
        return result.get()
    }

    private fun evaluateJavascript(
        webView: WebView,
        script: String,
    ) {
        composeRule.runOnIdle { webView.evaluateJavascript(script, null) }
    }

    private fun currentWebView(): WebView {
        val result = AtomicReference<WebView>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("capture current WebView") { result.set(it) },
        )
        return requireNotNull(result.get())
    }

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
            uiController.loopMainThreadUntilIdle()
        }
    }

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

    private fun downloadResponse(): MockResponse =
        MockResponse.Builder()
            .addHeader("Content-Type", "application/octet-stream")
            .addHeader("Content-Disposition", "attachment; filename=clipy.bin")
            .body("download")
            .build()

    private class RecordingDispatcher : Dispatcher() {
        private val responses = ConcurrentHashMap<String, MockResponse>()

        fun respond(path: String, response: MockResponse) {
            responses[path] = response
        }

        override fun dispatch(request: RecordedRequest): MockResponse =
            responses[request.url.encodedPath]
                ?: MockResponse.Builder().code(404).build()
    }

    private class ExternalWebViewTestState(
        appName: String?,
        val launchResult: Boolean,
    ) {
        val webViewIds = mutableStateOf(listOf(0))
        val appName = AtomicReference(appName)
        val pageUrl = AtomicReference("")
        val launchedIntent = AtomicReference<Intent?>()
        val messages = ConcurrentLinkedQueue<ErrorMessage>()
    }

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
