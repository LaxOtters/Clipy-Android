package com.laxotters.clipy.feature.session.browser.webview.client

import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.laxotters.clipy.feature.session.browser.dialog.SessionWebDialogCoordinator

/** WebView JavaScript 요청을 Session Dialog coordinator에 전달합니다. */
internal class SessionWebChromeClient(
    internal val dialogCoordinator: SessionWebDialogCoordinator,
) : WebChromeClient() {
    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean {
        dialogCoordinator.showAlert(
            url = url,
            message = message,
            onConfirm = result::confirm,
            onCancel = result::cancel,
        )
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean {
        dialogCoordinator.showConfirm(
            url = url,
            message = message,
            onConfirm = result::confirm,
            onCancel = result::cancel,
        )
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean {
        dialogCoordinator.showPrompt(
            url = url,
            message = message,
            defaultValue = defaultValue,
            onConfirm = result::confirm,
            onCancel = result::cancel,
        )
        return true
    }

    override fun onJsBeforeUnload(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean {
        dialogCoordinator.showBeforeUnload(
            url = url,
            message = message,
            onLeave = result::confirm,
            onStay = result::cancel,
        )
        return true
    }
}
