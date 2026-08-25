package com.laxotters.clipy.core.designsystem.component.actionmenu.model

import androidx.annotation.DrawableRes
import com.laxotters.clipy.core.designsystem.R

enum class ActionMenuIcon(
    @get:DrawableRes internal val resourceId: Int,
) {
    Link(R.drawable.ic_link),
    Share(R.drawable.ic_share),
    Edit(R.drawable.ic_pencil),
    Delete(R.drawable.ic_delete),
}
