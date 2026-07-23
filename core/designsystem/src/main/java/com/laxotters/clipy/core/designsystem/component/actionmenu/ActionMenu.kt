package com.laxotters.clipy.core.designsystem.component.actionmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuIcon
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuItem
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuItemType
import com.laxotters.clipy.core.designsystem.component.actionmenu.model.ActionMenuPlacement
import com.laxotters.clipy.core.designsystem.component.button.ClipyButton
import com.laxotters.clipy.core.designsystem.component.button.model.ButtonSize
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

/**
 * [anchor]를 기준으로 [items]를 표시하는 액션 메뉴입니다.
 *
 * [placement] 위치를 우선하며, 공간이 부족한 경우 반대 방향이나 화면 경계 안쪽으로 조정합니다.
 * [expanded] 상태와 항목 선택 후 닫힘은 호출자가 관리합니다.
 * 외부 터치나 시스템 Back으로 닫기가 요청되면 [onDismissRequest]를 호출합니다.
 *
 * @throws IllegalArgumentException [items]가 비어 있는 경우
 */
@Composable
fun ClipyActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<ActionMenuItem>,
    modifier: Modifier = Modifier,
    placement: ActionMenuPlacement = ActionMenuPlacement.BottomEnd,
    anchor: @Composable () -> Unit,
) {
    require(items.isNotEmpty()) {
        "ClipyActionMenu requires at least one item."
    }

    Box(modifier = modifier) {
        anchor()

        if (expanded) {
            val density = LocalDensity.current
            val positionProvider = remember(density, placement) {
                ActionMenuPositionProvider(
                    density = density,
                    placement = placement,
                )
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(135.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = ClipyTheme.colors.primary.indigo50,
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        items.forEach { item ->
                            ActionMenuItemContent(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionMenuItemContent(
    item: ActionMenuItem,
) {
    val textColor = when (item.type) {
        ActionMenuItemType.Default -> ClipyTheme.colors.neutral.gray800
        ActionMenuItemType.Destructive -> ClipyTheme.colors.error.error700
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(
                role = Role.Button,
                onClick = item.onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.icon.resourceId),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.text,
            color = textColor,
            style = ClipyTheme.typography.body2Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(
    name = "Action Menu",
    showBackground = true,
    widthDp = 300,
    heightDp = 400,
)
@Composable
private fun ClipyActionMenuPreview() {
    ClipyTheme {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClipyTheme.colors.neutral.gray50)
                .padding(20.dp, 70.dp),
        ) {
            ClipyActionMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.align(Alignment.TopEnd),
                items = previewActionMenuItems(
                    onClick = { expanded = false },
                ),
                anchor = {
                    ClipyButton(
                        text = "Anchor",
                        onClick = { expanded = true },
                        modifier = Modifier.padding(vertical = 10.dp),
                        size = ButtonSize.Small,
                    )
                },
            )
        }
    }
}

@Composable
private fun previewActionMenuItems(
    onClick: () -> Unit,
): List<ActionMenuItem> = listOf(
    ActionMenuItem(
        text = "Open in tab",
        onClick = onClick,
        icon = ActionMenuIcon.Link,
    ),
    ActionMenuItem(
        text = "Share",
        onClick = onClick,
        icon = ActionMenuIcon.Share,
    ),
    ActionMenuItem(
        text = "Edit",
        onClick = onClick,
        icon = ActionMenuIcon.Edit,
    ),
    ActionMenuItem(
        text = "Delete",
        onClick = onClick,
        icon = ActionMenuIcon.Delete,
        type = ActionMenuItemType.Destructive,
    ),
)
