package com.laxotters.clipy

import android.app.UiModeManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.laxotters.clipy.feature.main.MainActivity
import com.laxotters.clipy.feature.session.webview.SessionWebView
import com.laxotters.clipy.feature.session.webview.rememberSessionWebViewController
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = 31)
@RunWith(AndroidJUnit4::class)
class MainActivityWebViewConfigurationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var uiModeManager: UiModeManager
    private var originalNightMode = UiModeManager.MODE_NIGHT_NO
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    @Before
    fun setUp() {
        uiModeManager = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSystemService(UiModeManager::class.java)
        composeRule.activityRule.scenario.onActivity {
            originalRequestedOrientation = it.requestedOrientation
            originalNightMode = if (
                it.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            ) {
                UiModeManager.MODE_NIGHT_YES
            } else {
                UiModeManager.MODE_NIGHT_NO
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = originalRequestedOrientation
        }
        uiModeManager.setApplicationNightMode(originalNightMode)
    }

    @Test
    fun mainActivity_manifestHandlesEveryWebViewContinuityConfiguration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activityInfo = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(
            EXPECTED_CONFIG_CHANGES,
            activityInfo.configChanges and EXPECTED_CONFIG_CHANGES,
        )
    }

    @Test
    fun configurationChanges_keepActivityWebViewDomAndHistory() {
        val initialUrl = htmlDataUrl(
            """<button id="draft" onclick="this.textContent='kept'">before</button>""",
        )
        val pageState = launchWebView(initialUrl)

        changeDraftText()
        assertDraftText("kept")
        // 같은 문서 안에서 history만 추가해 구성 변경 전 DOM 상태를 유지합니다.
        navigateWebView("$initialUrl#current")
        waitForPage(pageState, "#current")

        val activityIdentity = System.identityHashCode(composeRule.activity)
        val beforeChange = currentWebViewState()

        changeOrientation()
        assertContinuity(activityIdentity, beforeChange)

        changeNightMode()
        assertContinuity(activityIdentity, beforeChange)
    }

    private fun launchWebView(initialUrl: String): AtomicReference<PageState> {
        val pageState = AtomicReference(PageState())

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val controller = rememberSessionWebViewController()
                SessionWebView(
                    url = initialUrl,
                    controller = controller,
                    onPageStateChanged = { url, _, _ ->
                        pageState.set(PageState(url = url))
                    },
                    onRootScrolled = { _, _, _, _ -> },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            pageState.get().url.isNotEmpty()
        }
        return pageState
    }

    private fun changeOrientation() {
        val currentOrientation = composeRule.activity.resources.configuration.orientation
        val targetOrientation = if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            Configuration.ORIENTATION_PORTRAIT
        } else {
            Configuration.ORIENTATION_LANDSCAPE
        }
        val requestedOrientation = if (targetOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = requestedOrientation
        }
        composeRule.waitUntil(timeoutMillis = CONFIGURATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation == targetOrientation
        }
    }

    private fun changeNightMode() {
        val currentNightMode = composeRule.activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        val targetNightMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            Configuration.UI_MODE_NIGHT_NO
        } else {
            Configuration.UI_MODE_NIGHT_YES
        }
        val applicationNightMode = if (targetNightMode == Configuration.UI_MODE_NIGHT_YES) {
            UiModeManager.MODE_NIGHT_YES
        } else {
            UiModeManager.MODE_NIGHT_NO
        }

        uiModeManager.setApplicationNightMode(applicationNightMode)
        composeRule.waitUntil(timeoutMillis = CONFIGURATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == targetNightMode
        }
    }

    private fun assertContinuity(
        activityIdentity: Int,
        expectedWebViewState: WebViewState,
    ) {
        assertEquals(activityIdentity, System.identityHashCode(composeRule.activity))
        assertEquals(expectedWebViewState, currentWebViewState())
        assertDraftText("kept")
    }

    private fun currentWebViewState(): WebViewState {
        val state = AtomicReference<WebViewState>()

        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("read the WebView identity, URL and history") { webView ->
                val history = webView.copyBackForwardList()
                state.set(
                    WebViewState(
                        identity = System.identityHashCode(webView),
                        url = webView.url.orEmpty(),
                        canGoBack = webView.canGoBack(),
                        canGoForward = webView.canGoForward(),
                        historySize = history.size,
                        historyIndex = history.currentIndex,
                    ),
                )
            },
        )

        return checkNotNull(state.get())
    }

    private fun navigateWebView(url: String) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("navigate the WebView test fixture") { webView ->
                webView.loadUrl(url)
            },
        )
    }

    private fun changeDraftText() {
        onWebView()
            .withElement(findElement(Locator.ID, "draft"))
            .perform(webClick())
    }

    private fun assertDraftText(text: String) {
        onWebView()
            .withElement(findElement(Locator.ID, "draft"))
            .check(webMatches(getText(), containsString(text)))
    }

    private fun waitForPage(
        pageState: AtomicReference<PageState>,
        path: String,
    ) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            pageState.get().url.endsWith(path)
        }
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

    private fun htmlDataUrl(body: String): String =
        "data:text/html;charset=utf-8," +
            Uri.encode(
                """
                <!doctype html>
                <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                    </head>
                    <body>$body</body>
                </html>
                """.trimIndent(),
            )

    private data class PageState(
        val url: String = "",
    )

    private data class WebViewState(
        val identity: Int,
        val url: String,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
        val historySize: Int,
        val historyIndex: Int,
    )

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
        const val CONFIGURATION_TIMEOUT_MILLIS = 15_000L

        const val EXPECTED_CONFIG_CHANGES =
            ActivityInfo.CONFIG_ORIENTATION or
                ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT or
                ActivityInfo.CONFIG_UI_MODE or
                ActivityInfo.CONFIG_FONT_SCALE or
                ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT or
                ActivityInfo.CONFIG_DENSITY
    }
}
