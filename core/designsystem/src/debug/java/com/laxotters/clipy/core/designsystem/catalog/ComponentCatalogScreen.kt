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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.component.ClipyDivider
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
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
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyDialogStyle
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyDualDialog
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialog
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogState
import com.laxotters.clipy.core.designsystem.component.dialog.ClipySingleDialog
import com.laxotters.clipy.core.designsystem.component.error.ClipyErrorOverlay
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarController
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarIcon
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarLayout
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import kotlinx.coroutines.launch

@Composable
fun ComponentCatalogScreen(modifier: Modifier = Modifier) {
    ClipySnackbarLayout(
        modifier = modifier.fillMaxSize(),
    ) {
        val snackbar = rememberClipySnackbarController()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ClipyTheme.colors.neutral.gray50)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp,
                ),
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
            DialogSection()
            ErrorOverlaySection()
            SnackbarSection(snackbar = snackbar)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ButtonSection() {
    CatalogSection(
        title = "Button",
    ) {
        ButtonType.entries.forEach { type ->
            CatalogStateLabel("${type.name} · Medium")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    true,
                    false,
                ).forEach { enabled ->
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                true,
                false,
            ).forEach { enabled ->
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
    CatalogSection(
        title = "Footer Button",
    ) {
        FooterButtonType.entries.forEach { type ->
            listOf(
                true,
                false,
            ).forEach { enabled ->
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
    CatalogSection(
        title = "Divider",
    ) {
        listOf(
            "Large · 40dp" to DividerDefaults.largeVerticalPadding,
            "Medium · 20dp" to DividerDefaults.mediumVerticalPadding,
            "Small · 10dp" to DividerDefaults.smallVerticalPadding,
        ).forEach { labeledPadding ->
            CatalogStateLabel(labeledPadding.first)
            ClipyDivider(verticalPadding = labeledPadding.second)
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

    CatalogSection(
        title = "Action Menu",
    ) {
        CatalogStateLabel("Default · Destructive")
        Text(
            text = "Placement: ${selectedPlacement.name}\n" +
                "Selected: ${selectedItem ?: "None"}",
            color = ClipyTheme.colors.neutral.gray800,
            style = ClipyTheme.typography.body2Medium,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionMenuPlacement.entries
                .chunked(2)
                .forEach { placements ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
private fun DialogSection() {
    var appDialog by remember { mutableStateOf<CatalogAppDialog?>(null) }
    var jsDialog by remember { mutableStateOf<CatalogJsDialog?>(null) }

    CatalogSection(
        title = "Dialog",
    ) {
        CatalogStateLabel("App dialog · Single action")
        ClipyButton(
            text = "Open app dialog",
            onClick = { appDialog = CatalogAppDialog.Single },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("App dialog · Two actions")
        ClipyButton(
            text = "Open confirmation",
            onClick = { appDialog = CatalogAppDialog.Dual },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("App dialog · Error · Single / Two actions")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClipyButton(
                text = "Error single",
                onClick = { appDialog = CatalogAppDialog.ErrorSingle },
                size = ButtonSize.Small,
            )
            ClipyButton(
                text = "Error two",
                onClick = { appDialog = CatalogAppDialog.ErrorDual },
                size = ButtonSize.Small,
            )
        }
        CatalogStateLabel("Js dialog · Alert / Confirm / Prompt")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClipyButton(
                text = "Alert",
                onClick = { jsDialog = CatalogJsDialog.Alert },
                size = ButtonSize.Small,
            )
            ClipyButton(
                text = "Confirm",
                onClick = { jsDialog = CatalogJsDialog.Confirm },
                size = ButtonSize.Small,
            )
            ClipyButton(
                text = "Prompt",
                onClick = { jsDialog = CatalogJsDialog.Prompt },
                size = ButtonSize.Small,
            )
        }
    }

    when (appDialog) {
        CatalogAppDialog.Single -> ClipySingleDialog(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = ClipyTextAction(
                label = "확인",
                onClick = { appDialog = null },
            ),
        )

        CatalogAppDialog.Dual -> ClipyDualDialog(
            title = "항목을 삭제할까요?",
            description = "삭제한 항목은 되돌릴 수 없습니다.",
            primaryAction = ClipyTextAction(
                label = "삭제",
                onClick = { appDialog = null },
            ),
            secondaryAction = ClipyTextAction(
                label = "취소",
                onClick = { appDialog = null },
            ),
        )

        CatalogAppDialog.ErrorSingle -> ClipySingleDialog(
            title = "연결할 수 없습니다",
            description = "잠시 후 다시 시도해주세요.",
            primaryAction = ClipyTextAction(
                label = "확인",
                onClick = { appDialog = null },
            ),
            style = ClipyDialogStyle.Error,
        )

        CatalogAppDialog.ErrorDual -> ClipyDualDialog(
            title = "항목을 삭제할 수 없습니다",
            description = "잠시 후 다시 시도하거나 취소해주세요.",
            primaryAction = ClipyTextAction(
                label = "재시도",
                onClick = { appDialog = null },
            ),
            secondaryAction = ClipyTextAction(
                label = "취소",
                onClick = { appDialog = null },
            ),
            style = ClipyDialogStyle.Error,
        )

        null -> Unit
    }

    when (jsDialog) {
        CatalogJsDialog.Alert -> ClipyJsDialog(
            source = "Request from example.com",
            state = ClipyJsDialogState.Alert(
                title = "알림",
                description = "웹사이트에서 메시지를 보냈습니다.",
                confirmAction = ClipyTextAction(
                    label = "확인",
                    onClick = { jsDialog = null },
                ),
            ),
        )

        CatalogJsDialog.Confirm -> ClipyJsDialog(
            source = "Request from example.com",
            state = ClipyJsDialogState.Confirm(
                title = "계속 진행할까요?",
                description = "웹사이트 요청을 확인해주세요.",
                confirmAction = ClipyTextAction(
                    label = "확인",
                    onClick = { jsDialog = null },
                ),
                cancelAction = ClipyTextAction(
                    label = "취소",
                    onClick = { jsDialog = null },
                ),
            ),
        )

        CatalogJsDialog.Prompt -> ClipyJsDialog(
            source = "Request from example.com",
            state = ClipyJsDialogState.Prompt(
                title = "이름을 입력해주세요",
                description = "웹사이트에 전달할 값을 입력하세요.",
                confirmAction = ClipyTextInputAction(
                    label = "확인",
                    onClick = { jsDialog = null },
                ),
                initialValue = "Clipy",
                cancelAction = ClipyTextAction(
                    label = "취소",
                    onClick = { jsDialog = null },
                ),
            ),
        )

        null -> Unit
    }
}

@Composable
private fun SnackbarSection(snackbar: ClipySnackbarController) {
    val coroutineScope = rememberCoroutineScope()

    CatalogSection(
        title = "Snackbar",
    ) {
        CatalogStateLabel("Message only · 2 seconds")
        ClipyButton(
            text = "Show message",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(message = "항목을 저장했습니다.")
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Message with action · 2 seconds")
        ClipyButton(
            text = "Show action",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "네트워크에 연결할 수 없습니다.",
                        action = ClipyTextAction(
                            label = "재시도",
                            onClick = {},
                        ),
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Long text and action · Stacked")
        ClipyButton(
            text = "Show stacked action",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "Long text Snackbar Example",
                        action = ClipyTextAction(
                            label = "Long Action Example",
                            onClick = {},
                        ),
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Long two-line text and action · Stacked")
        ClipyButton(
            text = "Show stacked two lines",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "Long text Snackbar Example\nLong text Snackbar Example",
                        action = ClipyTextAction(
                            label = "Long Action Example",
                            onClick = {},
                        ),
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Error icon")
        ClipyButton(
            text = "Show error",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "Something went wrong",
                        icon = ClipySnackbarIcon.Error,
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Success icon · One line")
        ClipyButton(
            text = "Show success",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "Item Saved",
                        icon = ClipySnackbarIcon.Success,
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Success icon · Two lines")
        ClipyButton(
            text = "Show success two lines",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "Item Saved\nYou can continue browsing.",
                        icon = ClipySnackbarIcon.Success,
                    )
                }
            },
            size = ButtonSize.Small,
        )
        CatalogStateLabel("Two-line message")
        ClipyButton(
            text = "Show two lines",
            onClick = {
                coroutineScope.launch {
                    snackbar.showSnackbar(
                        message = "콘텐츠를 불러오지 못했습니다.\n잠시 후 다시 시도해주세요.",
                    )
                }
            },
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun ErrorOverlaySection() {
    CatalogSection(
        title = "Error overlay",
    ) {
        CatalogStateLabel("Default")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) {
            ClipyErrorOverlay(
                title = "페이지를 불러올 수 없습니다",
                description = "잠시 후 다시 시도해주세요.",
            )
        }
        CatalogStateLabel("Secondary action")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
        ) {
            ClipyErrorOverlay(
                title = "페이지를 불러올 수 없습니다",
                description = "인터넷 연결을 확인한 후\n다시 시도해주세요.",
                action = ClipyTextAction(
                    label = "다시 시도",
                    onClick = {},
                ),
            )
        }
    }
}

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

private enum class CatalogJsDialog {
    Alert,
    Confirm,
    Prompt,
}

private enum class CatalogAppDialog {
    Single,
    Dual,
    ErrorSingle,
    ErrorDual,
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
