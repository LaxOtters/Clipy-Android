package com.laxotters.clipy.feature.session.browser

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarLayout
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.webview.SessionConfigurationTestActivity
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.launch
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

@RunWith(AndroidJUnit4::class)
class SessionBrowserUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<SessionConfigurationTestActivity>()

    private lateinit var server: MockWebServer
    private val documentRequests = AtomicInteger()
    private val documentLog = ConcurrentLinkedQueue<String>()
    private val browserVisible = mutableStateOf(true)
    private val generation = mutableStateOf(0)
    private val pageFinishedGeneration = AtomicInteger(-1)
    private val browserState = AtomicReference<SessionBrowserState>()
    private var showOtherMessage: () -> Unit = {}

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.url.encodedPath == "/file.bin") {
                    MockResponse.Builder()
                        .addHeader("Content-Type", "application/octet-stream")
                        .addHeader("Content-Disposition", "attachment; filename=sample.bin")
                        .body("sample")
                        .build()
                } else {
                    if (request.url.encodedPath == "/start") documentRequests.incrementAndGet()
                    // WebView가 자동으로 보내는 favicon 요청은 문서 요청 순서에서 제외합니다.
                    if (request.url.encodedPath != "/favicon.ico") {
                        documentLog += "${request.method} ${request.url.encodedPath}"
                    }
                    MockResponse.Builder()
                        .addHeader("Content-Type", "text/html; charset=utf-8")
                        .body(
                            """<a id="open" style="display:block;padding:48px" href="clipy-test://open">Open</a>""" +
                                """<form id="form" method="post" action="/submit"><input name="token" value="kept"></form>""",
                        )
                        .build()
                }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun failedExternalLaunch_showsSnackbarWithoutSessionViewModel() {
        launchBrowser()
        val original = currentWebView()

        onWebView().withElement(findElement(Locator.ID, "open")).perform(webClick())
        waitForText("Continue")
        composeRule.onNodeWithText("Continue").performClick()

        waitForText("Couldn't open external app")
        assertSame(original, currentWebView())
        assertEquals(server.url("/start").toString(), browserState.get().currentUrl)
    }

    @Test
    fun unsupportedDownload_showsSnackbarWithoutSessionViewModel() {
        launchBrowser()
        requestUnsupportedDownload()

        waitForText("Downloads aren't supported")
        assertEquals(0, composeRule.onAllNodesWithText("Open in browser?").fetchSemanticsNodes().size)
    }

    @Test
    fun browserRemoval_cancelsOwnSnackbarAndPreservesOtherCallerRequest() {
        launchBrowser()
        requestUnsupportedDownload()
        waitForText("Downloads aren't supported")

        composeRule.runOnIdle {
            showOtherMessage()
            browserVisible.value = false
        }

        waitForText("Other caller message")
        assertEquals(0, composeRule.onAllNodesWithText("Downloads aren't supported").fetchSemanticsNodes().size)
        assertFalse(browserState.get().canGoBack)
    }

    @Test
    fun recomposition_keepsWebViewAndDoesNotReloadInitialInput() {
        launchBrowser()
        val original = currentWebView()
        val requests = documentRequests.get()

        composeRule.runOnIdle { generation.value++ }
        composeRule.waitForIdle()

        assertSame(original, currentWebView())
        assertEquals(requests, documentRequests.get())

        composeRule.runOnIdle { browserState.get().reload() }
        composeRule.waitUntil(10_000) { pageFinishedGeneration.get() == 1 }
        assertEquals(requests + 1, documentRequests.get())
    }

    @Test
    fun browserReplacement_startsWithoutPreviousWebViewHistory() {
        launchBrowser()
        val original = currentWebView()
        val nextUrl = server.url("/next").toString()
        composeRule.runOnIdle { original.loadUrl(nextUrl) }
        composeRule.waitUntil(10_000) { browserState.get().currentUrl == nextUrl }
        assertTrue(browserState.get().canGoBack)

        composeRule.runOnIdle { browserVisible.value = false }
        composeRule.waitForIdle()
        assertFalse(browserState.get().canGoBack)
        composeRule.runOnIdle { browserVisible.value = true }
        composeRule.waitUntil(10_000) { browserState.get().currentUrl == server.url("/start").toString() }

        assertNotSame(original, currentWebView())
        assertFalse(browserState.get().canGoBack)
        assertFalse(browserState.get().canGoForward)
    }

    @Test
    fun rendererCrash_waitsForReopenAndOpensLastPageInNewWebView() {
        launchBrowser()
        val nextUrl = server.url("/next").toString()
        val original = currentWebView()
        composeRule.runOnIdle { original.loadUrl(nextUrl) }
        composeRule.waitUntil(10_000) { browserState.get().currentUrl == nextUrl }

        composeRule.runOnIdle { original.loadUrl("chrome://crash") }
        waitForText("This page stopped responding")
        composeRule.onNodeWithText("Reopen page").assertIsDisplayed()
        assertFalse(browserState.get().canGoBack)
        // 사용자가 선택하기 전에는 다시 열지 않습니다.
        val requestsBeforeReopen = documentLog.toList()
        Thread.sleep(AUTO_REOPEN_WAIT_MS)
        assertEquals(requestsBeforeReopen, documentLog.toList())

        composeRule.onNodeWithText("Reopen page").performClick()
        composeRule.waitUntil(10_000) {
            browserState.get().pageRecovery == null && browserState.get().currentUrl == nextUrl
        }
        assertNotSame(original, currentWebView())
        assertFalse(browserState.get().canGoBack)
        assertEquals("GET /next", documentLog.last())
    }

    @Test
    fun rendererKillAfterPost_refreshReopensWithGetWithoutResubmitting() {
        launchBrowser()
        val original = currentWebView()
        composeRule.runOnIdle { original.evaluateJavascript("document.getElementById('form').submit()", null) }
        val submitUrl = server.url("/submit").toString()
        composeRule.waitUntil(10_000) { browserState.get().currentUrl == submitUrl }

        composeRule.runOnIdle { original.loadUrl("chrome://kill") }
        waitForText("This page stopped responding")

        composeRule.runOnIdle { browserState.get().reload() }
        composeRule.waitUntil(10_000) { browserState.get().pageRecovery == null }
        assertNotSame(original, currentWebView())
        assertEquals(1, documentLog.count { it == "POST /submit" })
        assertEquals("GET /submit", documentLog.last())
    }

    private fun launchBrowser() {
        val initialUrl = server.url("/start").toString()
        composeRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    val otherController = rememberClipySnackbarController()
                    val otherScope = rememberCoroutineScope()
                    showOtherMessage = {
                        otherScope.launch {
                            otherController.showSnackbar("Other caller message")
                        }
                    }
                    if (browserVisible.value) {
                        val state = rememberSessionBrowserState("browser-test", initialUrl)
                        browserState.set(state)
                        val currentGeneration = generation.value
                        SessionBrowser(
                            state = state,
                            initialUrl = initialUrl,
                            onPageFinished = { pageFinishedGeneration.set(currentGeneration) },
                            onRootScrolled = {},
                            onHomeRequested = {},
                            modifier = Modifier.fillMaxSize(),
                            externalAppHandlerFactory = {
                                ExternalAppHandler(resolveAppName = { null }, launch = { false })
                            },
                        )
                    }
                }
            }
        }
        composeRule.waitUntil(10_000) { browserState.get()?.currentUrl == initialUrl }
    }

    private fun requestUnsupportedDownload() {
        val webView = currentWebView()
        composeRule.runOnIdle { webView.loadUrl(server.url("/file.bin").toString()) }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun currentWebView(): WebView {
        val result = AtomicReference<WebView>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

                override fun getDescription(): String = "Read current Browser WebView"

                override fun perform(uiController: UiController, view: View) {
                    result.set(view as WebView)
                }
            },
        )
        return result.get()
    }

    private companion object {
        const val AUTO_REOPEN_WAIT_MS = 1_000L
    }
}
