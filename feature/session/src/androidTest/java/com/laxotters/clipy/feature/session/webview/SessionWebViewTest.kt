package com.laxotters.clipy.feature.session.webview

import android.content.res.Configuration
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SessionWebViewTest {
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
    fun regularLink_onClick_navigatesAndReturnsThroughCurrentHistory() {
        dispatcher.respond(
            "/start",
            htmlResponse(
                """<a id="open" class="tap-target" href="/target">Open</a>""",
            ),
        )
        dispatcher.respond("/target", htmlResponse("Target"))
        val testState = launchWebView("/start")

        assertWebElementText(id = "open", text = "Open")
        clickWebView()

        waitForPage(testState, "/target")
        assertEquals("GET", dispatcher.singleRequest("/target").method)
        assertTrue(testState.page.get().canGoBack)

        goBack(testState)

        waitForPage(testState, "/start")
        assertFalse(testState.page.get().canGoBack)
        assertTrue(testState.page.get().canGoForward)
    }

    @Test
    fun blankTargetLink_onClick_opensInCurrentWebViewHistory() {
        dispatcher.respond(
            "/start",
            htmlResponse(
                """<a id="open" class="tap-target" href="/redirect?source=blank" target="_blank">Open</a>""",
            ),
        )
        dispatcher.respond(
            "/redirect",
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", "${server.url("/target?token=kept")}#result")
                .build(),
        )
        dispatcher.respond("/target", htmlResponse("Target"))
        val testState = launchWebView("/start")

        assertWebElementText(id = "open", text = "Open")
        clickWebView()

        waitForPage(testState, "/target?token=kept#result")
        assertEquals("GET", dispatcher.singleRequest("/start").method)
        val redirectRequest = dispatcher.singleRequest("/redirect")
        assertEquals("GET", redirectRequest.method)
        assertEquals("source=blank", redirectRequest.url.encodedQuery)
        val targetRequest = dispatcher.singleRequest("/target")
        assertEquals("GET", targetRequest.method)
        assertEquals("token=kept", targetRequest.url.encodedQuery)
        assertTrue(testState.page.get().canGoBack)

        goBack(testState)

        waitForPage(testState, "/start")
        assertFalse(testState.page.get().canGoBack)
        assertTrue(testState.page.get().canGoForward)
    }

    @Test
    fun blankTargetForm_onSubmit_preservesPostRequest() {
        dispatcher.respond(
            "/form",
            htmlResponse(
                """
                <form action="/submit" method="post" target="_blank">
                    <input name="payload" value="kept">
                    <button id="submit" class="tap-target" type="submit">Submit</button>
                </form>
                """.trimIndent(),
            ),
        )
        dispatcher.respond("/submit", htmlResponse("Submitted"))
        val testState = launchWebView("/form")

        assertWebElementText(id = "submit", text = "Submit")
        clickWebView()

        waitForPage(testState, "/submit")
        val submittedRequest = dispatcher.singleRequest("/submit")
        assertEquals("POST", submittedRequest.method)
        assertEquals("payload=kept", submittedRequest.body?.utf8())
        assertTrue(testState.page.get().canGoBack)

        goBack(testState)

        waitForPage(testState, "/form")
        assertEquals(1, dispatcher.requestCount("/submit"))
        assertFalse(testState.page.get().canGoBack)
        assertTrue(testState.page.get().canGoForward)
    }

    @Test
    fun javascriptWindowOpen_userGestureNavigatesAndAutomaticRequestIsBlocked() {
        dispatcher.respond(
            "/start",
            htmlResponse(
                """
                <script>window.open('/automatic')</script>
                <button id="open" class="tap-target" onclick="window.open('/target')">Open</button>
                """.trimIndent(),
            ),
        )
        dispatcher.respond("/automatic", htmlResponse("Automatic"))
        dispatcher.respond("/target", htmlResponse("Target"))
        val testState = launchWebView("/start")

        assertEquals(0, dispatcher.requestCount("/automatic"))
        assertWebElementText(id = "open", text = "Open")
        clickWebView()

        waitForPage(testState, "/target")
        assertEquals("GET", dispatcher.singleRequest("/target").method)
        assertTrue(testState.page.get().canGoBack)

        goBack(testState)

        waitForPage(testState, "/start")
        assertEquals(0, dispatcher.requestCount("/automatic"))
        assertFalse(testState.page.get().canGoBack)
        assertTrue(testState.page.get().canGoForward)
    }

    @Test
    fun javascriptWindowOpen_rapidRequests_usesWebViewNavigationOrder() {
        dispatcher.respond(
            "/start",
            htmlResponse(
                """
                <button id="open" class="tap-target" onclick="window.open('/first'); window.open('/second')">Open</button>
                """.trimIndent(),
            ),
        )
        dispatcher.respond("/first", htmlResponse("First"))
        dispatcher.respond("/second", htmlResponse("Second"))
        val testState = launchWebView("/start")

        assertWebElementText(id = "open", text = "Open")
        clickWebView()

        waitForPage(testState, "/first")
        assertEquals("GET", dispatcher.singleRequest("/first").method)
        assertEquals(0, dispatcher.requestCount("/second"))
    }

    @Test
    fun javascriptWindowOpen_withoutUrl_navigatesToBlockedAboutBlank() {
        dispatcher.respond(
            "/start",
            htmlResponse(
                """<button id="open" class="tap-target" onclick="window.open()">Open</button>""",
            ),
        )
        launchWebView("/start")

        assertWebElementText(id = "open", text = "Open")
        clickWebView()

        val webViewState = currentWebViewState()

        assertEquals("about:blank#blocked", webViewState.url)
        assertTrue(webViewState.canGoBack)
    }

    @Test
    fun nonUserRootMovement_fromJavascriptAnchorAndLayout_doesNotEmitRootScroll() {
        dispatcher.respond(
            "/scroll",
            htmlResponse(
                """
                <a id="anchor" href="#target">Jump</a>
                <div style="height: 2500px"></div>
                <div id="target">Target</div>
                <div style="height: 2500px"></div>
                """.trimIndent(),
            ),
        )
        val testState = launchWebView("/scroll")

        evaluateJavascript("window.scrollTo(0, 400)")
        waitForScrollY(minimumScrollY = 400)
        assertEquals(0, testState.rootScrollCount.get())

        onWebView()
            .withElement(findElement(Locator.ID, "anchor"))
            .perform(webClick())
        waitForScrollY(minimumScrollY = 2_000)
        assertEquals(0, testState.rootScrollCount.get())

        evaluateJavascript(
            "document.body.insertAdjacentHTML('afterbegin', '<div style=\"height:300px\"></div>')",
        )
        composeRule.waitForIdle()
        assertEquals(0, testState.rootScrollCount.get())
    }

    @Test
    fun touchDragAndFling_emitRootScrollUntilFlingSettles() {
        dispatcher.respond(
            "/scroll",
            htmlResponse("""<div style="height: 6000px">Scrollable</div>"""),
        )
        val testState = launchWebView("/scroll")

        val countAtRelease = performFastUpwardSwipe(testState)

        assertTrue(countAtRelease > 0)
        composeRule.waitUntil(timeoutMillis = FLING_TIMEOUT_MILLIS) {
            testState.rootScrollCount.get() > countAtRelease
        }
    }

    @Test
    fun configurationReset_ignoresCorrectionAndAllowsNextUserDrag() {
        dispatcher.respond(
            "/scroll",
            htmlResponse("""<div style="height: 6000px">Scrollable</div>"""),
        )
        val testState = launchWebView("/scroll")
        performFastUpwardSwipe(testState)
        dispatchWebViewConfigurationChanged()
        val countAfterConfiguration = testState.rootScrollCount.get()

        evaluateJavascript("window.scrollTo(0, 1200)")
        waitForScrollY(minimumScrollY = 1_200)
        assertEquals(countAfterConfiguration, testState.rootScrollCount.get())

        performFastUpwardSwipe(testState)
        assertTrue(testState.rootScrollCount.get() > countAfterConfiguration)
    }

    private fun launchWebView(path: String): WebViewTestState {
        val testState = WebViewTestState()
        val initialUrl = server.url(path).toString()

        composeRule.setContent {
            val controller = rememberSessionWebViewController()
            testState.controller.set(controller)
            SessionWebView(
                url = initialUrl,
                controller = controller,
                onPageStateChanged = { url, canGoBack, canGoForward ->
                    testState.page.set(
                        PageState(
                            url = url,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward,
                        ),
                    )
                },
                onRootScrolled = { _, _, _, _ ->
                    testState.rootScrollCount.incrementAndGet()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        waitForPage(testState, path)
        return testState
    }

    private fun assertWebElementText(
        id: String,
        text: String,
    ) {
        onWebView()
            .withElement(findElement(Locator.ID, id))
            .check(webMatches(getText(), containsString(text)))
    }

    private fun clickWebView() {
        onView(isAssignableFrom(WebView::class.java)).perform(click())
    }

    private fun goBack(testState: WebViewTestState) {
        composeRule.runOnIdle { testState.controller.get().goBack() }
    }

    private fun currentWebViewState(): CurrentWebViewState {
        val state = AtomicReference<CurrentWebViewState>()

        onView(isAssignableFrom(WebView::class.java)).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

                override fun getDescription(): String = "read the current WebView state"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val webView = view as WebView
                    state.set(
                        CurrentWebViewState(
                            url = webView.url,
                            canGoBack = webView.canGoBack(),
                        ),
                    )
                }
            },
        )

        return checkNotNull(state.get())
    }

    private fun evaluateJavascript(script: String) {
        val completed = AtomicBoolean(false)
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("evaluate JavaScript") { webView ->
                webView.evaluateJavascript(script) { completed.set(true) }
            },
        )
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) { completed.get() }
    }

    private fun waitForScrollY(minimumScrollY: Int) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            currentWebViewScrollY() >= minimumScrollY
        }
    }

    private fun currentWebViewScrollY(): Int {
        val scrollY = AtomicInteger()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("read the WebView root scroll position") { webView ->
                scrollY.set(webView.scrollY)
            },
        )
        return scrollY.get()
    }

    private fun performFastUpwardSwipe(testState: WebViewTestState): Int {
        val countAtRelease = AtomicInteger()
        onView(isAssignableFrom(WebView::class.java)).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

                override fun getDescription(): String = "perform a fast upward WebView swipe"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val downTime = SystemClock.uptimeMillis()
                    val x = view.width / 2f
                    val startY = view.height * 0.8f
                    val endY = view.height * 0.2f
                    dispatchTouch(view, downTime, downTime, MotionEvent.ACTION_DOWN, x, startY)
                    uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                    repeat(SWIPE_MOVE_COUNT) { index ->
                        val progress = (index + 1f) / SWIPE_MOVE_COUNT
                        val y = startY + ((endY - startY) * progress)
                        dispatchTouch(
                            view,
                            downTime,
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_MOVE,
                            x,
                            y,
                        )
                        uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                    }
                    dispatchTouch(
                        view,
                        downTime,
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP,
                        x,
                        endY,
                    )
                    countAtRelease.set(testState.rootScrollCount.get())
                    uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                }
            },
        )
        return countAtRelease.get()
    }

    private fun dispatchWebViewConfigurationChanged() {
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("dispatch WebView configuration change") { webView ->
                webView.dispatchConfigurationChanged(Configuration(webView.resources.configuration))
            },
        )
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
        }
    }

    private fun dispatchTouch(
        view: View,
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun waitForPage(
        testState: WebViewTestState,
        path: String,
    ) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            testState.page.get().url.endsWith(path)
        }
    }

    private fun htmlResponse(body: String): MockResponse =
        MockResponse.Builder()
            .addHeader("Content-Type", "text/html; charset=utf-8")
            .body(
                """
                <!doctype html>
                <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <link rel="icon" href="data:,">
                        <style>
                            html, body, form { width: 100%; height: 100%; margin: 0; }
                            .tap-target { display: block; width: 100%; height: 100%; }
                        </style>
                    </head>
                    <body>$body</body>
                </html>
                """.trimIndent(),
            )
            .build()

    private class RecordingDispatcher : Dispatcher() {
        private val responses = ConcurrentHashMap<String, MockResponse>()
        private val requests = ConcurrentLinkedQueue<RecordedRequest>()

        fun respond(path: String, response: MockResponse) {
            responses[path] = response
        }

        fun requestCount(path: String): Int =
            requests.count { it.url.encodedPath == path }

        fun singleRequest(path: String): RecordedRequest =
            requests.single { it.url.encodedPath == path }

        override fun dispatch(request: RecordedRequest): MockResponse {
            requests += request
            return responses[request.url.encodedPath]
                ?: MockResponse.Builder().code(404).build()
        }
    }

    private class WebViewTestState {
        val controller = AtomicReference<SessionWebViewController>()
        val page = AtomicReference(PageState())
        val rootScrollCount = AtomicInteger()
    }

    private data class PageState(
        val url: String = "",
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
    )

    private data class CurrentWebViewState(
        val url: String?,
        val canGoBack: Boolean,
    )

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
        const val FLING_TIMEOUT_MILLIS = 3_000L
        const val SWIPE_MOVE_COUNT = 5
        const val SWIPE_MOVE_INTERVAL_MILLIS = 16L
    }
}
