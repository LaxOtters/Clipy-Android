package com.laxotters.clipy.feature.session.webview

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
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyDualDialog
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogHost
import com.laxotters.clipy.core.designsystem.component.dialog.ClipyJsDialogManager
import com.laxotters.clipy.core.designsystem.component.dialog.rememberClipyJsDialogManager

/** Session의 JS Dialog와 앱 확인 Dialog가 공유하는 단일 표시 경계를 소유합니다. */
@Stable
internal class SessionWebDialogState(
    internal val jsDialogManager: ClipyJsDialogManager,
    internal val displayGate: SessionWebDialogGate,
    internal val externalActionManager: SessionExternalActionDialogManager,
)

/** 먼저 수락한 Dialog 요청만 표시하고 후발 요청을 대기시키지 않습니다. */
internal class SessionWebDialogGate {
    private var activeOwner: Any? = null
    private var isCancelling = false

    @MainThread
    fun tryAcquire(owner: Any): Boolean {
        if (isCancelling || activeOwner != null) return false

        activeOwner = owner
        return true
    }

    @MainThread
    fun release(owner: Any) {
        if (activeOwner === owner) {
            activeOwner = null
        }
    }

    @MainThread
    fun cancel(
        owner: Any,
        action: () -> Unit,
    ) {
        if (isCancelling || activeOwner !== owner) return

        isCancelling = true
        try {
            action()
        } finally {
            if (activeOwner === owner) {
                activeOwner = null
            }
            isCancelling = false
        }
    }
}

@Stable
internal class SessionExternalActionDialogManager(
    private val displayGate: SessionWebDialogGate,
) {
    internal var activeEntry: SessionExternalActionDialogEntry? by mutableStateOf(null)
        private set

    @MainThread
    fun show(
        owner: Any,
        request: SessionExternalActionDialogRequest,
    ): Boolean {
        val entry = SessionExternalActionDialogEntry(owner = owner, request = request)
        if (!displayGate.tryAcquire(entry)) return false

        activeEntry = entry
        return true
    }

    @MainThread
    fun cancelOwner(owner: Any) {
        val entry = activeEntry ?: return
        if (entry.owner !== owner) return

        activeEntry = null
        displayGate.release(entry)
    }

    @MainThread
    internal fun complete(
        entry: SessionExternalActionDialogEntry,
        action: () -> Unit,
    ) {
        if (activeEntry !== entry) return

        // 확인 동작에서 새 요청이 발생할 수 있어 완료한 요청의 표시 권한을 먼저 반환합니다.
        activeEntry = null
        displayGate.release(entry)
        action()
    }
}

internal class SessionExternalActionDialogEntry(
    val owner: Any,
    val request: SessionExternalActionDialogRequest,
)

internal data class SessionExternalActionDialogRequest(
    val title: String,
    val description: String,
    val continueLabel: String,
    val cancelLabel: String,
    val onContinue: () -> Unit,
)

@Composable
internal fun rememberSessionWebDialogState(): SessionWebDialogState {
    val jsDialogManager = rememberClipyJsDialogManager()
    val displayGate = remember { SessionWebDialogGate() }
    val externalActionManager = remember(displayGate) {
        SessionExternalActionDialogManager(displayGate)
    }

    return remember(jsDialogManager, displayGate, externalActionManager) {
        SessionWebDialogState(
            jsDialogManager = jsDialogManager,
            displayGate = displayGate,
            externalActionManager = externalActionManager,
        )
    }
}

/** Session에서 수락된 JS 또는 앱 확인 Dialog 한 건을 표시합니다. */
@Composable
internal fun SessionWebDialogHosts(
    state: SessionWebDialogState,
    modifier: Modifier = Modifier,
) {
    val externalEntry = state.externalActionManager.activeEntry
    if (externalEntry != null) {
        key(externalEntry) {
            val request = externalEntry.request
            ClipyDualDialog(
                title = request.title,
                description = request.description,
                primaryAction = ClipyTextAction(
                    label = request.continueLabel,
                    onClick = {
                        state.externalActionManager.complete(
                            entry = externalEntry,
                            action = request.onContinue,
                        )
                    },
                ),
                secondaryAction = ClipyTextAction(
                    label = request.cancelLabel,
                    onClick = {
                        state.externalActionManager.complete(
                            entry = externalEntry,
                            action = {},
                        )
                    },
                ),
                modifier = modifier,
            )
        }
    }

    ClipyJsDialogHost(
        manager = state.jsDialogManager,
        modifier = modifier,
    )
}
