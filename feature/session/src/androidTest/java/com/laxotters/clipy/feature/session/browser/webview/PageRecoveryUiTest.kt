package com.laxotters.clipy.feature.session.browser.webview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.SessionPageRecovery
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction
import com.laxotters.clipy.feature.session.browser.recovery.SessionRecoveryOverlay
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageRecoveryUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<SessionConfigurationTestActivity>()

    @Test
    fun recoveryButton_acceptsTouchAndBlocksRepeatedTouchWhileRunning() {
        var isRunning by mutableStateOf(false)
        val actionClicks = AtomicInteger()
        composeRule.setContent {
            ClipyTheme {
                SessionRecoveryOverlay(
                    SessionPageRecovery(
                        NAVIGATION,
                        SessionPageErrorCause.NO_NETWORK,
                        SessionRecoveryAction.TRY_AGAIN,
                        true,
                        isRunning,
                    ),
                    {
                        actionClicks.incrementAndGet()
                        isRunning = true
                    },
                )
            }
        }
        val button = composeRule.onNodeWithText("Try again")
        val idle = buttonPixels()
        button.performTouchInput { down(center) }
        button.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(1f, 1f)) }
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitUntil(3_000) { !idle.contentEquals(buttonPixels()) }
        button.performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(1, actionClicks.get()) }
        composeRule.onNodeWithText("Try again").assertIsNotEnabled()
        composeRule.onNodeWithText("Try again").performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, actionClicks.get()) }
    }

    private fun buttonPixels(): IntArray {
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithText("Try again").fetchSemanticsNode().boundsInWindow
        val image = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val width = bounds.width.toInt()
        val height = bounds.height.toInt()
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, bounds.left.toInt(), bounds.top.toInt(), width, height)
        image.recycle()
        return pixels
    }

    private companion object {
        val NAVIGATION = SessionPageNavigation(1, 1, "https://example.com/failed")
    }
}
