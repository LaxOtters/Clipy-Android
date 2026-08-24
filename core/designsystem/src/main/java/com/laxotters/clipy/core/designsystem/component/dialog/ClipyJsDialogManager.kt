package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction

/** Route 수명 동안 하나의 JavaScript Dialog 요청만 관리합니다. */
@Stable
class ClipyJsDialogManager internal constructor() {
    internal var activeRequest: ClipyJsDialogRequest? by mutableStateOf(null)
        private set

    /** JavaScript Dialog 요청을 등록하며, 이미 요청이 있으면 `false`를 반환합니다. */
    @MainThread
    fun show(request: ClipyJsDialogRequest): Boolean {
        if (activeRequest != null) {
            return false
        }

        activeRequest = request
        return true
    }

    @MainThread
    internal fun complete(
        request: ClipyJsDialogRequest,
        action: () -> Unit,
    ) {
        if (activeRequest !== request) {
            return
        }

        try {
            action()
        } finally {
            if (activeRequest === request) {
                activeRequest = null
            }
        }
    }
}

/** 현재 요청을 JavaScript Dialog로 표시하고 action 완료 시 요청을 제거합니다. */
@Composable
fun ClipyJsDialogHost(
    manager: ClipyJsDialogManager,
    modifier: Modifier = Modifier,
) {
    val request = manager.activeRequest ?: return

    when (request) {
        is ClipyJsDialogRequest.Alert -> {
            ClipyJsDialog(
                source = request.source,
                state = ClipyJsDialogState.Alert(
                    title = request.title,
                    description = request.description,
                    confirmAction = request.confirmAction.withCompletion(
                        request = request,
                        manager = manager,
                    ),
                ),
                modifier = modifier,
            )
        }

        is ClipyJsDialogRequest.Confirm -> {
            ClipyJsDialog(
                source = request.source,
                state = ClipyJsDialogState.Confirm(
                    title = request.title,
                    description = request.description,
                    confirmAction = request.confirmAction.withCompletion(
                        request = request,
                        manager = manager,
                    ),
                    cancelAction = request.cancelAction.withCompletion(
                        request = request,
                        manager = manager,
                    ),
                ),
                modifier = modifier,
            )
        }

        is ClipyJsDialogRequest.Prompt -> {
            ClipyJsDialog(
                source = request.source,
                state = ClipyJsDialogState.Prompt(
                    title = request.title,
                    description = request.description,
                    initialValue = request.initialValue,
                    confirmAction = request.confirmAction.withCompletion(
                        request = request,
                        manager = manager,
                    ),
                    cancelAction = request.cancelAction.withCompletion(
                        request = request,
                        manager = manager,
                    ),
                ),
                modifier = modifier,
            )
        }
    }
}

@Composable
fun rememberClipyJsDialogManager(): ClipyJsDialogManager = remember {
    ClipyJsDialogManager()
}

/** JavaScript Dialog에 표시할 콘텐츠와 결과 action을 정의합니다. */
sealed interface ClipyJsDialogRequest {
    val source: String
    val title: String
    val description: String

    data class Alert(
        override val source: String,
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
    ) : ClipyJsDialogRequest

    data class Confirm(
        override val source: String,
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
        val cancelAction: ClipyTextAction,
    ) : ClipyJsDialogRequest

    data class Prompt(
        override val source: String,
        override val title: String,
        override val description: String,
        val initialValue: String,
        val confirmAction: ClipyTextInputAction,
        val cancelAction: ClipyTextAction,
    ) : ClipyJsDialogRequest
}

private fun ClipyTextAction.withCompletion(
    request: ClipyJsDialogRequest,
    manager: ClipyJsDialogManager,
): ClipyTextAction {
    val originalAction = onClick

    return copy(
        onClick = {
            manager.complete(
                request = request,
                action = originalAction,
            )
        },
    )
}

private fun ClipyTextInputAction.withCompletion(
    request: ClipyJsDialogRequest,
    manager: ClipyJsDialogManager,
): ClipyTextInputAction {
    val originalAction = onClick

    return copy(
        onClick = { value ->
            manager.complete(
                request = request,
                action = { originalAction(value) },
            )
        },
    )
}
