package com.laxotters.clipy.feature.session.webview

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.component.bottomsheet.BottomSheetValue
import com.laxotters.clipy.core.navigation.Route
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.feature.session.SessionTopBarState
import com.laxotters.clipy.feature.session.SessionUiEvent
import com.laxotters.clipy.feature.session.SessionViewModel
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SessionConfigurationContinuityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<SessionConfigurationTestActivity>()

    private lateinit var uiModeManager: UiModeManager
    private lateinit var displaySettings: EmulatorDisplaySettings
    private lateinit var sessionViewModel: SessionViewModel
    private var originalNightMode = UiModeManager.MODE_NIGHT_NO
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var originalOrientation = Configuration.ORIENTATION_UNDEFINED
    private var nightModeChanged = false

    @Before
    fun setUp() {
        displaySettings = EmulatorDisplaySettings(InstrumentationRegistry.getInstrumentation())
        uiModeManager = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSystemService(UiModeManager::class.java)
        composeRule.activityRule.scenario.onActivity {
            originalRequestedOrientation = it.requestedOrientation
            originalOrientation = it.resources.configuration.orientation
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
        displaySettings.restoreAll()
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = originalRequestedOrientation
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && nightModeChanged) {
            uiModeManager.setApplicationNightMode(originalNightMode)
        }
    }

    @Test
    fun configurationChanges_keepActivityWebViewDomHistoryScrollAndChrome() {
        verifyBrowsingConfigurationChanges(::configurationChanges)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun nightModeAndFontWeightChanges_keepActivityWebViewDomHistoryScrollAndChrome() {
        verifyBrowsingConfigurationChanges(::api31ConfigurationChanges)
    }

    private fun verifyBrowsingConfigurationChanges(
        scenarios: (Configuration) -> List<ConfigurationChange>,
    ) {
        val initialUrl = htmlDataUrl(
            """
            <button id="draft" onclick="this.textContent='kept'">before</button>
            <div style="height: 6000px"></div>
            """.trimIndent(),
        )
        val pageState = launchWebView(initialUrl)

        changeDraftText()
        assertDraftText("kept")
        // 같은 문서 안에서 history만 추가해 구성 변경 전 DOM 상태를 유지합니다.
        navigateWebView("$initialUrl#current")
        waitForPage(pageState, "#current")
        scrollWebViewTo(600)
        waitForScrollY(600)

        verifyUserRootScrollChromeTransitions()
        stopFlingAndScrollWebViewTo(600)
        waitForScrollY(600)

        val activityIdentity = System.identityHashCode(composeRule.activity)
        val beforeChange = currentWebViewState()
        val originalConfiguration = Configuration(composeRule.activity.resources.configuration)

        scenarios(originalConfiguration).forEach { scenario ->
            verifyConfigurationRoundTrip(activityIdentity, beforeChange, scenario)
        }
    }

    @Test
    fun configurationChanges_keepJavaScriptDialogAndPendingResult() {
        verifyDialogConfigurationChanges(::configurationChanges)
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun nightModeAndFontWeightChanges_keepJavaScriptDialogAndPendingResult() {
        verifyDialogConfigurationChanges(::api31ConfigurationChanges)
    }

    @Test
    fun configurationFailure_preservesChangeAndRestoreCauses() {
        val changeFailure = IllegalStateException("change failure")
        val restoreFailure = IllegalArgumentException("restore failure")

        val failure = runCatching {
            verifyConfigurationPhases(
                name = "orientation",
                change = { throw changeFailure },
                restore = { throw restoreFailure },
            )
        }.exceptionOrNull()

        assertEquals("orientation: change/verify failed", failure?.message)
        assertSame(changeFailure, failure?.cause)
        val suppressed = checkNotNull(failure).suppressed.single()
        assertEquals("orientation: restore/verify failed", suppressed.message)
        assertSame(restoreFailure, suppressed.cause)
    }

    @Test
    fun configurationRestoreFailure_reportsRestorePhase() {
        val restoreFailure = IllegalStateException("restore failure")

        val failure = runCatching {
            verifyConfigurationPhases(
                name = "density",
                change = {},
                restore = { throw restoreFailure },
            )
        }.exceptionOrNull()

        assertEquals("density: restore/verify failed", failure?.message)
        assertSame(restoreFailure, failure?.cause)
    }

    private fun verifyDialogConfigurationChanges(
        scenarios: (Configuration) -> List<ConfigurationChange>,
    ) {
        val pageState = launchWebView("about:blank")
        val webView = currentWebView()
        startJavascript(
            "document.title = 'pending'; var result = prompt('Configuration prompt', 'Initial'); " +
                "document.title = result === null ? 'cancelled' : result",
        )
        assertDialogDisplayed()
        composeRule.onNodeWithText("Initial").performTextReplacement("Retained input")

        val activityIdentity = System.identityHashCode(composeRule.activity)
        val originalConfiguration = Configuration(composeRule.activity.resources.configuration)

        scenarios(originalConfiguration).forEach { scenario ->
            verifyDialogConfigurationRoundTrip(activityIdentity, webView, scenario)
        }

        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            currentWebViewTitle(webView) == "Retained input"
        }
        assertTrue(pageState.get().url.isNotEmpty())
    }

    private fun configurationChanges(originalConfiguration: Configuration): List<ConfigurationChange> =
        listOf(
            orientationChange(),
            windowSizeChange(originalConfiguration),
            fontScaleChange(originalConfiguration),
            densityChange(originalConfiguration),
        )

    @RequiresApi(31)
    private fun api31ConfigurationChanges(originalConfiguration: Configuration): List<ConfigurationChange> =
        listOf(nightModeChange(originalConfiguration), fontWeightChange(originalConfiguration))

    private fun orientationChange(): ConfigurationChange = ConfigurationChange(
        name = "orientation",
        change = ::changeOrientation,
        changed = {
            composeRule.activity.resources.configuration.orientation != originalOrientation
        },
        restore = ::restoreOrientation,
        restored = {
            composeRule.activity.resources.configuration.orientation == originalOrientation
        },
    )

    private fun windowSizeChange(originalConfiguration: Configuration): ConfigurationChange = ConfigurationChange(
        name = "windowSize",
        change = { displaySettings.setWindowSize(displaySettings.changedWindowSize) },
        changed = {
            composeRule.activity.resources.configuration.screenWidthDp !=
                originalConfiguration.screenWidthDp
        },
        restore = displaySettings::restoreWindowSize,
        restored = {
            composeRule.activity.resources.configuration.screenWidthDp ==
                originalConfiguration.screenWidthDp
        },
    )

    @RequiresApi(31)
    private fun nightModeChange(originalConfiguration: Configuration): ConfigurationChange {
        val originalUiMode = originalConfiguration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return ConfigurationChange(
            name = "nightMode",
            change = ::changeNightMode,
            changed = {
                composeRule.activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK != originalUiMode
            },
            restore = { uiModeManager.setApplicationNightMode(originalNightMode) },
            restored = {
                composeRule.activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == originalUiMode
            },
        )
    }

    private fun fontScaleChange(originalConfiguration: Configuration): ConfigurationChange {
        val targetFontScale = if (originalConfiguration.fontScale < 1.1f) 1.15f else 1f
        return ConfigurationChange(
            name = "fontScale",
            change = { displaySettings.setFontScale(targetFontScale) },
            changed = {
                composeRule.activity.resources.configuration.fontScale.isCloseTo(targetFontScale)
            },
            restore = displaySettings::restoreFontScale,
            restored = {
                composeRule.activity.resources.configuration.fontScale
                    .isCloseTo(originalConfiguration.fontScale)
            },
        )
    }

    @RequiresApi(31)
    private fun fontWeightChange(originalConfiguration: Configuration): ConfigurationChange {
        val targetFontWeightAdjustment = if (originalConfiguration.fontWeightAdjustment == 300) {
            0
        } else {
            300
        }
        return ConfigurationChange(
            name = "fontWeight",
            change = {
                displaySettings.setFontWeightAdjustment(targetFontWeightAdjustment)
            },
            changed = {
                composeRule.activity.resources.configuration.fontWeightAdjustment ==
                    targetFontWeightAdjustment
            },
            restore = displaySettings::restoreFontWeightAdjustment,
            restored = {
                composeRule.activity.resources.configuration.fontWeightAdjustment ==
                    originalConfiguration.fontWeightAdjustment
            },
        )
    }

    private fun densityChange(originalConfiguration: Configuration): ConfigurationChange = ConfigurationChange(
        name = "density",
        change = { displaySettings.setDensity(displaySettings.changedDensity) },
        changed = {
            composeRule.activity.resources.configuration.densityDpi ==
                displaySettings.changedDensity
        },
        restore = displaySettings::restoreDensity,
        restored = {
            composeRule.activity.resources.configuration.densityDpi ==
                originalConfiguration.densityDpi
        },
    )

    private data class ConfigurationChange(
        val name: String,
        val change: () -> Unit,
        val changed: () -> Boolean,
        val restore: () -> Unit,
        val restored: () -> Boolean,
    )

    private fun launchWebView(initialUrl: String): AtomicReference<PageState> {
        val pageState = AtomicReference(PageState())
        sessionViewModel = SessionViewModel(Route.Session(CONFIGURATION_TEST_SESSION_ID))

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val controller = rememberSessionWebViewController()
                val dialogState = rememberSessionWebDialogState()

                SessionWebView(
                    url = initialUrl,
                    controller = controller,
                    dialogState = dialogState,
                    callbacks = SessionWebViewCallbacks(
                        onPageFinished = { pageInfo ->
                            pageState.set(PageState(url = pageInfo.url))
                            sessionViewModel.dispatch(
                                SessionUiEvent.PageLoaded(
                                    sessionId = CONFIGURATION_TEST_SESSION_ID,
                                    url = pageInfo.url,
                                    canGoBack = pageInfo.canGoBack,
                                    canGoForward = pageInfo.canGoForward,
                                ),
                            )
                        },
                        onRootScrolled = { metrics ->
                            sessionViewModel.dispatch(
                                SessionUiEvent.WebViewRootScrolled(
                                    deltaY = metrics.deltaY,
                                    scrollableDistance = metrics.scrollableDistance,
                                    viewportHeight = metrics.viewportHeight,
                                    touchSlopPx = metrics.touchSlopPx,
                                ),
                            )
                        },
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
                SessionWebDialogHosts(state = dialogState)
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

    private fun restoreOrientation() {
        val requestedOrientation = if (originalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = requestedOrientation
        }
    }

    @RequiresApi(31)
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

        nightModeChanged = true
        uiModeManager.setApplicationNightMode(applicationNightMode)
        composeRule.waitUntil(timeoutMillis = CONFIGURATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == targetNightMode
        }
    }

    private fun assertContinuity(
        activityIdentity: Int,
        expectedWebViewState: WebViewState,
        expectedBottomSheetState: BottomSheetState,
        expectedTopBarState: SessionTopBarState,
    ) {
        assertEquals(activityIdentity, System.identityHashCode(composeRule.activity))
        val currentWebViewState = currentWebViewState()
        assertEquals(
            expectedWebViewState.copy(scrollY = currentWebViewState.scrollY),
            currentWebViewState,
        )
        assertTrue(currentWebViewState.scrollY > 0)
        // 구성 변경 뒤 scrollY가 0으로 초기화되지 않았는지 확인합니다.
        // 변경 전 값과의 차이는 25%까지 허용합니다.
        assertTrue(
            kotlin.math.abs(currentWebViewState.scrollY - expectedWebViewState.scrollY) <=
                expectedWebViewState.scrollY * MAX_SCROLL_POSITION_CHANGE_RATIO,
        )
        assertDraftText("kept")
        assertEquals(expectedBottomSheetState, sessionViewModel.state.value.bottomSheetState)
        assertEquals(expectedTopBarState, sessionViewModel.state.value.topBarState)
    }

    private fun verifyConfigurationRoundTrip(
        activityIdentity: Int,
        expectedWebViewState: WebViewState,
        scenario: ConfigurationChange,
    ) {
        setBrowsingChromeHidden()
        verifyConfigurationPhases(
            name = scenario.name,
            change = {
                scenario.change()
                waitForConfiguration(scenario.changed)
                assertContinuity(
                    activityIdentity = activityIdentity,
                    expectedWebViewState = expectedWebViewState,
                    expectedBottomSheetState = BottomSheetState.HIDDEN,
                    expectedTopBarState = SessionTopBarState.FOLDED,
                )
            },
            restore = {
                try {
                    setBrowsingChromeMinimized()
                } finally {
                    scenario.restore()
                }
                waitForConfiguration(scenario.restored)
                assertContinuity(
                    activityIdentity = activityIdentity,
                    expectedWebViewState = expectedWebViewState,
                    expectedBottomSheetState = BottomSheetState.MINIMIZED,
                    expectedTopBarState = SessionTopBarState.UNFOLDED,
                )
            },
        )
    }

    private fun verifyDialogConfigurationRoundTrip(
        activityIdentity: Int,
        webView: WebView,
        scenario: ConfigurationChange,
    ) {
        verifyConfigurationPhases(
            name = scenario.name,
            change = {
                scenario.change()
                waitForConfiguration(scenario.changed)
                assertDialogContinuity(activityIdentity, webView)
            },
            restore = {
                scenario.restore()
                waitForConfiguration(scenario.restored)
                assertDialogContinuity(activityIdentity, webView)
            },
        )
    }

    private fun verifyConfigurationPhases(
        name: String,
        change: () -> Unit,
        restore: () -> Unit,
    ) {
        val changeFailure = runCatching(change).exceptionOrNull()?.let {
            AssertionError("$name: change/verify failed", it)
        }
        // 복원도 실패하면 최초 실패를 덮지 않고 추가 예외로 함께 남깁니다.
        val restoreFailure = runCatching(restore).exceptionOrNull()?.let {
            AssertionError("$name: restore/verify failed", it)
        }
        if (changeFailure != null) {
            restoreFailure?.let(changeFailure::addSuppressed)
            throw changeFailure
        }
        if (restoreFailure != null) throw restoreFailure
    }

    private fun assertDialogContinuity(
        activityIdentity: Int,
        webView: WebView,
    ) {
        assertEquals(activityIdentity, System.identityHashCode(composeRule.activity))
        composeRule.runOnIdle {
            assertTrue(webView.isAttachedToWindow)
        }
        assertDialogExists()
        composeRule.onNode(hasSetTextAction()).assertTextEquals("Retained input")
        assertEquals("pending", currentWebViewTitle(webView))
    }

    private fun assertDialogExists() {
        composeRule.onNodeWithText("Enter information").fetchSemanticsNode()
        composeRule.onNodeWithText("Configuration prompt").fetchSemanticsNode()
        composeRule.onNodeWithText("Confirm").fetchSemanticsNode()
    }

    private fun assertDialogDisplayed() {
        composeRule.onNodeWithText("Enter information").assertIsDisplayed()
        composeRule.onNodeWithText("Configuration prompt").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").assertIsDisplayed()
    }

    private fun waitForConfiguration(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = CONFIGURATION_TIMEOUT_MILLIS, condition = condition)
    }

    private fun verifyUserRootScrollChromeTransitions() {
        setBrowsingChromeMinimized()

        swipeWebView(startRatio = 0.8f, endRatio = 0.2f)
        composeRule.waitUntil(timeoutMillis = ROOT_SCROLL_TIMEOUT_MILLIS) {
            sessionViewModel.state.value.bottomSheetState == BottomSheetState.HIDDEN
        }

        swipeWebView(startRatio = 0.2f, endRatio = 0.8f)
        composeRule.waitUntil(timeoutMillis = ROOT_SCROLL_TIMEOUT_MILLIS) {
            sessionViewModel.state.value.bottomSheetState == BottomSheetState.MINIMIZED
        }
    }

    private fun setBrowsingChromeHidden() {
        setBrowsingChromeMinimized()
        sessionViewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
    }

    private fun setBrowsingChromeMinimized() {
        val state = sessionViewModel.state.value
        if (state.bottomSheetState != BottomSheetState.MINIMIZED) {
            sessionViewModel.dispatch(
                SessionUiEvent.BottomSheetValueChanged(BottomSheetValue.MINIMIZED),
            )
        }
        if (sessionViewModel.state.value.topBarState == SessionTopBarState.FOLDED) {
            sessionViewModel.dispatch(SessionUiEvent.TopBarFoldClicked)
        }
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
                        scrollY = webView.scrollY,
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

    private fun startJavascript(script: String) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("start JavaScript") { webView ->
                webView.evaluateJavascript(script, null)
            },
        )
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("Configuration prompt")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun currentWebView(): WebView {
        val webView = AtomicReference<WebView>()
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("capture the current WebView") { currentWebView ->
                webView.set(currentWebView)
            },
        )
        return checkNotNull(webView.get())
    }

    private fun currentWebViewTitle(webView: WebView): String? {
        val title = AtomicReference<String?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            title.set(webView.title)
        }
        return title.get()
    }

    private fun scrollWebViewTo(scrollY: Int) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            webViewAction("set the WebView root scroll position") { webView ->
                webView.scrollTo(0, scrollY)
            },
        )
    }

    private fun stopFlingAndScrollWebViewTo(scrollY: Int) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

                override fun getDescription(): String = "stop fling and set WebView root scroll position"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val eventTime = SystemClock.uptimeMillis()
                    val x = view.width / 2f
                    val y = view.height / 2f
                    view.dispatchTouch(eventTime, eventTime, MotionEvent.ACTION_DOWN, x, y)
                    view.dispatchTouch(eventTime, eventTime, MotionEvent.ACTION_CANCEL, x, y)

                    // 취소 직후에도 남은 스크롤이 반영될 수 있어 좌표가 멈춘 뒤 기준 위치를 설정합니다.
                    var lastScrollY = view.scrollY
                    var lastChangeTime = SystemClock.uptimeMillis()
                    val deadline = lastChangeTime + ROOT_SCROLL_TIMEOUT_MILLIS
                    while (SystemClock.uptimeMillis() - lastChangeTime < ROOT_SCROLL_IDLE_MILLIS) {
                        assertTrue(
                            "Root scroll did not stop after touch cancellation",
                            SystemClock.uptimeMillis() < deadline,
                        )
                        uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                        if (view.scrollY != lastScrollY) {
                            lastScrollY = view.scrollY
                            lastChangeTime = SystemClock.uptimeMillis()
                        }
                    }
                    view.scrollTo(0, scrollY)
                }
            },
        )
    }

    private fun waitForScrollY(expectedScrollY: Int) {
        composeRule.waitUntil(timeoutMillis = PAGE_LOAD_TIMEOUT_MILLIS) {
            currentWebViewState().scrollY == expectedScrollY
        }
    }

    private fun swipeWebView(
        startRatio: Float,
        endRatio: Float,
    ) {
        onView(isAssignableFrom(WebView::class.java)).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

                override fun getDescription(): String = "swipe the WebView root content"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val downTime = SystemClock.uptimeMillis()
                    val x = view.width / 2f
                    val startY = view.height * startRatio
                    val endY = view.height * endRatio
                    view.dispatchTouch(downTime, downTime, MotionEvent.ACTION_DOWN, x, startY)
                    uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                    repeat(SWIPE_MOVE_COUNT) { index ->
                        val progress = (index + 1f) / SWIPE_MOVE_COUNT
                        val y = startY + ((endY - startY) * progress)
                        view.dispatchTouch(
                            downTime,
                            SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_MOVE,
                            x,
                            y,
                        )
                        uiController.loopMainThreadForAtLeast(SWIPE_MOVE_INTERVAL_MILLIS)
                    }
                    view.dispatchTouch(
                        downTime,
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP,
                        x,
                        endY,
                    )
                    uiController.loopMainThreadForAtLeast(ROOT_SCROLL_IDLE_MILLIS)
                }
            },
        )
    }

    private fun View.dispatchTouch(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        try {
            dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
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
        val scrollY: Int,
    )

    private companion object {
        const val PAGE_LOAD_TIMEOUT_MILLIS = 10_000L
        const val CONFIGURATION_TIMEOUT_MILLIS = 15_000L
        const val ROOT_SCROLL_TIMEOUT_MILLIS = 5_000L
        const val ROOT_SCROLL_IDLE_MILLIS = 200L
        const val SWIPE_MOVE_COUNT = 5
        const val SWIPE_MOVE_INTERVAL_MILLIS = 16L
        const val CONFIGURATION_TEST_SESSION_ID = "configuration-test"
        const val MAX_SCROLL_POSITION_CHANGE_RATIO = 0.25f
    }
}

private fun Float.isCloseTo(other: Float): Boolean = kotlin.math.abs(this - other) < 0.01f
