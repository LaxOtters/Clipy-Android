package com.laxotters.clipy.core.designsystem.component.dialog

import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction

/** JavaScript Dialog의 화면 상태와 action을 정의합니다. */
sealed interface JsDialogState {
    val title: String
    val description: String

    data class Alert(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
    ) : JsDialogState

    data class Confirm(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
        val cancelAction: ClipyTextAction,
    ) : JsDialogState

    data class Prompt(
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextInputAction,
        val initialValue: String,
        val cancelAction: ClipyTextAction,
    ) : JsDialogState
}
