package com.laxotters.clipy.core.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.BuildConfig
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonSize
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.component.button.model.FooterButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
fun ClipyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    type: ButtonType = ButtonType.Primary,
    size: ButtonSize = ButtonSize.Medium,
) {
    if (BuildConfig.DEBUG) {
        check(size != ButtonSize.Small || type == ButtonType.Primary) {
            "Unsupported ClipyButton combination: " +
                "size=$size, type=$type. " +
                "Small supports Primary only."
        }
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(ButtonDefaults.height(size)),
        enabled = enabled,
        shape = ButtonDefaults.shape(size),
        colors = ButtonDefaults.colors(
            type = type,
            size = size,
        ),
        border = ButtonDefaults.border(type),
        contentPadding = ButtonDefaults.contentPadding,
    ) {
        Text(
            text = text,
            style = ButtonDefaults.textStyle(size),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ClipyFooterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    type: FooterButtonType = FooterButtonType.Gradient,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dropShadow(
                    shape = FooterButtonDefaults.shape,
                    shadow = FooterButtonDefaults.shadow,
                )
                .clip(FooterButtonDefaults.shape)
                .background(
                    FooterButtonDefaults.background(
                        enabled = enabled,
                        type = type,
                    ),
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = FooterButtonDefaults.contentColor(enabled),
                style = ClipyTheme.typography.heading4,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(
    name = "Button",
    showBackground = true,
)
@Composable
private fun ClipyButtonPreview() {
    ClipyTheme {
        Column(
            modifier = Modifier
                .background(ClipyTheme.colors.neutral.gray50)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipyButton(text = "Primary", onClick = {})
                ClipyButton(text = "Disabled", onClick = {}, enabled = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipyButton(
                    text = "Secondary",
                    onClick = {},
                    type = ButtonType.Secondary,
                )
                ClipyButton(
                    text = "Disabled",
                    onClick = {},
                    enabled = false,
                    type = ButtonType.Secondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipyButton(
                    text = "Small",
                    onClick = {},
                    size = ButtonSize.Small,
                )
                ClipyButton(
                    text = "Disabled",
                    onClick = {},
                    enabled = false,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

@Preview(
    name = "Footer Button",
    showBackground = true,
)
@Composable
private fun ClipyFooterButtonPreview() {
    ClipyTheme {
        Column(Modifier.background(ClipyTheme.colors.neutral.gray50)) {
            ClipyFooterButton(
                text = "Gradient",
                onClick = {},
            )
            ClipyFooterButton(
                text = "Gradient disabled",
                onClick = {},
                enabled = false,
            )
            ClipyFooterButton(
                text = "Solid",
                onClick = {},
                type = FooterButtonType.Solid,
            )
            ClipyFooterButton(
                text = "Solid disabled",
                onClick = {},
                enabled = false,
                type = FooterButtonType.Solid,
            )
        }
    }
}
