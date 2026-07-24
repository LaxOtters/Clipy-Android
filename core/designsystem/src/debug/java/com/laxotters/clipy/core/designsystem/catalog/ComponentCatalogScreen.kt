package com.laxotters.clipy.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.component.ClipyDivider
import com.laxotters.clipy.core.designsystem.component.DividerDefaults
import com.laxotters.clipy.core.designsystem.component.actionmenu.ClipyActionMenu
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuIcon
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuItem
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuItemType
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuPlacement
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.ClipyFooterButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonSize
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonType
import com.laxotters.clipy.core.designsystem.component.button.model.FooterButtonType
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
fun ComponentCatalogScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClipyTheme.colors.neutral.gray50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            text = "Clipy Component Catalog",
            color = ClipyTheme.colors.neutral.gray950,
            style = ClipyTheme.typography.heading1,
        )
        ButtonSection()
        FooterButtonSection()
        DividerSection()
        ActionMenuSection()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ButtonSection() {
    CatalogSection(title = "Button") {
        ButtonType.entries.forEach { type ->
            CatalogStateLabel("${type.name} · Medium")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(true, false).forEach { enabled ->
                    ClipyButton(
                        text = if (enabled) "Enabled" else "Disabled",
                        onClick = {},
                        enabled = enabled,
                        type = type,
                        size = ButtonSize.Medium,
                    )
                }
            }
        }
        CatalogStateLabel("Primary · Small")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true, false).forEach { enabled ->
                ClipyButton(
                    text = if (enabled) "Enabled" else "Disabled",
                    onClick = {},
                    enabled = enabled,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun FooterButtonSection() {
    CatalogSection(title = "Footer Button") {
        FooterButtonType.entries.forEach { type ->
            listOf(true, false).forEach { enabled ->
                CatalogStateLabel("${type.name} · ${if (enabled) "Enabled" else "Disabled"}")
                ClipyFooterButton(
                    text = "Confirm",
                    onClick = {},
                    enabled = enabled,
                    type = type,
                )
            }
        }
    }
}

@Composable
private fun DividerSection() {
    CatalogSection(title = "Divider") {
        listOf(
            "Large · 40dp" to DividerDefaults.largeVerticalPadding,
            "Medium · 20dp" to DividerDefaults.mediumVerticalPadding,
            "Small · 10dp" to DividerDefaults.smallVerticalPadding,
        ).forEach { (label, padding) ->
            CatalogStateLabel(label)
            ClipyDivider(verticalPadding = padding)
        }
    }
}

@Composable
private fun ActionMenuSection() {
    var expanded by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var selectedPlacement by remember {
        mutableStateOf(ActionMenuPlacement.BottomEnd)
    }

    CatalogSection(title = "Action Menu") {
        CatalogStateLabel("Default · Destructive")
        Text(
            text = "Placement: ${selectedPlacement.name}\n" +
                "Selected: ${selectedItem ?: "None"}",
            color = ClipyTheme.colors.neutral.gray800,
            style = ClipyTheme.typography.body2Medium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionMenuPlacement.entries
                .chunked(2)
                .forEach { placements ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        placements.forEach { placement ->
                            ClipyButton(
                                text = placement.name,
                                onClick = { selectedPlacement = placement },
                                enabled = placement != selectedPlacement,
                                size = ButtonSize.Small,
                            )
                        }
                    }
                }
        }
        Spacer(Modifier.height(16.dp))
        CatalogStateLabel("Preview")
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ClipyActionMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                items = catalogActionMenuItems(
                    onClick = { item ->
                        selectedItem = item
                        expanded = false
                    },
                ),
                placement = selectedPlacement,
                anchor = {
                    ClipyButton(
                        text = "Open menu",
                        onClick = { expanded = true },
                        size = ButtonSize.Small,
                    )
                },
            )
        }
        Spacer(Modifier.height(240.dp))
    }
}

@Composable
private fun catalogActionMenuItems(
    onClick: (String) -> Unit,
): List<ActionMenuItem> = listOf(
    ActionMenuItem(
        text = "Open in tab",
        onClick = { onClick("Open in tab") },
        icon = ActionMenuIcon.Link,
    ),
    ActionMenuItem(
        text = "Share",
        onClick = { onClick("Share") },
        icon = ActionMenuIcon.Share,
    ),
    ActionMenuItem(
        text = "Edit",
        onClick = { onClick("Edit") },
        icon = ActionMenuIcon.Edit,
    ),
    ActionMenuItem(
        text = "Delete",
        onClick = { onClick("Delete") },
        icon = ActionMenuIcon.Delete,
        type = ActionMenuItemType.Destructive,
    ),
)

@Composable
private fun CatalogSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = ClipyTheme.colors.neutral.gray950,
            style = ClipyTheme.typography.heading3,
        )
        content()
    }
}

@Composable
private fun CatalogStateLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = ClipyTheme.colors.neutral.gray600,
        style = ClipyTheme.typography.body3Medium,
    )
}

@Preview(
    name = "Phone",
    showBackground = true,
    widthDp = 390,
    heightDp = 1400,
)
@Composable
private fun ComponentCatalogPhonePreview() {
    ClipyTheme {
        ComponentCatalogScreen()
    }
}

@Preview(
    name = "Tablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 1400,
)
@Composable
private fun ComponentCatalogTabletPreview() {
    ClipyTheme {
        ComponentCatalogScreen()
    }
}
