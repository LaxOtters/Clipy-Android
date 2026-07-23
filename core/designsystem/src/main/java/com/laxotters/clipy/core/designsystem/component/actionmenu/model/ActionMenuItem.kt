package com.laxotters.clipy.core.designsystem.component.actionmenu.model

enum class ActionMenuItemType {
    Default,
    Destructive,
}

data class ActionMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val icon: ActionMenuIcon,
    val type: ActionMenuItemType = ActionMenuItemType.Default,
)
