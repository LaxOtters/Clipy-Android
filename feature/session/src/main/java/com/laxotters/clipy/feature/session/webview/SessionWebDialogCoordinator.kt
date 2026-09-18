package com.laxotters.clipy.feature.session.webview

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.ClipyTextInputAction
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogManager
import com.laxotters.clipy.core.designsystem.component.dialog.JsDialogRequest
import com.laxotters.clipy.feature.session.R
import com.laxotters.clipy.feature.session.util.formatUrlLabel

/** WebView JavaScript 결과 수명을 소유하고 요청을 공용 Dialog 모델로 변환합니다. */
internal class SessionWebDialogCoordinator(
    private val showDialog: (JsDialogRequest) -> Boolean,
    private val cancelActiveDialog: (JsDialogRequest) -> Unit,
    private var strings: SessionWebDialogStrings,
    private val displayGate: SessionWebDialogGate = SessionWebDialogGate(),
) {
    constructor(
        manager: ClipyJsDialogManager,
        strings: SessionWebDialogStrings,
        displayGate: SessionWebDialogGate,
    ) : this(
        showDialog = manager::show,
        cancelActiveDialog = manager::cancelActive,
        strings = strings,
        displayGate = displayGate,
    )

    private var isClosed = false
    private var ownedRequest: JsDialogRequest? = null

    /** 같은 WebView의 수명은 유지하고 다음 요청에 사용할 문구만 갱신합니다. */
    @MainThread
    fun updateStrings(strings: SessionWebDialogStrings) {
        this.strings = strings
    }

    @MainThread
    fun showAlert(
        url: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        val completion = ExactOnceCompletion<Unit>(
            onConfirm = { onConfirm() },
            onCancel = onCancel,
        )
        return show(
            request = JsDialogRequest.Alert(
                source = source(url),
                title = strings.alertTitle,
                description = message,
                confirmAction = ClipyTextAction(
                    label = strings.confirmAction,
                    onClick = { completion.confirm(Unit) },
                ),
                onCancel = completion::cancel,
            ),
            completion = completion,
        )
    }

    @MainThread
    fun showConfirm(
        url: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ): Boolean = showConfirmation(
        url = url,
        message = message,
        copy = ConfirmationCopy(
            title = strings.confirmTitle,
            confirmLabel = strings.confirmAction,
            cancelLabel = strings.cancelAction,
        ),
        onConfirm = onConfirm,
        onCancel = onCancel,
    )

    @MainThread
    fun showPrompt(
        url: String,
        message: String,
        defaultValue: String?,
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        val completion = ExactOnceCompletion<String>(
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
        return show(
            request = JsDialogRequest.Prompt(
                source = source(url),
                title = strings.promptTitle,
                description = message,
                initialValue = defaultValue.orEmpty(),
                confirmAction = ClipyTextInputAction(
                    label = strings.confirmAction,
                    onClick = completion::confirm,
                ),
                cancelAction = ClipyTextAction(
                    label = strings.cancelAction,
                    onClick = completion::cancel,
                ),
            ),
            completion = completion,
        )
    }

    @MainThread
    fun showBeforeUnload(
        url: String,
        message: String,
        onLeave: () -> Unit,
        onStay: () -> Unit,
    ): Boolean = showConfirmation(
        url = url,
        message = message,
        copy = ConfirmationCopy(
            title = strings.beforeUnloadTitle,
            confirmLabel = strings.leaveAction,
            cancelLabel = strings.stayAction,
        ),
        onConfirm = onLeave,
        onCancel = onStay,
    )

    /** 실제 WebView 해제 시 먼저 요청을 차단하고 자신이 소유한 요청만 취소합니다. */
    @MainThread
    fun close() {
        if (isClosed) return

        isClosed = true
        val request = ownedRequest
        ownedRequest = null
        if (request != null) {
            displayGate.cancel(request) {
                cancelActiveDialog(request)
            }
        }
    }

    private fun showConfirmation(
        url: String,
        message: String,
        copy: ConfirmationCopy,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        val completion = ExactOnceCompletion<Unit>(
            onConfirm = { onConfirm() },
            onCancel = onCancel,
        )
        return show(
            request = JsDialogRequest.Confirm(
                source = source(url),
                title = copy.title,
                description = message,
                confirmAction = ClipyTextAction(
                    label = copy.confirmLabel,
                    onClick = { completion.confirm(Unit) },
                ),
                cancelAction = ClipyTextAction(
                    label = copy.cancelLabel,
                    onClick = completion::cancel,
                ),
            ),
            completion = completion,
        )
    }

    private fun show(
        request: JsDialogRequest,
        completion: ExactOnceCompletion<*>,
    ): Boolean {
        completion.onCompleted = {
            if (ownedRequest === request) {
                ownedRequest = null
            }
            displayGate.release(request)
        }
        if (isClosed || !displayGate.tryAcquire(request) || !showDialog(request)) {
            completion.cancel()
            return false
        }

        ownedRequest = request
        return true
    }

    private fun source(url: String): String = strings.sourcePrefix + formatUrlLabel(url)
}

private data class ConfirmationCopy(
    val title: String,
    val confirmLabel: String,
    val cancelLabel: String,
)

internal data class SessionWebDialogStrings(
    val sourcePrefix: String,
    val alertTitle: String,
    val confirmTitle: String,
    val promptTitle: String,
    val beforeUnloadTitle: String,
    val confirmAction: String,
    val cancelAction: String,
    val stayAction: String,
    val leaveAction: String,
)

@Composable
internal fun sessionWebDialogStrings(): SessionWebDialogStrings =
    SessionWebDialogStrings(
        sourcePrefix = stringResource(R.string.session_js_dialog_source, ""),
        alertTitle = stringResource(R.string.session_js_dialog_alert_title),
        confirmTitle = stringResource(R.string.session_js_dialog_confirm_title),
        promptTitle = stringResource(R.string.session_js_dialog_prompt_title),
        beforeUnloadTitle = stringResource(R.string.session_js_dialog_before_unload_title),
        confirmAction = stringResource(R.string.session_js_dialog_confirm_action),
        cancelAction = stringResource(R.string.session_js_dialog_cancel_action),
        stayAction = stringResource(R.string.session_js_dialog_stay_action),
        leaveAction = stringResource(R.string.session_js_dialog_leave_action),
    )

private class ExactOnceCompletion<T>(
    private val onConfirm: (T) -> Unit,
    private val onCancel: () -> Unit,
) {
    var onCompleted: () -> Unit = {}
    private var isCompleted = false

    fun confirm(value: T) {
        complete { onConfirm(value) }
    }

    fun cancel() {
        complete(onCancel)
    }

    private fun complete(action: () -> Unit) {
        if (isCompleted) return

        isCompleted = true
        onCompleted()
        onCompleted = {}
        action()
    }
}
