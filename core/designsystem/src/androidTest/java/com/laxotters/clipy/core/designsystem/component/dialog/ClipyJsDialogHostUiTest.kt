package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipyJsDialogHostUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun promptConfirm_invokesCallbackWithLatestValueAndHidesDialog() {
        var confirmedValue: String? = null

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipyJsDialogManager()
                LaunchedEffect(Unit) {
                    manager.show(
                        ClipyJsDialogRequest.Prompt(
                            source = "Request from example.com",
                            title = "Enter a name",
                            description = "This value is sent to the website.",
                            initialValue = "Clipy",
                            confirmAction = ClipyTextInputAction(
                                label = "Confirm",
                                onClick = { confirmedValue = it },
                            ),
                            cancelAction = ClipyTextAction(
                                label = "Cancel",
                                onClick = {},
                            ),
                        ),
                    )
                }
                ClipyJsDialogHost(manager = manager)
            }
        }

        composeTestRule.onNodeWithText("Clipy").performTextReplacement("Updated")
        composeTestRule.onNodeWithText("Confirm").performClick()

        composeTestRule.runOnIdle {
            assertEquals("Updated", confirmedValue)
        }
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule
                .onAllNodesWithText("Enter a name")
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }
}
