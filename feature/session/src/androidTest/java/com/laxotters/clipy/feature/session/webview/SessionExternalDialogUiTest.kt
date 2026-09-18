package com.laxotters.clipy.feature.session.webview

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionExternalDialogUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun externalDialog_exposesOnlyButtonDismissAndCompletesSelectedActionOnce() {
        val continueCount = AtomicInteger()
        showDialog(continueCount)

        composeRule.onNodeWithText("Open external app?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertHasClickAction()
        composeRule.onNodeWithText("Continue").assertHasClickAction()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Open external app?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onAllNodesWithText("Open external app?").assertCountEquals(0)
        assertEquals(1, continueCount.get())
    }

    private fun showDialog(continueCount: AtomicInteger) {
        val owner = Any()
        composeRule.setContent {
            ClipyTheme {
                val state = rememberSessionWebDialogState()
                LaunchedEffect(state) {
                    state.externalActionManager.show(
                        owner = owner,
                        request = SessionExternalActionDialogRequest(
                            title = "Open external app?",
                            description = "Clipy will open another app to continue.",
                            continueLabel = "Continue",
                            cancelLabel = "Cancel",
                            onContinue = { continueCount.incrementAndGet() },
                        ),
                    )
                }
                SessionWebDialogHosts(state = state)
            }
        }
    }
}
