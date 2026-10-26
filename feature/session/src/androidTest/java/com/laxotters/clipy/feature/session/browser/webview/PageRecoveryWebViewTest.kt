package com.laxotters.clipy.feature.session.browser.webview

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarLayout
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.browser.SessionBrowserState
import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageFailure
import com.laxotters.clipy.feature.session.browser.SessionPageLoad
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogHosts
import com.laxotters.clipy.feature.session.browser.dialog.rememberSessionWebDialogState
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.recovery.SessionRecoveryContent
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
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
class PageRecoveryWebViewTest {
    @get:Rule val composeRule = createAndroidComposeRule<SessionConfigurationTestActivity>()
    private lateinit var server: MockWebServer
    private lateinit var browserState: SessionBrowserState
    private val allowResponse = AtomicBoolean(false)
    private val retryResponse = AtomicReference<MockResponse?>()
    private val externalLaunches = ConcurrentLinkedQueue<String>()
    private val externalErrors = ConcurrentLinkedQueue<String>()
    private val requests = ConcurrentLinkedQueue<RecordedRequest>()
    private val responseGate = AtomicReference<CountDownLatch?>()
    private val imageGate = CountDownLatch(1)
    private val slowImage = AtomicBoolean(false)
    private val finishedUrls = ConcurrentLinkedQueue<String>()
    private val readyUrls = ConcurrentLinkedQueue<String>()
    private val navigation = AtomicReference<SessionPageNavigation>()
    private val controller = AtomicReference<SessionWebViewController>()
    private val network = AtomicReference(SessionNetworkAvailability.VALIDATED)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return when (request.url.encodedPath) {
                    "/failed", "/submit" -> if (allowResponse.get()) {
                        responseGate.get()?.await(TIMEOUT, TimeUnit.MILLISECONDS)
                        retryResponse.get() ?: html(
                            "<p id='result'>Recovered</p>" + if (slowImage.get()) "<img src='/slow-image'>" else "",
                        )
                    } else {
                        MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build()
                    }
                    "/404" -> html("<p id='result'>Site 404</p>", 404)
                    "/download" -> downloadResponse()
                    "/500" -> html("<p id='result'>Site 500</p>", 500)
                    "/subresources" -> html(
                        "<p id='result'>Usable</p><iframe src='/failed'></iframe><img src='/failed'>",
                    )
                    "/slow-image" -> {
                        imageGate.await(TIMEOUT, TimeUnit.MILLISECONDS)
                        MockResponse.Builder().code(204).build()
                    }
                    else -> html(
                        "<p id='result'>Previous</p><form method='post' action='/submit'><input name='token' value='kept'></form>",
                    )
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        responseGate.get()?.countDown()
        imageGate.countDown()
        server.close()
    }

