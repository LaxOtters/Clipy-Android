package com.laxotters.clipy.core.designsystem.component.dialog

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot as isViewRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsDialogUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var originalRequestedOrientation: Int? = null

    @After
    fun restoreOrientation() {
        originalRequestedOrientation?.let { orientation ->
            composeTestRule.activityRule.scenario.onActivity {
                it.requestedOrientation = orientation
            }
        }
    }

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

    @Test
    fun jsPrompt_landscapeScrollsTitleAndDescriptionOnly() {
        setScrollablePrompt(Configuration.ORIENTATION_LANDSCAPE)

        val title = composeTestRule.onNodeWithText("Enter a name")
        val titleLayouts = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
        assertTrue(
            "The landscape prompt must show the full title before scrolling",
            title.fetchSemanticsNode().boundsInRoot.height >= titleLayouts.single().size.height,
        )
        val originalTitlePosition = title.fetchSemanticsNode().positionInRoot
        val fixedNodes = listOf("Request from example.com", "Clipy", "Cancel", "OK")
            .map { composeTestRule.onNodeWithText(it) }
        val originalPositions = fixedNodes.map { it.fetchSemanticsNode().positionInRoot }

        composeTestRule.onNode(hasScrollAction())
            .assert(hasAnyDescendant(hasText("Enter a name")))
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100f) }

        assertTrue(title.fetchSemanticsNode().positionInRoot.y < originalTitlePosition.y)
        fixedNodes.forEachIndexed { index, node ->
            node.assertIsDisplayed()
            assertEquals(originalPositions[index], node.fetchSemanticsNode().positionInRoot)
        }
    }

    @Test
    fun jsPrompt_portraitScrollsDescriptionAndKeepsTitleFixed() {
        setScrollablePrompt(Configuration.ORIENTATION_PORTRAIT)

        val title = composeTestRule.onNodeWithText("Enter a name")
        val originalTitlePosition = title.fetchSemanticsNode().positionInRoot
        composeTestRule.onNode(hasScrollAction())
            .assert(hasText("Long description\n".repeat(40)))
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100f) }

        title.assertIsDisplayed()
        assertEquals(originalTitlePosition, title.fetchSemanticsNode().positionInRoot)
    }

    private fun setScrollablePrompt(orientation: Int) {
        composeTestRule.activityRule.scenario.onActivity {
            originalRequestedOrientation = it.requestedOrientation
            it.requestedOrientation = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.activity.resources.configuration.orientation == orientation
        }
        composeTestRule.setContent {
            ClipyTheme {
                ClipyJsDialog(
                    source = "Request from example.com",
                    state = JsDialogState.Prompt(
                        title = "Enter a name",
                        description = "Long description\n".repeat(40),
                        confirmAction = ClipyTextInputAction(label = "OK", onClick = {}),
                        initialValue = "Clipy",
                        cancelAction = ClipyTextAction(label = "Cancel", onClick = {}),
                    ),
                    modifier = Modifier.height(320.dp),
                )
            }
        }
        onView(isViewRoot()).inRoot(isDialog()).perform(closeSoftKeyboard())
    }
}
