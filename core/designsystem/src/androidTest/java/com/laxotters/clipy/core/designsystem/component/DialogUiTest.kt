package com.laxotters.clipy.core.designsystem.component

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
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyDialogStyle
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyDualDialog
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialog
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogState
import com.laxotters.clipy.core.designsystem.component.dialog.ClipySingleDialog
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
    fun clipyDialog_backAndOutsideTapDoNotDismiss() {
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
        pressBackUnconditionally()
        composeTestRule.onNode(
            isRoot() and hasAnyDescendant(hasTestTag("dialog-content")),
        ).performTouchInput {
            click(
                Offset(
                    x = 1f,
                    y = 1f,
                ),
            )
        }

        composeTestRule.onNodeWithText("Connection failed").assertIsDisplayed()
    }

    @Test
    fun clipyDialog_twoActionsInvokeTheirCallbacks() {
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
            assertEquals(
                1,
                cancelCount,
            )
            assertEquals(
                1,
                confirmCount,
            )
        }
    }

    @Test
    fun clipyErrorDialog_supportsTwoActions() {
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
                    style = ClipyDialogStyle.Error,
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                cancelCount,
            )
            assertEquals(
                1,
                retryCount,
            )
        }
    }

    @Test
    fun jsAlert_displaysSourceAndInvokesConfirm() {
        var confirmCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = ClipyJsDialogState.Alert(
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
            assertEquals(
                1,
                confirmCount,
            )
        }
    }

    @Test
    fun jsDialog_backAndOutsideTapDoNotDismiss() {
        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = ClipyJsDialogState.Alert(
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
        pressBackUnconditionally()
        composeTestRule.onNode(
            isRoot() and hasAnyDescendant(hasTestTag("java-script-dialog-content")),
        ).performTouchInput {
            click(
                Offset(
                    x = 1f,
                    y = 1f,
                ),
            )
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
                    state = ClipyJsDialogState.Confirm(
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
            assertEquals(
                1,
                cancelCount,
            )
            assertEquals(
                1,
                confirmCount,
            )
        }
    }

    @Test
    fun jsPrompt_confirmsLatestInputValue() {
        var confirmedValue: String? = null

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = ClipyJsDialogState.Prompt(
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
            assertEquals(
                "Updated",
                confirmedValue,
            )
        }
    }

    @Test
    fun clipyDialog_longDescriptionScrollsAndKeepsActionVisible() {
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
            assertEquals(
                1,
                confirmCount,
            )
        }
    }

    @Test
    fun jsPrompt_longContentScrollsAndKeepsActionsVisible() {
        var confirmedValue: String? = null

        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = ClipyJsDialogState.Prompt(
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
            assertEquals(
                "Clipy",
                confirmedValue,
            )
        }
    }
}
