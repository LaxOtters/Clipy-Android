package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.annotation.MainThread
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ClipySnackbarIcon {
    Success,
    Error,
}

internal data class ClipySnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    val icon: ClipySnackbarIcon? = null,
) : SnackbarVisuals {
    init {
        require(actionLabel == null || icon == null) {
            "Clipy Snackbar does not support action and icon together."
        }
    }

    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

internal suspend fun SnackbarHostState.showClipySnackbar(
    message: String,
    actionLabel: String? = null,
    icon: ClipySnackbarIcon? = null,
): SnackbarResult = showSnackbar(
    ClipySnackbarVisuals(
        message = message,
        actionLabel = actionLabel,
        icon = icon,
    ),
)

/** 앱 UI 수명 동안 Snackbar 요청의 FIFO 대기열과 중복 제거를 관리합니다. */
@Stable
class ClipySnackbarManager internal constructor(
    internal val hostState: SnackbarHostState,
    parentScope: CoroutineScope,
) {
    private val managerJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + managerJob)
    private val registeredKeys = mutableSetOf<String>()

    /** Snackbar 요청을 대기열에 등록합니다. */
    @MainThread
    fun showSnackbar(
        message: String,
        icon: ClipySnackbarIcon? = null,
        action: ClipyTextAction? = null,
        key: String = message,
    ) {
        require(icon == null || action == null) {
            "Clipy Snackbar does not support action and icon together."
        }

        enqueue(
            key = key,
            show = {
                hostState.showClipySnackbar(
                    message = message,
                    actionLabel = action?.label,
                    icon = icon,
                )
            },
            onAction = action?.onClick,
        )
    }

    internal fun cancel() {
        scope.cancel()
    }

    private fun enqueue(
        key: String,
        show: suspend () -> SnackbarResult,
        onAction: (() -> Unit)?,
    ) {
        if (!scope.isActive || !registeredKeys.add(key)) {
            return
        }

        scope.launch {
            try {
                if (show() == SnackbarResult.ActionPerformed) {
                    onAction?.invoke()
                }
            } finally {
                registeredKeys.remove(key)
            }
        }
    }
}

/** 현재 Composition 수명에 맞춰 Snackbar Manager와 요청 대기열을 생성합니다. */
@Composable
fun rememberClipySnackbarManager(): ClipySnackbarManager {
    val parentScope = rememberCoroutineScope()
    val hostState = remember { SnackbarHostState() }
    val manager = remember(
        hostState,
        parentScope,
    ) {
        ClipySnackbarManager(
            hostState = hostState,
            parentScope = parentScope,
        )
    }

    return manager
}
