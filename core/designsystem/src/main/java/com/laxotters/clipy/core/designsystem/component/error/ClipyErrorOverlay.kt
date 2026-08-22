package com.laxotters.clipy.core.designsystem.component.error

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

private val ErrorOverlayHorizontalPadding = 20.dp
private val ErrorOverlaySpacing = 30.dp
private val ErrorTextSpacing = 10.dp
private val ErrorIconSize = 70.dp

/**
 * 호출자가 전달한 영역을 채우고 action 외 입력을 소비하는 오류 오버레이입니다.
 */
@Composable
fun ClipyErrorOverlay(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: ClipyTextAction? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClipyTheme.colors.primary.indigo50)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final)
                            .changes
                            .forEach { it.consume() }
                    }
                }
            }
            .padding(horizontal = ErrorOverlayHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ErrorOverlaySpacing),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_circle_exclamation),
                contentDescription = null,
                modifier = Modifier.size(ErrorIconSize),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ErrorTextSpacing),
            ) {
                Text(
                    text = title,
                    color = ClipyTheme.colors.neutral.gray950,
                    style = ClipyTheme.typography.heading3,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    color = ClipyTheme.colors.neutral.gray950,
                    style = ClipyTheme.typography.body1Regular,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                ClipyButton(
                    text = action.label,
                    onClick = action.onClick,
                    type = ButtonType.Secondary,
                )
            }
        }
    }
}

@Preview(
    name = "Error overlay · No action",
    showBackground = true,
    widthDp = 390,
    heightDp = 520,
)
@Composable
private fun ClipyErrorOverlayNoActionPreview() {
    ClipyTheme {
        ClipyErrorOverlay(
            title = "This page can't be opened",
            description = "Please try again later.",
        )
    }
}

@Preview(
    name = "Error overlay · Action",
    showBackground = true,
    widthDp = 390,
    heightDp = 520,
)
@Composable
private fun ClipyErrorOverlayActionPreview() {
    ClipyTheme {
        ClipyErrorOverlay(
            title = "This page can't be opened",
            description = "The page may be unavailable\nPlease try again later.",
            action = ClipyTextAction(
                label = "Go back",
                onClick = {},
            ),
        )
    }
}
