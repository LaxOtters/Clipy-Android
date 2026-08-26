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

/** Route 수명에서 한 번에 하나의 JS Dialog 요청만 수락하고 표시를 중재합니다. */
@Stable
class ClipyJsDialogManager internal constructor() {
    internal var activeRequestEntry: JsDialogRequestEntry? by mutableStateOf(null)
        private set

    /** 표시 중인 요청이 없을 때만 요청을 수락하고, 수락 여부를 반환합니다. */
    @MainThread
    fun show(request: ClipyJsDialogRequest): Boolean {
        if (activeRequestEntry != null) {
            return false
        }

        activeRequestEntry = JsDialogRequestEntry(request)
        return true
    }

    /** 현재 요청만 완료하고, action 실행 뒤 표시 슬롯을 비웁니다. */
    @MainThread
    internal fun complete(
        requestEntry: JsDialogRequestEntry,
        action: () -> Unit,
    ) {
        // 현재 표시 요청과 무관한 이전 action은 처리하지 않습니다.
        if (activeRequestEntry !== requestEntry) {
            return
        }

        try {
            action()
        } finally {
            if (activeRequestEntry === requestEntry) {
                activeRequestEntry = null
            }
        }
    }
}

/** Host가 표시하는 JS Dialog 요청 한 건의 상태와 action 수명을 구분합니다. */
internal class JsDialogRequestEntry(
    val request: ClipyJsDialogRequest,
)

/** Manager가 수락한 현재 JS Dialog 요청을 표시하고, action 실행 뒤 요청을 완료합니다. */
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
            is ClipyJsDialogRequest.Alert -> {
                ClipyJsDialog(
                    source = request.source,
                    state = ClipyJsDialogState.Alert(
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

            is ClipyJsDialogRequest.Confirm -> {
                ClipyJsDialog(
                    source = request.source,
                    state = ClipyJsDialogState.Confirm(
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

            is ClipyJsDialogRequest.Prompt -> {
                ClipyJsDialog(
                    source = request.source,
                    state = ClipyJsDialogState.Prompt(
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

/** JS Dialog에 표시할 콘텐츠와 웹 요청에 돌려줄 action을 정의합니다. */
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
