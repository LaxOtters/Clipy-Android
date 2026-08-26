package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarController
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarLayout
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnackbarUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun snackbarAction_performsAction() {
        var actionCount = 0
        var backgroundClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp),
                ) {
                    SnackbarRequest(
                        message = "Could not connect.",
                        action = ClipyTextAction(
                            label = "Retry",
                            onClick = { actionCount++ },
                        ),
                    )
                    ClickableBackground { backgroundClickCount++ }
                }
            }
        }

        waitForSnackbar("Retry")
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                actionCount,
            )
            assertEquals(
                0,
                backgroundClickCount,
            )
        }
    }

    @Test
    fun snackbarController_requestIsDisplayedBySharedLayout() {
        val message = "Shared snackbar"

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    SnackbarRequest(message = message)
                }
            }
        }

        waitForSnackbar(message)
    }

    @Test
    fun snackbarAction_hasAtLeast48DpTouchTarget() {
        val actionLabel = "Retry"

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    SnackbarRequest(
                        message = "Could not connect.",
                        action = ClipyTextAction(
                            label = actionLabel,
                            onClick = {},
                        ),
                    )
                }
            }
        }

        waitForSnackbar(actionLabel)
        composeTestRule.waitForIdle()

        val actionBounds = composeTestRule
            .onNodeWithText(actionLabel)
            .fetchSemanticsNode()
            .boundsInRoot
        val minimumTouchTarget = with(composeTestRule.density) {
            48.dp.toPx()
        }

        assertTrue(actionBounds.height >= minimumTouchTarget)
    }

    @Test
    fun snackbarOverflow_stacksActionBelowMessage() {
        val message = "Long text Snackbar Example\nLong text Snackbar Example"
        val actionLabel = "Long Action Example"

        setSnackbarContent(
            width = 390,
            message = message,
            action = ClipyTextAction(
                label = actionLabel,
                onClick = {},
            ),
        )
        waitForSnackbar(actionLabel)
        composeTestRule.waitForIdle()

        val messageBounds = composeTestRule.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeTestRule.onNodeWithText(actionLabel).fetchSemanticsNode().boundsInRoot

        assertTrue(actionBounds.top >= messageBounds.bottom)
    }

    @Test
    fun snackbarTwoLineMessageWithinWidth_keepsActionInline() {
        val message = "text\ntext"
        val actionLabel = "Action"

        setSnackbarContent(
            width = 390,
            message = message,
            action = ClipyTextAction(
                label = actionLabel,
                onClick = {},
            ),
        )
        waitForSnackbar(actionLabel)
        composeTestRule.waitForIdle()

        val messageBounds = composeTestRule.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeTestRule.onNodeWithText(actionLabel).fetchSemanticsNode().boundsInRoot

        assertTrue(actionBounds.top < messageBounds.bottom)
        assertTrue(actionBounds.bottom > messageBounds.top)
    }

    @Test
    fun snackbarLargeFont_whenCombinedWidthOverflows_stacksAction() {
        val message = "Network connection failed."
        val actionLabel = "Try again"

        composeTestRule.setContent {
            ClipyTheme {
                val currentDensity = LocalDensity.current

                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = currentDensity.density,
                        fontScale = 1.5f,
                    ),
                ) {
                    ClipySnackbarLayout(modifier = Modifier.width(390.dp)) {
                        SnackbarRequest(
                            message = message,
                            action = ClipyTextAction(
                                label = actionLabel,
                                onClick = {},
                            ),
                        )
                    }
                }
            }
        }

        waitForSnackbar(actionLabel)
        composeTestRule.waitForIdle()

        val messageBounds = composeTestRule.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeTestRule.onNodeWithText(actionLabel).fetchSemanticsNode().boundsInRoot

        assertTrue(actionBounds.top >= messageBounds.bottom)
    }

    @Test
    fun snackbar_dismissesWhenOtherAreaIsTouched() {
        val message = "Item saved"
        var backgroundClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                        .testTag(SNACKBAR_LAYOUT_TAG),
                ) {
                    SnackbarRequest(message = message)
                    ClickableBackground { backgroundClickCount++ }
                }
            }
        }

        waitForSnackbar(message)
        clickSnackbarOutside()
        waitForSnackbarToDisappear(message)

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                backgroundClickCount,
            )
        }
    }

    @Test
    fun snackbarMessageTouch_doesNotReachBackgroundOrDismiss() {
        val message = "Item saved"
        var backgroundClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp),
                ) {
                    SnackbarRequest(message = message)
                    ClickableBackground { backgroundClickCount++ }
                }
            }
        }

        waitForSnackbar(message)
        composeTestRule.onNodeWithText(message).performTouchInput { click() }

        composeTestRule.runOnIdle {
            assertEquals(
                0,
                backgroundClickCount,
            )
        }
        composeTestRule.onNodeWithText(message).fetchSemanticsNode()
    }

    @Test
    fun dismissingCurrentSnackbar_keepsNextSnackbarInteractive() {
        val firstMessage = "First snackbar"
        val secondMessage = "Second snackbar"
        var backgroundClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                        .testTag(SNACKBAR_LAYOUT_TAG),
                ) {
                    val controller = rememberClipySnackbarController()

                    LaunchedEffect(controller) {
                        launch {
                            controller.showSnackbar(message = firstMessage)
                        }
                        launch {
                            controller.showSnackbar(message = secondMessage)
                        }
                    }
                    ClickableBackground { backgroundClickCount++ }
                }
            }
        }

        waitForSnackbar(firstMessage)
        clickSnackbarOutside()
        waitForSnackbar(secondMessage)
        clickSnackbarOutside()
        waitForSnackbarToDisappear(secondMessage)

        composeTestRule.runOnIdle {
            assertEquals(
                2,
                backgroundClickCount,
            )
        }
    }

    @Test
    fun routeContentRemoval_cancelsItsSnackbarRequest() {
        val message = "Route snackbar"
        var routeVisible by mutableStateOf(true)

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    if (routeVisible) {
                        SnackbarRequest(message = message)
                    }
                }
            }
        }

        waitForSnackbar(message)
        composeTestRule.runOnIdle {
            routeVisible = false
        }

        waitForSnackbarToDisappear(message)
    }

    @Test
    fun requestKeyChange_cancelsPreviousSnackbar() {
        val previousMessage = "Previous snackbar"
        val currentMessage = "Current snackbar"
        var message by mutableStateOf(previousMessage)

        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout {
                    SnackbarRequest(message = message)
                }
            }
        }

        waitForSnackbar(previousMessage)
        composeTestRule.runOnIdle {
            message = currentMessage
        }

        waitForSnackbarToDisappear(previousMessage)
        waitForSnackbar(currentMessage)
    }

    @Test
    fun snackbar_dismissesAfterTwoSeconds() {
        val message = "Timed snackbar"

        setSnackbarContent(
            width = 390,
            message = message,
        )
        waitForSnackbar(message)
        val displayedAt = System.currentTimeMillis()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(System.currentTimeMillis() - displayedAt >= 1_800)
    }

    private fun setSnackbarContent(
        width: Int,
        message: String,
        action: ClipyTextAction? = null,
    ) {
        composeTestRule.setContent {
            ClipyTheme {
                ClipySnackbarLayout(modifier = Modifier.width(width.dp)) {
                    SnackbarRequest(
                        message = message,
                        action = action,
                    )
                }
            }
        }
    }

    private fun waitForSnackbar(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForSnackbarToDisappear(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun clickSnackbarOutside() {
        val layoutBounds = composeTestRule
            .onNodeWithTag(SNACKBAR_LAYOUT_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        composeTestRule.onNodeWithTag(SNACKBAR_LAYOUT_TAG).performTouchInput {
            click(
                Offset(
                    x = layoutBounds.width / 2f,
                    y = layoutBounds.height - 1f,
                ),
            )
        }
    }

    private companion object {
        const val SNACKBAR_LAYOUT_TAG = "snackbar-layout"
    }
}

@Composable
private fun SnackbarRequest(
    message: String,
    action: ClipyTextAction? = null,
) {
    val controller = rememberClipySnackbarController()

    ShowSnackbarRequest(
        controller = controller,
        message = message,
        action = action,
    )
}

@Composable
private fun ShowSnackbarRequest(
    controller: ClipySnackbarController,
    message: String,
    action: ClipyTextAction? = null,
) {
    LaunchedEffect(
        controller,
        message,
        action,
    ) {
        controller.showSnackbar(
            message = message,
            action = action,
        )
    }
}

@Composable
private fun ClickableBackground(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
    )
}
