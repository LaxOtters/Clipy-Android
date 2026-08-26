package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laxotters.clipy.core.designsystem.component.error.ClipyErrorOverlay
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorOverlayUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun coveredContent_tapped_doesNotReceiveInput() {
        var coveredContentClickCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                Box(
                    modifier = Modifier
                        .width(390.dp)
                        .height(520.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { coveredContentClickCount++ }
                            .testTag("coveredContent"),
                    )
                    ClipyErrorOverlay(
                        title = "This page can't be opened",
                        description = "Please try again later.",
                        modifier = Modifier.testTag("errorOverlay"),
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("errorOverlay").performTouchInput {
            click(
                Offset(
                    x = 1f,
                    y = 1f,
                ),
            )
        }

        composeTestRule.runOnIdle {
            assertEquals(
                0,
                coveredContentClickCount,
            )
        }
    }

    @Test
    fun action_tapped_runsAction() {
        var actionCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyErrorOverlay(
                    title = "This page can't be opened",
                    description = "Please try again later.",
                    modifier = Modifier
                        .width(390.dp)
                        .height(520.dp),
                    action = ClipyTextAction(
                        label = "Go back",
                        onClick = { actionCount++ },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Go back").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                actionCount,
            )
        }
    }

    @Test
    fun longContent_scrolled_keepsActionReachable() {
        var actionCount = 0

        composeTestRule.setContent {
            ClipyTheme {
                ClipyErrorOverlay(
                    title = "This page can't be opened",
                    description = "The page may be temporarily unavailable. ".repeat(12),
                    modifier = Modifier
                        .width(390.dp)
                        .height(240.dp),
                    action = ClipyTextAction(
                        label = "Go back",
                        onClick = { actionCount++ },
                    ),
                )
            }
        }

        composeTestRule.onNode(hasScrollAction()).assertExists()
        composeTestRule.onNodeWithText("Go back")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                actionCount,
            )
        }
    }
}
