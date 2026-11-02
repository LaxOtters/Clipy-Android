package com.laxotters.clipy.feature.session.browser.webview

import androidx.annotation.MainThread

/** WebView 해제를 한 번 수행하고, 일부 정리가 실패해도 남은 자원을 정리합니다. */
internal class SessionWebViewRelease(
    private val closeDialog: () -> Unit,
    private val closeExternalAction: () -> Unit,
    private val detachController: () -> Unit,
    private val releaseCallbacks: () -> Unit,
    private val removeFromParent: () -> Unit,
    private val destroy: () -> Unit,
) {
    private var isReleased = false

    @MainThread
    fun release() {
        if (isReleased) return

        isReleased = true
        var failure: Throwable? = null
        val releaseSteps = listOf(
            closeDialog,
            closeExternalAction,
            detachController,
            releaseCallbacks,
            removeFromParent,
            destroy,
        )
        for (step in releaseSteps) {
            try {
                step()
            } catch (error: Throwable) {
                val original = failure
                if (original == null) {
                    failure = error
                } else if (original !== error) {
                    original.addSuppressed(error)
                }
            }
        }
        failure?.let { throw it }
    }
}
