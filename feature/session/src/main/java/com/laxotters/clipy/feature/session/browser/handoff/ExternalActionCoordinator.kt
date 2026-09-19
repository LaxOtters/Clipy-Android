package com.laxotters.clipy.feature.session.browser.handoff

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.laxotters.clipy.feature.session.R
import com.laxotters.clipy.feature.session.browser.dialog.SessionExternalActionDialogManager
import com.laxotters.clipy.feature.session.browser.dialog.SessionExternalActionDialogRequest
import com.laxotters.clipy.feature.session.browser.webview.ErrorMessage
import com.laxotters.clipy.feature.session.browser.webview.navigation.WebNavigationPolicy

/** 외부 앱 이동과 브라우저 다운로드 fallback의 확인·실행·실패 피드백을 연결합니다. */
internal class ExternalActionCoordinator(
    private val dialogManager: SessionExternalActionDialogManager,
    private val appHandler: ExternalAppHandler,
    private val onErrorMessage: (ErrorMessage) -> Unit,
    private var strings: ExternalActionStrings,
) {
    private val dialogOwner = Any()
    private var isClosed = false

    @MainThread
    fun updateStrings(strings: ExternalActionStrings) {
        this.strings = strings
    }

    @MainThread
    fun requestExternalApp(uriString: String) {
        if (isClosed) return

        val intent = appHandler.createIntent(uriString)
        if (intent == null) {
            reportExternalAppFailure()
            return
        }
        val appName = appHandler.resolveAppName(intent)
        dialogManager.show(
            owner = dialogOwner,
            request = SessionExternalActionDialogRequest(
                title = strings.formatExternalAppTitle(appName),
                description = strings.externalAppDescription,
                continueLabel = strings.continueAction,
                cancelLabel = strings.cancelAction,
                onContinue = {
                    if (!appHandler.launch(intent)) {
                        reportExternalAppFailure()
                    }
                },
            ),
        )
    }

    @MainThread
    fun requestOpenInBrowser(url: String) {
        if (isClosed) return
        if (!WebNavigationPolicy.isHttpUrlWithHost(url)) {
            reportUnsupportedDownload()
            return
        }

        val intent = appHandler.createIntent(url)
        if (intent == null) {
            reportUnsupportedDownload()
            return
        }
        dialogManager.show(
            owner = dialogOwner,
            request = SessionExternalActionDialogRequest(
                title = strings.downloadTitle,
                description = strings.downloadDescription,
                continueLabel = strings.continueAction,
                cancelLabel = strings.cancelAction,
                onContinue = {
                    if (!appHandler.launch(intent)) {
                        reportExternalAppFailure()
                    }
                },
            ),
        )
    }

    @MainThread
    fun reportUnsupportedDownload() {
        if (isClosed) return

        onErrorMessage(
            ErrorMessage(
                message = strings.unsupportedDownloadMessage,
                key = UNSUPPORTED_DOWNLOAD_MESSAGE_KEY,
            ),
        )
    }

    /** 실제 WebView 해제 시 자신이 표시한 확인 Dialog만 제거합니다. */
    @MainThread
    fun close() {
        if (isClosed) return

        isClosed = true
        dialogManager.cancelOwner(dialogOwner)
    }

    private fun reportExternalAppFailure() {
        if (isClosed) return

        onErrorMessage(
            ErrorMessage(
                message = strings.externalAppFailureMessage,
                key = EXTERNAL_APP_FAILURE_MESSAGE_KEY,
            ),
        )
    }

    private companion object {
        const val EXTERNAL_APP_FAILURE_MESSAGE_KEY = "session_external_app_failure"
        const val UNSUPPORTED_DOWNLOAD_MESSAGE_KEY = "session_unsupported_download"
    }
}

internal data class ExternalActionStrings(
    val externalAppTitle: String,
    val namedAppTitle: String,
    val externalAppDescription: String,
    val downloadTitle: String,
    val downloadDescription: String,
    val cancelAction: String,
    val continueAction: String,
    val externalAppFailureMessage: String,
    val unsupportedDownloadMessage: String,
) {
    fun formatExternalAppTitle(appName: String?): String =
        if (appName == null) {
            externalAppTitle
        } else {
            namedAppTitle.format(appName)
        }
}

@Composable
internal fun externalActionStrings(): ExternalActionStrings =
    ExternalActionStrings(
        externalAppTitle = stringResource(R.string.session_external_app_dialog_title),
        namedAppTitle = stringResource(R.string.session_external_app_dialog_named_title),
        externalAppDescription = stringResource(R.string.session_external_app_dialog_description),
        downloadTitle = stringResource(R.string.session_download_dialog_title),
        downloadDescription = stringResource(R.string.session_download_dialog_description),
        cancelAction = stringResource(R.string.session_external_action_cancel),
        continueAction = stringResource(R.string.session_external_action_continue),
        externalAppFailureMessage = stringResource(R.string.session_external_app_failure_message),
        unsupportedDownloadMessage = stringResource(R.string.session_download_unsupported_message),
    )
