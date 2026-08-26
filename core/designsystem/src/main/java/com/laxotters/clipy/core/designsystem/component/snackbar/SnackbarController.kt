package com.laxotters.clipy.core.designsystem.component.snackbar

import androidx.annotation.MainThread
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction

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

/** 화면에서 Snackbar를 요청하기 위한 공개 API입니다. */
@Stable
class ClipySnackbarController internal constructor(
    private val manager: ClipySnackbarManager,
) {
    /** 현재 coroutine이 취소될 때까지 Snackbar 표시 완료를 기다립니다. */
    @MainThread
    suspend fun showSnackbar(
        message: String,
        icon: ClipySnackbarIcon? = null,
        action: ClipyTextAction? = null,
        key: String = message,
    ) {
        require(icon == null || action == null) {
            "Clipy Snackbar does not support action and icon together."
        }

        manager.showSnackbar(
            key = key,
            message = message,
            icon = icon,
            action = action,
        )
    }
}

/** Snackbar Host 내부에서 요청 대기열과 중복 key를 관리합니다. */
@Stable
internal class ClipySnackbarManager(
    internal val hostState: SnackbarHostState,
) {
    private val registeredKeys = mutableSetOf<String>()

    @MainThread
    internal suspend fun showSnackbar(
        key: String,
        message: String,
        icon: ClipySnackbarIcon?,
        action: ClipyTextAction?,
    ) {
        if (!registeredKeys.add(key)) {
            return
        }

        val result = try {
            hostState.showClipySnackbar(
                message = message,
                actionLabel = action?.label,
                icon = icon,
            )
        } finally {
            registeredKeys.remove(key)
        }

        if (result == SnackbarResult.ActionPerformed) {
            action?.onClick?.invoke()
        }
    }
}

internal val LocalClipySnackbarManager = staticCompositionLocalOf<ClipySnackbarManager> {
    error("ClipySnackbarManager is not provided.")
}

/** 현재 Snackbar Host에 연결된 Snackbar 요청 API를 반환합니다. */
@Composable
fun rememberClipySnackbarController(): ClipySnackbarController {
    val manager = LocalClipySnackbarManager.current

    return remember(manager) {
        ClipySnackbarController(manager)
    }
}

@Composable
internal fun rememberClipySnackbarManager(): ClipySnackbarManager {
    val hostState = remember { SnackbarHostState() }

    return remember(hostState) {
        ClipySnackbarManager(hostState)
    }
}
