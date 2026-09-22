package com.laxotters.clipy.feature.session.browser.webview

import com.laxotters.clipy.feature.session.browser.SessionPageNavigation

/** 한 WebView의 탐색 상태와 표시 가능한 문서를 추적합니다. */
internal class SessionDocumentTracker(
    private val webViewId: Long,
) {
    var navigation: SessionPageNavigation? = null
        private set
    var hasVisibleDocument: Boolean = false
        private set
    private var hasFailed = false
    private var hasCommitted = false
    private var isStopped = false
    var hasFinished = false
        private set
    private var nextNavigationId = 0L

    val isSuccessful: Boolean
        get() = hasCommitted && hasFinished && !hasFailed && !isStopped

    val canShowDocument: Boolean
        get() = hasCommitted && !hasFailed && !isStopped

    fun start(url: String): SessionPageNavigation {
        nextNavigationId += 1
        hasFailed = false
        hasCommitted = false
        hasFinished = false
        isStopped = false
        return SessionPageNavigation(webViewId, nextNavigationId, url).also { navigation = it }
    }

    fun fail(url: String, discardVisibleDocument: Boolean = false): Boolean {
        if (isStopped || navigation?.url != url) return false
        hasFailed = true
        // 새 문서로 전환된 뒤 실패하면 이전 문서가 남아 있다고 보지 않습니다.
        if (hasCommitted || discardVisibleDocument) {
            hasVisibleDocument = false
        }
        return true
    }

    fun commit(url: String): Boolean {
        if (isStopped || navigation?.url != url) return false
        hasCommitted = true
        if (hasFailed) {
            hasVisibleDocument = false
        }
        return true
    }

    fun markDocumentVisible(navigation: SessionPageNavigation): Boolean {
        if (this.navigation != navigation || !canShowDocument) return false
        hasVisibleDocument = true
        return true
    }

    fun finish(url: String): Boolean {
        if (isStopped || navigation?.url != url) return false
        hasFinished = true
        return true
    }

    /** 이미 표시 중인 정상 문서는 유지하고, 본문을 기다리던 탐색만 종료합니다. */
    fun stop(): SessionPageNavigation? {
        if (isStopped || canShowDocument) return null
        val current = navigation ?: return null
        isStopped = true
        return current
    }

    fun close() {
        navigation = null
        hasVisibleDocument = false
        hasFailed = false
        hasCommitted = false
        hasFinished = false
        isStopped = false
    }
}
