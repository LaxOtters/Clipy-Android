package com.laxotters.clipy.core.designsystem.component.dialog

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_backAndOutsideTapDoNotDismiss() {
        composeTestRule.setContent {
            ClipyTheme {
                ClipySingleDialog(
                    title = "Connection failed",
                    description = "Please try again later.",
                    primaryAction = ClipyTextAction(
                        label = "Confirm",
                        onClick = {},
                    ),
                    modifier = Modifier.testTag("dialog-content"),
                )
            }
        }

        composeTestRule.onNodeWithText("Connection failed").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.onNode(
            isRoot() and hasAnyDescendant(hasTestTag("dialog-content")),
        ).performTouchInput {
            click(Offset(x = 1f, y = 1f))
        }

        composeTestRule.onNodeWithText("Connection failed").assertIsDisplayed()
    }

    @Test
    fun dialog_twoActionsInvokeTheirCallbacks() {
        var cancelCount = 0
        var confirmCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyDualDialog(
                    title = "Delete item?",
                    description = "This cannot be undone.",
                    primaryAction = ClipyTextAction(
                        label = "Delete",
                        onClick = { confirmCount++ },
                    ),
                    secondaryAction = ClipyTextAction(
                        label = "Cancel",
                        onClick = { cancelCount++ },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, cancelCount)
            assertEquals(1, confirmCount)
        }
    }

    @Test
    fun errorDialog_supportsTwoActions() {
        var cancelCount = 0
        var retryCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyDualDialog(
                    title = "Connection failed",
                    description = "Please try again later.",
                    primaryAction = ClipyTextAction(
                        label = "Retry",
                        onClick = { retryCount++ },
                    ),
                    secondaryAction = ClipyTextAction(
                        label = "Cancel",
                        onClick = { cancelCount++ },
                    ),
                    style = DialogStyle.Error,
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, cancelCount)
            assertEquals(1, retryCount)
        }
    }

    @Test
    fun dialog_longDescriptionScrollsAndKeepsActionVisible() {
        var confirmCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipySingleDialog(
                    title = "Terms",
                    description = "Long description\n".repeat(40),
                    primaryAction = ClipyTextAction(
                        label = "Confirm",
                        onClick = { confirmCount++ },
                    ),
                )
            }
        }

        composeTestRule.onNode(hasScrollAction()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed().performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, confirmCount)
        }
    }
}
