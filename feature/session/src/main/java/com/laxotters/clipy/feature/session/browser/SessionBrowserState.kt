package com.laxotters.clipy.feature.session.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.laxotters.clipy.feature.session.browser.webview.PageInfo
import com.laxotters.clipy.feature.session.browser.webview.SessionWebViewController
import com.laxotters.clipy.feature.session.util.formatUrlLabel

/** 현재 WebView의 표시 상태와 탐색 명령을 소유하며 영속 복원 데이터는 보관하지 않습니다. */
@Stable
internal class SessionBrowserState(
    private val initialUrl: () -> String?,
    internal val controller: SessionWebViewController = SessionWebViewController(),
) {
    private var pageInfo: PageInfo? by mutableStateOf(null)

    val currentUrl: String get() = pageInfo?.url.orEmpty()
    val urlLabel: String get() = formatUrlLabel(pageInfo?.url ?: initialUrl().orEmpty())
    val canGoBack: Boolean get() = pageInfo?.canGoBack == true
    val canGoForward: Boolean get() = pageInfo?.canGoForward == true
    val canRefresh: Boolean get() = initialUrl() != null

    fun goBack() = controller.goBack()

    fun goForward() = controller.goForward()

    fun reload() = controller.reload()

    internal fun updatePageInfo(pageInfo: PageInfo) {
        this.pageInfo = pageInfo
    }

    internal fun reset() {
        pageInfo = null
    }
}

@Composable
internal fun rememberSessionBrowserState(
    sessionId: String,
    initialUrl: String?,
): SessionBrowserState {
    val currentInitialUrl = rememberUpdatedState(initialUrl)
    return remember(sessionId) {
        SessionBrowserState(initialUrl = { currentInitialUrl.value })
    }
}
