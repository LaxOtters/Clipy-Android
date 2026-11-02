package com.laxotters.clipy.feature.session.browser.webview

import android.content.Context
import androidx.annotation.MainThread
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * 앱 프로세스에서 Safe Browsing 초기화를 한 번 시작하고, 끝난 뒤에 대기 중인 첫 페이지 로드를 실행합니다.
 * 기능을 지원하지 않거나 초기화에 실패해도 로드는 그대로 진행합니다.
 */
internal class SafeBrowsingStarter(
    private val isSupported: () -> Boolean = {
        WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)
    },
    private val start: (Context, onFinished: () -> Unit) -> Unit = { context, onFinished ->
        // 완료 callback은 UI thread에서 호출됩니다.
        WebViewCompat.startSafeBrowsing(context.applicationContext) { onFinished() }
    },
) {
    private var isReady = false
    private var isStarting = false
    private val pendingActions = mutableListOf<() -> Unit>()

    @MainThread
    fun runWhenReady(context: Context, action: () -> Unit) {
        if (isReady) {
            action()
            return
        }
        pendingActions += action
        if (isStarting) return
        isStarting = true
        if (isSupported()) {
            start(context, ::finish)
        } else {
            finish()
        }
    }

    private fun finish() {
        isReady = true
        isStarting = false
        val actions = pendingActions.toList()
        pendingActions.clear()
        actions.forEach { it() }
    }

    companion object {
        val Default = SafeBrowsingStarter()
    }
}
