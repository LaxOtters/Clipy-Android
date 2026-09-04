package com.laxotters.clipy.core.designsystem.component.dialog

import android.view.KeyEvent
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun jsAlert_displaysSourceAndInvokesConfirm() {
        var confirmCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Alert(
                        title = "Notice",
                        description = "The website sent a message.",
                        confirmAction = ClipyTextAction(
                            label = "OK",
                            onClick = { confirmCount++ },
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Request from example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, confirmCount)
        }
    }

    @Test
    fun jsDialog_backAndOutsideTapDoNotDismiss() {
        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Alert(
                        title = "Notice",
                        description = "The website sent a message.",
                        confirmAction = ClipyTextAction(
                            label = "OK",
                            onClick = {},
                        ),
                    ),
                    modifier = Modifier.testTag("java-script-dialog-content"),
                )
            }
        }

        composeTestRule.onNodeWithText("Notice").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeTestRule.onNode(
            isRoot() and hasAnyDescendant(hasTestTag("java-script-dialog-content")),
        ).performTouchInput {
            click(Offset(x = 1f, y = 1f))
        }

        composeTestRule.onNodeWithText("Notice").assertIsDisplayed()
    }

    @Test
    fun jsConfirm_displaysAndInvokesBothActions() {
        var cancelCount = 0
        var confirmCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Confirm(
                        title = "Continue?",
                        description = "Please confirm the request.",
                        confirmAction = ClipyTextAction(
                            label = "OK",
                            onClick = { confirmCount++ },
                        ),
                        cancelAction = ClipyTextAction(
                            label = "Cancel",
                            onClick = { cancelCount++ },
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, cancelCount)
            assertEquals(1, confirmCount)
        }
    }

    @Test
    fun jsPrompt_confirmsLatestInputValue() {
        var confirmedValue: String? = null

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Prompt(
                        title = "Enter a name",
                        description = "This value is sent to the website.",
                        confirmAction = ClipyTextInputAction(
                            label = "OK",
                            onClick = { confirmedValue = it },
                        ),
                        initialValue = "Clipy",
                        cancelAction = ClipyTextAction(
                            label = "Cancel",
                            onClick = {},
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Clipy").performTextReplacement("Updated")
        composeTestRule.onNodeWithText("Updated").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()

        composeTestRule.runOnIdle {
            assertEquals("Updated", confirmedValue)
        }
    }

    @Test
    fun jsPrompt_longContentScrollsAndKeepsActionsVisible() {
        var confirmedValue: String? = null

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Prompt(
                        title = "Enter a name",
                        description = "Long description\n".repeat(40),
                        confirmAction = ClipyTextInputAction(
                            label = "OK",
                            onClick = { confirmedValue = it },
                        ),
                        initialValue = "Clipy",
                        cancelAction = ClipyTextAction(
                            label = "Cancel",
                            onClick = {},
                        ),
                    ),
                    modifier = Modifier.height(320.dp),
                )
            }
        }

        composeTestRule.onNode(hasScrollAction()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Clipy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()

        composeTestRule.runOnIdle {
            assertEquals("Clipy", confirmedValue)
        }
    }
}