    @Test
    fun retryExternalRedirect_cancelAndLaunchFailure_leaveRecoveryActionable() {
        launchWebView("/failed")
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery?.showOverlay == true && finishedUrls.any { it.endsWith("/failed") }
        }
        retryResponse.set(MockResponse.Builder().code(302).addHeader("Location", "clipy-auth://callback").build())
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        waitForExternalDialog()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performTouchInput { click() }
        composeRule.onNodeWithText("Try again").assertIsEnabled()
        assertTrue(externalLaunches.isEmpty())
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        waitForExternalDialog()
        composeRule.onNodeWithText("Continue").assertIsDisplayed().performTouchInput { click() }
        composeRule.onNodeWithText("Try again").assertIsEnabled()
        assertEquals(listOf("clipy-auth://callback"), externalLaunches.toList())
        assertEquals(1, externalErrors.size)
        retryResponse.set(null)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
    }

    @Test
    fun retryDownload_cancelKeepsFailureTargetAndNextRetryCanRecover() {
        launchWebView("/failed")
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery?.showOverlay == true && finishedUrls.any { it.endsWith("/failed") }
        }
        val target = browserState.pageRecovery!!.navigation
        retryResponse.set(downloadResponse())
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        waitForExternalDialog()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performTouchInput { click() }
        composeRule.onNodeWithText("Try again").assertIsEnabled()
        assertEquals(target, browserState.pageRecovery!!.navigation)
        assertTrue(externalLaunches.isEmpty())
        retryResponse.set(MockResponse.Builder().code(302).addHeader("Location", server.url("/download")).build())
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        waitForExternalDialog()
        composeRule.onNodeWithText("Cancel").performTouchInput { click() }
        composeRule.onNodeWithText("Try again").assertIsEnabled()
        assertEquals(target, browserState.pageRecovery!!.navigation)
        retryResponse.set(null)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
        onWebView().withElement(findElement(Locator.ID, "result"))
            .check(webMatches(getText(), containsString("Recovered")))
    }

    @Test
    fun offlineReload_reportsBeforeReturningAndPreservesHistory() {
        launchWebView("/earlier")
        composeRule.waitUntil(TIMEOUT) {
            finishedUrls.any { it.endsWith("/earlier") } && readyUrls.any { it.endsWith("/earlier") }
        }
        var historySize = 0
        var historyIndex = 0
        webAction("record loaded document history") {
            assertEquals(View.VISIBLE, it.visibility)
            val history = it.copyBackForwardList()
            historySize = history.size
            historyIndex = history.currentIndex
        }
        val requestCount = requests.size
        network.set(SessionNetworkAvailability.UNAVAILABLE)
        composeRule.runOnIdle {
            controller.get().reload()
            val recovery = checkNotNull(browserState.pageRecovery)
            assertEquals(SessionPageErrorCause.NO_NETWORK, recovery.cause)
            assertTrue(recovery.showOverlay)
            assertFalse(recovery.isRunning)
        }
        composeRule.onNodeWithText("No internet connection").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        webAction("verify offline refresh does not navigate") {
            val history = it.copyBackForwardList()
            assertEquals(historySize, history.size)
            assertEquals(historyIndex, history.currentIndex)
            assertEquals(View.INVISIBLE, it.visibility)
        }
        assertEquals(requestCount, requests.size)
    }

    @Test
    fun offlineRetry_keepsHistoryAndRecoversOnlyAfterManualRetryOnline() {
        launchWebView("/failed")
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery?.showOverlay == true && finishedUrls.any { it.endsWith("/failed") }
        }
        var historySize = 0
        var historyIndex = 0
        webAction("record failed document history") {
            val history = it.copyBackForwardList()
            historySize = history.size
            historyIndex = history.currentIndex
        }
        val firstNavigation = browserState.pageRecovery!!.navigation
        val requestCount = requests.size
        network.set(SessionNetworkAvailability.UNAVAILABLE)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.runOnIdle {
            val recovery = checkNotNull(browserState.pageRecovery)
            assertTrue(recovery.navigation != firstNavigation)
            assertEquals(SessionPageErrorCause.NO_NETWORK, recovery.cause)
            assertFalse(recovery.isRunning)
        }
        composeRule.onNodeWithText("No internet connection").assertIsDisplayed()
        webAction("verify offline retry does not navigate") {
            val history = it.copyBackForwardList()
            assertEquals(historySize, history.size)
            assertEquals(historyIndex, history.currentIndex)
            assertEquals(View.INVISIBLE, it.visibility)
        }
        assertEquals(requestCount, requests.size)
        val offlineRecovery = browserState.pageRecovery
        allowResponse.set(true)
        network.set(SessionNetworkAvailability.VALIDATED)
        composeRule.runOnIdle { assertEquals(offlineRecovery, browserState.pageRecovery) }
        assertEquals(requestCount, requests.size)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
        assertTrue(requests.size > requestCount)
        onWebView().withElement(findElement(Locator.ID, "result"))
            .check(webMatches(getText(), containsString("Recovered")))
    }

    @Test
    fun retry_disablesButtonImmediatelyAndShowsBodyBeforeSlowImageCompletes() {
        launchWebView("/failed")
        // 실패 문서의 완료 callback을 재시도 문서의 완료로 세지 않도록 먼저 기다립니다.
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery?.showOverlay == true && finishedUrls.any { it.endsWith("/failed") }
        }
        finishedUrls.clear()
        val gate = CountDownLatch(1)
        responseGate.set(gate)
        slowImage.set(true)
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.onNodeWithText("Try again").assertIsNotEnabled()
        gate.countDown()
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
        assertTrue(requests.any { it.url.encodedPath == "/slow-image" })
        assertFalse(finishedUrls.any { it.endsWith("/failed") })
        imageGate.countDown()
        onWebView().withElement(findElement(Locator.ID, "result"))
            .check(webMatches(getText(), containsString("Recovered")))
    }

    @Test
    fun firstGetFailure_tryAgainRequestsFailedUrlAndClearsOverlayOnlyAfterSuccess() {
        launchWebView("/failed")
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery?.showOverlay == true }
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        assertTrue(browserState.pageRecovery!!.navigation.url.endsWith("/failed"))
        val failedCount = requests.count { it.url.encodedPath == "/failed" }
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery == null && browserState.currentUrl.endsWith("/failed")
        }
        assertTrue(requests.count { it.url.encodedPath == "/failed" } > failedCount)
        assertTrue(requests.filter { it.url.encodedPath == "/failed" }.all { it.method == "GET" })
        onWebView().withElement(
            findElement(Locator.ID, "result"),
        ).check(webMatches(getText(), containsString("Recovered")))
    }

    @Test
    fun retryFails_keepsOverlayAndEnablesFollowingRetry() {
        launchWebView("/failed")
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery?.showOverlay == true }
        val firstNavigation = browserState.pageRecovery!!.navigation
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) {
            val recovery = browserState.pageRecovery
            recovery != null && recovery.navigation != firstNavigation && !recovery.isRunning
        }
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
    }

    @Test
    fun postFailure_goBackReturnsToPreviousDocumentWithoutResending() {
        launchWebView("/earlier")
        composeRule.waitUntil(TIMEOUT) {
            finishedUrls.any { it.endsWith("/earlier") } && readyUrls.any { it.endsWith("/earlier") }
        }
        // 실패한 POST가 history에 추가되는지에 의존하지 않도록 이전 GET 기록을 먼저 만듭니다.
        webAction("open form page") { it.loadUrl(server.url("/start").toString()) }
        composeRule.waitUntil(TIMEOUT) {
            finishedUrls.any { it.endsWith("/start") } && readyUrls.any { it.endsWith("/start") }
        }
        webAction("submit form") { it.evaluateJavascript("document.forms[0].submit()", null) }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery?.showOverlay == true }
        composeRule.onAllNodesWithText("Try again").assertCountEquals(0)
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery?.action ==
                com.laxotters.clipy.feature.session.browser.SessionRecoveryAction.GO_BACK
        }
        composeRule.onNodeWithText("Go back").assertIsDisplayed()
        val postCount = requests.count { it.url.encodedPath == "/submit" && it.method == "POST" }
        assertTrue(postCount > 0)
        composeRule.onNodeWithText("Go back").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery == null }
        assertEquals(postCount, requests.count { it.url.encodedPath == "/submit" && it.method == "POST" })
        onWebView().withElement(
            findElement(Locator.ID, "result"),
        ).check(webMatches(getText(), containsString("Previous")))
    }

    @Test
    fun postFailure_reloadDoesNotResendPostBodyAndKeepsActionEnabled() {
        launchWebView("/start")
        composeRule.waitUntil(TIMEOUT) { browserState.currentUrl.endsWith("/start") }
        webAction("submit form") { it.evaluateJavascript("document.forms[0].submit()", null) }
        composeRule.waitUntil(TIMEOUT) { browserState.pageRecovery?.showOverlay == true }
        val postCount = requests.count { it.url.encodedPath == "/submit" && it.method == "POST" }
        assertTrue(postCount > 0)
        composeRule.runOnIdle { controller.get().reload() }
        onView(isAssignableFrom(WebView::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)
            override fun getDescription() = "observe the local POST resubmission window"
            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                uiController.loopMainThreadForAtLeast(1_000)
            }
        })
        assertEquals(postCount, requests.count { it.url.encodedPath == "/submit" && it.method == "POST" })
        val recovery = browserState.pageRecovery!!
        assertFalse(recovery.isRunning)
        // 이전 방문 기록이 없는 POST 실패라 Go back 대신 Go home이 표시될 수 있습니다.
        val label = if (recovery.action == SessionRecoveryAction.GO_BACK) "Go back" else "Go home"
        composeRule.onNodeWithText(label).assertIsEnabled()
    }

    @Test
    fun normal404And500HtmlAndFailedSubresources_keepSiteDocument() {
        launchWebView("/404")
        listOf("/404", "/500", "/subresources").forEach { path ->
            webAction("open $path") { it.loadUrl(server.url(path).toString()) }
            composeRule.waitUntil(TIMEOUT) { browserState.currentUrl.endsWith(path) }
            assertEquals(null, browserState.pageRecovery)
        }
        onWebView().withElement(
            findElement(Locator.ID, "result"),
        ).check(webMatches(getText(), containsString("Usable")))
    }

    @Test
    fun retainedDocumentSnackbar_retryUsesFailureTargetAndPreservesCurrentBody() {
        launchWebView("/start")
        composeRule.waitUntil(TIMEOUT) { browserState.currentUrl.endsWith("/start") }
        val failed = navigation.get().copy(url = server.url("/failed").toString())
        composeRule.runOnIdle {
            browserState.onPageLoadChanged(
                SessionPageLoad.Failed(
                    SessionPageFailure(
                        failed,
                        SessionLoadError.CONNECTION,
                        "GET",
                        true,
                        false,
                        false,
                        SessionNetworkAvailability.VALIDATED,
                    ),
                ),
            )
        }
        composeRule.onNodeWithText("Couldn't load this page").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
        assertFalse(browserState.pageRecovery!!.showOverlay)
        onWebView().withElement(
            findElement(Locator.ID, "result"),
        ).check(webMatches(getText(), containsString("Previous")))
        allowResponse.set(true)
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.waitUntil(TIMEOUT) {
            browserState.pageRecovery == null &&
                browserState.currentUrl.endsWith("/failed")
        }
        assertTrue(requests.any { it.url.encodedPath == "/failed" && it.method == "GET" })
    }

    private fun waitForExternalDialog() {
        composeRule.waitUntil(TIMEOUT) {
            composeRule.onAllNodesWithText("Cancel").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun launchWebView(path: String) {
        val initialUrl = server.url(path).toString()
        browserState = SessionBrowserState({ initialUrl })
        controller.set(browserState.controller)
        composeRule.setContent {
            ClipyTheme {
                val dialogState = rememberSessionWebDialogState()
                ClipySnackbarLayout(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        SessionWebView(
                            url = initialUrl,
                            controller = browserState.controller,
                            dialogState = dialogState,
                            callbacks = callbacks(),
                            modifier = Modifier.fillMaxSize(),
                            networkAvailability = { network.get() },
                            externalAppHandlerFactory = {
                                ExternalAppHandler(resolveAppName = { null }, launch = {
                                    externalLaunches += it.dataString.orEmpty()
                                    false
                                })
                            },
                        )
                        SessionRecoveryContent(browserState, {})
                    }
                }
                SessionWebDialogHosts(dialogState)
            }
        }
    }

    private fun callbacks() = SessionWebViewCallbacks(
        onPageFinished = {
            finishedUrls += it.url
            browserState.updatePageInfo(it)
        },
        onRootScrolled = {},
        onErrorMessage = { externalErrors += it.message },
        onPageLoadChanged = {
            when (it) {
                is SessionPageLoad.Loading -> navigation.set(it.navigation)
                is SessionPageLoad.Ready -> readyUrls += it.navigation.url
                else -> Unit
            }
            browserState.onPageLoadChanged(it)
        },
        onHistoryChanged = browserState::updatePageInfo,
    )

    private fun webAction(description: String, block: (WebView) -> Unit) {
        onView(isAssignableFrom(WebView::class.java)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)
            override fun getDescription() = description
            override fun perform(uiController: UiController, view: View) {
                block(view as WebView)
            }
        })
    }

    private fun downloadResponse(): MockResponse = MockResponse.Builder()
        .addHeader("Content-Type", "application/octet-stream")
        .addHeader("Content-Disposition", "attachment; filename=example.bin").body("download").build()

    private fun html(body: String, code: Int = 200): MockResponse = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "text/html; charset=utf-8").addHeader("Cache-Control", "no-store")
        .body(
            "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'><link rel='icon' href='data:,'></head><body>$body</body></html>",
        )
        .build()

    private companion object {
        const val SESSION_ID = "recovery-test"
        const val TIMEOUT = 10_000L
    }
}
