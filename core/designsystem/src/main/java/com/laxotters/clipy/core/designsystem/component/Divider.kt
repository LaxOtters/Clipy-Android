@file:Suppress("MatchingDeclarationName")

package com.laxotters.clipy.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

object DividerDefaults {
    val largeVerticalPadding = 20.dp
    val mediumVerticalPadding = 10.dp
    val smallVerticalPadding = 5.dp
}

@Composable
fun ClipyDivider(
    modifier: Modifier = Modifier,
    verticalPadding: Dp = DividerDefaults.largeVerticalPadding,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(verticalPadding * 2),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(ClipyTheme.colors.neutral.gray100),
        )
    }
}

@Preview(
    name = "Divider",
    showBackground = true,
)
@Composable
private fun ClipyDividerPreview() {
    ClipyTheme {
        Column(Modifier.padding(horizontal = 20.dp)) {
            ClipyDivider(verticalPadding = DividerDefaults.largeVerticalPadding)
            ClipyDivider(verticalPadding = DividerDefaults.mediumVerticalPadding)
            ClipyDivider(verticalPadding = DividerDefaults.smallVerticalPadding)
        }
    }
}
