package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.R
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonSize
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
fun ClipyTopbar(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = ClipyTheme.colors.neutral.gray900,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopbarDefaults.height)
            .padding(horizontal = TopbarDefaults.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.let { content ->
            content()
            Spacer(modifier = Modifier.width(TopbarDefaults.componentSpacing))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = ClipyTheme.typography.heading2,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingContent?.let { content ->
            Spacer(modifier = Modifier.width(TopbarDefaults.componentSpacing))
            content()
        }
    }
}

@Preview(
    name = "Top bar - title only",
    showBackground = true,
)
@Composable
private fun ClipyTopbarTitleOnlyPreview() {
    ClipyTheme {
        ClipyTopbar(
            title = "Clipy",
            titleColor = ClipyTheme.colors.primary.indigo500,
        )
    }
}

@Preview(
    name = "Top bar - close",
    showBackground = true,
)
@Composable
private fun ClipyTopbarWithClosePreview() {
    ClipyTheme {
        ClipyTopbar(
            title = "Item",
            leadingContent = {
                CloseTopbarButton(onClick = {})
            },
        )
    }
}

@Preview(
    name = "Top bar - close and save",
    showBackground = true,
)
@Composable
private fun ClipyTopbarWithCloseAndSavePreview() {
    ClipyTheme {
        ClipyTopbar(
            title = "Item",
            leadingContent = {
                CloseTopbarButton(onClick = {})
            },
            trailingContent = {
                ClipyButton(
                    text = "Save",
                    onClick = {},
                    size = ButtonSize.Small,
                )
            },
        )
    }
}

@Composable
private fun CloseTopbarButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_x_mark),
            contentDescription = "Close",
            modifier = Modifier.size(14.dp),
            tint = ClipyTheme.colors.neutral.gray600,
        )
    }
}

private object TopbarDefaults {
    val height = 60.dp
    val horizontalPadding = 20.dp
    val componentSpacing = 16.dp
}
