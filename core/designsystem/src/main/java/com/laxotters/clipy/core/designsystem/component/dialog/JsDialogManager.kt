package com.laxotters.clipy.core.designsystem.component.dialog

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction

/** 소유자 수명 안에서 하나의 JS Dialog 요청만 수락하고 표시를 중재합니다. */
@Stable
class ClipyJsDialogManager internal constructor() {
    internal var activeRequestEntry: JsDialogRequestEntry? by mutableStateOf(null)
        private set
    private var isCancelling = false

    /** 표시 중인 요청이 없을 때만 요청을 수락하고, 수락 여부를 반환합니다. */
    @MainThread
    fun show(request: JsDialogRequest): Boolean {
        if (isCancelling || activeRequestEntry != null) {
            return false
        }

        activeRequestEntry = JsDialogRequestEntry(request)
        return true
    }

    /** 현재 요청을 수명주기 종료로 취소합니다. 취소 중 발생한 후속 요청은 수락하지 않습니다. */
    @MainThread
    fun cancelActive() {
        if (isCancelling) return
        val requestEntry = activeRequestEntry ?: return

        isCancelling = true
        try {
            requestEntry.request.onCancel()
        } finally {
            if (activeRequestEntry === requestEntry) {
                activeRequestEntry = null
            }
            isCancelling = false
        }
    }

    /** 소유자가 넘긴 요청이 현재 요청일 때만 취소하여 다른 WebView의 요청을 보호합니다. */
    @MainThread
    fun cancelActive(request: JsDialogRequest) {
        if (activeRequestEntry?.request === request) {
            cancelActive()
        }
    }

    /** 현재 사용자 응답을 완료합니다. 응답이 만든 후속 요청을 받을 수 있도록 action 전에 슬롯을 비웁니다. */
    @MainThread
    internal fun complete(
        requestEntry: JsDialogRequestEntry,
        action: () -> Unit,
    ) {
        // 현재 표시 요청과 무관한 이전 action은 처리하지 않습니다.
        if (isCancelling || activeRequestEntry !== requestEntry) {
            return
        }

        activeRequestEntry = null
        action()
    }
}

/** Host가 표시하는 JS Dialog 요청 한 건의 상태와 action 수명을 구분합니다. */
internal class JsDialogRequestEntry(
    val request: JsDialogRequest,
)

/** Manager가 수락한 현재 JS Dialog 요청을 표시하고, 사용자 응답을 한 번만 완료합니다. */
@Composable
fun ClipyJsDialogHost(
    manager: ClipyJsDialogManager,
    modifier: Modifier = Modifier,
) {
    val requestEntry = manager.activeRequestEntry ?: return
    val request = requestEntry.request

    // 요청이 바뀌면 Dialog 내부 입력값과 스크롤 상태를 새로 생성합니다.
    key(requestEntry) {
        when (request) {
            is JsDialogRequest.Alert -> {
                ClipyJsDialog(
                    source = request.source,
                    state = JsDialogState.Alert(
                        title = request.title,
                        description = request.description,
                        confirmAction = request.confirmAction.withCompletion(
                            requestEntry = requestEntry,
                            manager = manager,
                        ),
                    ),
                    modifier = modifier,
                )
            }

            is JsDialogRequest.Confirm -> {
                ClipyJsDialog(
                    source = request.source,
                    state = JsDialogState.Confirm(
                        title = request.title,
                        description = request.description,
                        confirmAction = request.confirmAction.withCompletion(
                            requestEntry = requestEntry,
                            manager = manager,
                        ),
                        cancelAction = request.cancelAction.withCompletion(
                            requestEntry = requestEntry,
                            manager = manager,
                        ),
                    ),
                    modifier = modifier,
                )
            }

            is JsDialogRequest.Prompt -> {
                ClipyJsDialog(
                    source = request.source,
                    state = JsDialogState.Prompt(
                        title = request.title,
                        description = request.description,
                        initialValue = request.initialValue,
                        confirmAction = request.confirmAction.withCompletion(
                            requestEntry = requestEntry,
                            manager = manager,
                        ),
                        cancelAction = request.cancelAction.withCompletion(
                            requestEntry = requestEntry,
                            manager = manager,
                        ),
                    ),
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun rememberClipyJsDialogManager(): ClipyJsDialogManager = remember {
    ClipyJsDialogManager()
}

/** JS Dialog의 표시 내용, 사용자 action과 수명주기 취소 동작을 정의합니다. */
sealed interface JsDialogRequest {
    val source: String
    val title: String
    val description: String
    val onCancel: () -> Unit

    data class Alert(
        override val source: String,
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
        override val onCancel: () -> Unit,
    ) : JsDialogRequest

    data class Confirm(
        override val source: String,
        override val title: String,
        override val description: String,
        val confirmAction: ClipyTextAction,
        val cancelAction: ClipyTextAction,
    ) : JsDialogRequest {
        override val onCancel: () -> Unit
            get() = cancelAction.onClick
    }

    data class Prompt(
        override val source: String,
        override val title: String,
        override val description: String,
        val initialValue: String,
        val confirmAction: ClipyTextInputAction,
        val cancelAction: ClipyTextAction,
    ) : JsDialogRequest {
        override val onCancel: () -> Unit
            get() = cancelAction.onClick
    }
}

private fun ClipyTextAction.withCompletion(
    requestEntry: JsDialogRequestEntry,
    manager: ClipyJsDialogManager,
): ClipyTextAction {
    val originalAction = onClick

    return copy(
        onClick = {
            manager.complete(
                requestEntry = requestEntry,
                action = originalAction,
            )
        },
    )
}

private fun ClipyTextInputAction.withCompletion(
    requestEntry: JsDialogRequestEntry,
    manager: ClipyJsDialogManager,
): ClipyTextInputAction {
    val originalAction = onClick

    return copy(
        onClick = { value ->
            manager.complete(
                requestEntry = requestEntry,
                action = { originalAction(value) },
            )
        },
    )
}
