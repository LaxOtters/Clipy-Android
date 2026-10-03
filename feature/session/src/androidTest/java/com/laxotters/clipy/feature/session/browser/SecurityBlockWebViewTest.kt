package com.laxotters.clipy.feature.session.browser

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebViewFeature
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarLayout
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.browser.handoff.ExternalAppHandler
import com.laxotters.clipy.feature.session.browser.webview.SessionConfigurationTestActivity
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityBlockWebViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<SessionConfigurationTestActivity>()

    private lateinit var server: MockWebServer
    private lateinit var untrusted: MockWebServer
    private val browserState = AtomicReference<SessionBrowserState>()
    private val untrustedRequests = AtomicInteger()
    private val externalLaunches = ConcurrentLinkedQueue<String>()
    private val homeRequests = AtomicInteger()
    private val finishedPages = AtomicInteger()

    @Before
    fun setUp() {
        // 기기가 신뢰하지 않는 자체 서명 인증서로 HTTPS 응답합니다.
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        untrusted = MockWebServer()
        untrusted.useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory())
        untrusted.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                untrustedRequests.incrementAndGet()
                return html("<p>Insecure</p>")
            }
        }
        untrusted.start()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/mixed" -> html("""<p>Usable</p><img src="${untrusted.url("/image.png")}">""")
                else -> html(
                    """<a id="secure" style="display:block;padding:48px" href="${untrusted.url("/bad")}">Secure</a>""",
                )
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
        untrusted.close()
    }

    @Test
    fun sslLinkFromPage_blocksAndGoBackShowsSamePageWithoutHistoryMove() {
        val start = server.url("/start").toString()
        launchBrowser(start)
        val webView = currentWebView()

        composeRule.runOnIdle { webView.evaluateJavascript("document.getElementById('secure').click()", null) }

        waitForText("This connection isn’t secure")
        composeRule.onNodeWithText("The site’s security certificate couldn’t be verified.").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Try again").fetchSemanticsNodes().size)
        assertEquals(0, untrustedRequests.get())
        assertEquals(SessionRecoveryAction.RETURN_TO_PAGE, browserState.get().pageRecovery?.action)
        assertTrue(browserState.get().canGoBack)

        composeRule.onNodeWithText("Go back").performClick()
        composeRule.waitUntil(TIMEOUT) { browserState.get().pageRecovery == null }
        assertEquals(start, browserState.get().currentUrl)
        assertFalse(browserState.get().canGoBack)
        assertEquals(View.VISIBLE, webView.visibility)
        assertTrue(externalLaunches.isEmpty())
    }

    @Test
    fun sslSubresource_keepsPageWithoutBlock() {
        val mixed = server.url("/mixed").toString()
        launchBrowser(mixed)
        Thread.sleep(SETTLE_MS)
        assertNull(browserState.get().pageRecovery)
        assertEquals(0, composeRule.onAllNodesWithText("This connection isn’t secure").fetchSemanticsNodes().size)
    }

    @Test
    fun sslFirstLoad_offersHomeOnly() {
        launchBrowser(untrusted.url("/bad").toString(), waitForUrl = false)
        waitForText("This connection isn’t secure")
        assertEquals(0, untrustedRequests.get())
        assertFalse(browserState.get().canGoBack)

        composeRule.onNodeWithText("Go home").performClick()
        assertEquals(1, homeRequests.get())
    }

    @Test
    fun safeBrowsingMalware_blocksAndGoBackShowsPreviousPage() {
        // 기기 WebView가 Safe Browsing callback을 지원하지 않으면 위험 사이트 판정 자체가 없습니다.
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_HIT))
        val start = server.url("/start").toString()
        launchBrowser(start)
        val webView = currentWebView()

        composeRule.runOnIdle { webView.loadUrl(SAFE_BROWSING_MALWARE_URL) }

        waitForText("This site may be unsafe")
        composeRule.onNodeWithText("This site was flagged as potentially dangerous.").assertIsDisplayed()
        composeRule.onNodeWithText("Go back").performClick()
        composeRule.waitUntil(TIMEOUT) { browserState.get().pageRecovery == null }
        assertEquals(start, browserState.get().currentUrl)
        assertTrue(externalLaunches.isEmpty())
    }

    private fun launchBrowser(initialUrl: String, waitForUrl: Boolean = true) {
        composeRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    val state = rememberSessionBrowserState("security-test", initialUrl)
                    browserState.set(state)
                    SessionBrowser(
                        state = state,
                        initialUrl = initialUrl,
                        onPageFinished = { finishedPages.incrementAndGet() },
                        onRootScrolled = {},
                        onHomeRequested = { homeRequests.incrementAndGet() },
                        modifier = Modifier.fillMaxSize(),
                        externalAppHandlerFactory = {
                            ExternalAppHandler(resolveAppName = { null }, launch = {
                                externalLaunches += it.dataString.orEmpty()
                                false
                            })
                        },
                    )
                }
            }
        }
        if (waitForUrl) {
            composeRule.waitUntil(TIMEOUT) {
                browserState.get()?.currentUrl == initialUrl && finishedPages.get() > 0
            }
            // 본문 표시 준비 callback은 완료 callback 뒤 다음 frame에서 올 수 있어 잠시 기다립니다.
            Thread.sleep(READY_WAIT_MS)
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(TIMEOUT) {
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

    private fun html(body: String) = MockResponse.Builder()
        .addHeader("Content-Type", "text/html; charset=utf-8")
        .body(body)
        .build()

    private companion object {
        const val TIMEOUT = 10_000L
        const val SETTLE_MS = 1_500L
        const val READY_WAIT_MS = 500L
        const val SAFE_BROWSING_MALWARE_URL = "chrome://safe-browsing/match?type=malware"
    }
}
