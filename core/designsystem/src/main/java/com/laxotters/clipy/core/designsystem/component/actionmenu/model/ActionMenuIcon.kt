package com.laxotters.clipy.core.designsystem.component.actionmenu.model

import androidx.annotation.DrawableRes
import com.laxotters.clipy.core.designsystem.R

enum class ActionMenuIcon(
    @get:DrawableRes internal val resourceId: Int,
) {
    Link(R.drawable.ic_action_menu_link),
    Share(R.drawable.ic_action_menu_share),
    Edit(R.drawable.ic_action_menu_edit),
    Delete(R.drawable.ic_action_menu_delete),
}
