package com.laxotters.clipy.feature.session.browser.recovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laxotters.clipy.core.designsystem.component.ClipyTextAction
import com.laxotters.clipy.core.designsystem.component.error.ClipyErrorOverlay
import com.laxotters.clipy.core.designsystem.component.snackbar.ClipySnackbarIcon
import com.laxotters.clipy.core.designsystem.component.snackbar.rememberClipySnackbarController
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.feature.session.R
import com.laxotters.clipy.feature.session.browser.SessionBrowserState
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.SessionPageRecovery
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction

/** Browser의 복구 상태를 오류 화면과 Snackbar에 연결합니다. */
@Composable
internal fun SessionRecoveryContent(state: SessionBrowserState, onHomeRequested: () -> Unit) {
    val recovery = state.pageRecovery
    val snackbar = rememberClipySnackbarController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnHomeRequested by rememberUpdatedState(onHomeRequested)
    val message = stringResource(R.string.session_page_load_failure_message)
    val retryLabel = stringResource(R.string.session_recovery_try_again)

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.dismissRecoverySnackbar() }

    LaunchedEffect(recovery, message, retryLabel) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@LaunchedEffect
        if (recovery == null || recovery.showOverlay || recovery.isRunning) return@LaunchedEffect
        snackbar.showSnackbar(
            message = message,
            icon = if (recovery.action == null) ClipySnackbarIcon.Error else null,
            action = recovery.action?.let {
                ClipyTextAction(retryLabel) { state.performRecovery(recovery.navigation, currentOnHomeRequested) }
            },
            key = "session_page_load_failure",
        )
    }
    if (recovery?.showOverlay == true) {
        SessionRecoveryOverlay(recovery, { state.performRecovery(it, currentOnHomeRequested) })
    }
}

@Composable
internal fun SessionRecoveryOverlay(
    recovery: SessionPageRecovery,
    onRecoveryClick: (SessionPageNavigation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, description) = when (recovery.cause) {
        SessionPageErrorCause.NO_NETWORK ->
            R.string.session_recovery_no_network_title to
                R.string.session_recovery_no_network_description

        SessionPageErrorCause.SERVER ->
            R.string.session_recovery_server_title to
                R.string.session_recovery_server_description

        SessionPageErrorCause.UNKNOWN ->
            R.string.session_recovery_unknown_title to
                R.string.session_recovery_unknown_description

        SessionPageErrorCause.UNAVAILABLE ->
            R.string.session_recovery_unavailable_title to
                R.string.session_recovery_unavailable_description

        SessionPageErrorCause.RENDERER_GONE ->
            R.string.session_recovery_renderer_gone_title to
                R.string.session_recovery_renderer_gone_description
    }
    val label = when (recovery.action) {
        SessionRecoveryAction.TRY_AGAIN -> R.string.session_recovery_try_again
        SessionRecoveryAction.GO_BACK -> R.string.session_recovery_go_back
        SessionRecoveryAction.GO_HOME -> R.string.session_recovery_go_home
        SessionRecoveryAction.REOPEN -> R.string.session_recovery_reopen
        null -> null
    }
    ClipyErrorOverlay(
        title = stringResource(title),
        description = stringResource(description),
        modifier = modifier,
        action = label?.let {
            ClipyTextAction(stringResource(it)) {
                onRecoveryClick(recovery.navigation)
            }
        },
        actionEnabled = !recovery.isRunning,
    )
}

@Preview(name = "Recovery", showBackground = true, widthDp = 390, heightDp = 520)
@Preview(name = "Recovery large text", showBackground = true, widthDp = 360, heightDp = 520, fontScale = 1.3f)
@Composable
private fun SessionRecoveryPreview(
    @PreviewParameter(SessionRecoveryPreviewProvider::class) recovery: SessionPageRecovery,
) {
    ClipyTheme {
        SessionRecoveryOverlay(recovery, {})
    }
}

internal class SessionRecoveryPreviewProvider : PreviewParameterProvider<SessionPageRecovery> {
    override val values: Sequence<SessionPageRecovery> = sequence {
        val navigation = SessionPageNavigation(1, 1, "https://example.com")
        SessionPageErrorCause.entries.forEach { cause ->
            val actions = when (cause) {
                SessionPageErrorCause.UNAVAILABLE ->
                    listOf(SessionRecoveryAction.GO_BACK, SessionRecoveryAction.GO_HOME)

                SessionPageErrorCause.RENDERER_GONE ->
                    listOf(SessionRecoveryAction.REOPEN, SessionRecoveryAction.GO_HOME)

                else -> SessionRecoveryAction.entries - SessionRecoveryAction.REOPEN
            }
            actions.forEach { action -> yield(SessionPageRecovery(navigation, cause, action, true)) }
        }
        yield(
            SessionPageRecovery(
                navigation,
                SessionPageErrorCause.RENDERER_GONE,
                SessionRecoveryAction.REOPEN,
                true,
                true,
            ),
        )
        yield(
            SessionPageRecovery(
                navigation,
                SessionPageErrorCause.NO_NETWORK,
                SessionRecoveryAction.TRY_AGAIN,
                true,
                true,
            ),
        )
    }
}
