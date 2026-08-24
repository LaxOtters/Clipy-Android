package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarHost
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarManager
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
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

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(
                        message = "Could not connect.",
                        action = ClipyTextAction(
                            label = "Retry",
                            onClick = { actionCount++ },
                        ),
                    )
                }
                ClipySnackbarHost(manager = manager)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                actionCount,
            )
        }
    }

    @Test
    fun snackbarAction_hasAtLeast48DpTouchTarget() {
        val actionLabel = "Retry"

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(
                        message = "Could not connect.",
                        action = ClipyTextAction(
                            label = actionLabel,
                            onClick = {},
                        ),
                    )
                }
                ClipySnackbarHost(manager = manager)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(actionLabel).fetchSemanticsNodes().isNotEmpty()
        }
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

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(
                        message = message,
                        action = ClipyTextAction(
                            label = actionLabel,
                            onClick = {},
                        ),
                    )
                }
                Box(modifier = Modifier.width(390.dp)) {
                    ClipySnackbarHost(manager = manager)
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(actionLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        val messageBounds = composeTestRule.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeTestRule.onNodeWithText(actionLabel).fetchSemanticsNode().boundsInRoot

        assertTrue(actionBounds.top >= messageBounds.bottom)
    }

    @Test
    fun snackbarTwoLineMessageWithinWidth_keepsActionInline() {
        val message = "text\ntext"
        val actionLabel = "Action"

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(
                        message = message,
                        action = ClipyTextAction(
                            label = actionLabel,
                            onClick = {},
                        ),
                    )
                }
                Box(modifier = Modifier.width(390.dp)) {
                    ClipySnackbarHost(manager = manager)
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(actionLabel).fetchSemanticsNodes().isNotEmpty()
        }
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
                    val manager = rememberClipySnackbarManager()
                    LaunchedEffect(Unit) {
                        manager.showSnackbar(
                            message = message,
                            action = ClipyTextAction(
                                label = actionLabel,
                                onClick = {},
                            ),
                        )
                    }
                    Box(modifier = Modifier.width(390.dp)) {
                        ClipySnackbarHost(manager = manager)
                    }
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(actionLabel).fetchSemanticsNodes().isNotEmpty()
        }
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
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(message = message)
                }
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { backgroundClickCount++ },
                    )
                    ClipySnackbarHost(
                        manager = manager,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("snackbar-host"),
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        val hostBounds = composeTestRule.onNodeWithTag("snackbar-host").fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag("snackbar-host").performTouchInput {
            click(
                Offset(
                    x = hostBounds.width / 2f,
                    y = hostBounds.height - 1f,
                ),
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.runOnIdle {
            assertEquals(
                1,
                backgroundClickCount,
            )
        }
    }

    @Test
    fun dismissingCurrentSnackbar_keepsNextSnackbarInteractive() {
        val firstMessage = "First snackbar"
        val secondMessage = "Second snackbar"
        var backgroundClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(message = firstMessage)
                    manager.showSnackbar(message = secondMessage)
                }
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { backgroundClickCount++ },
                    )
                    ClipySnackbarHost(
                        manager = manager,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("snackbar-host"),
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(firstMessage).fetchSemanticsNodes().isNotEmpty()
        }
        val hostBounds = composeTestRule.onNodeWithTag("snackbar-host").fetchSemanticsNode().boundsInRoot
        composeTestRule.onNodeWithTag("snackbar-host").performTouchInput {
            click(
                Offset(
                    x = hostBounds.width / 2f,
                    y = hostBounds.height - 1f,
                ),
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(secondMessage).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("snackbar-host").performTouchInput {
            click(
                Offset(
                    x = hostBounds.width / 2f,
                    y = hostBounds.height - 1f,
                ),
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 1_000) {
            composeTestRule.onAllNodesWithText(secondMessage).fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.runOnIdle {
            assertEquals(
                2,
                backgroundClickCount,
            )
        }
    }

    @Test
    fun snackbar_dismissesAfterTwoSeconds() {
        val message = "Timed snackbar"

        composeTestRule.setContent {
            ClipyTheme {
                val manager = rememberClipySnackbarManager()
                LaunchedEffect(Unit) {
                    manager.showSnackbar(message = message)
                }
                ClipySnackbarHost(manager = manager)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        val displayedAt = System.currentTimeMillis()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isEmpty()
        }

        assertTrue(System.currentTimeMillis() - displayedAt >= 1_800)
    }
}
