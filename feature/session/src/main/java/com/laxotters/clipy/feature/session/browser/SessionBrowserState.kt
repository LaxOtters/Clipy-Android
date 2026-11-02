package com.laxotters.clipy.feature.session.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.laxotters.clipy.feature.session.browser.recovery.SessionPageRecoveryPolicy
import com.laxotters.clipy.feature.session.browser.webview.PageInfo
import com.laxotters.clipy.feature.session.browser.webview.SessionWebViewController
import com.laxotters.clipy.feature.session.util.formatUrlLabel

/** 현재 WebView의 탐색 상태와 복구 액션을 관리합니다. */
@Stable
internal class SessionBrowserState(
    private val initialUrl: () -> String?,
    internal val controller: SessionWebViewController = SessionWebViewController(),
) {
    private var pageInfo: PageInfo? by mutableStateOf(null)
    var pageRecovery: SessionPageRecovery? by mutableStateOf(null)
        private set
    private var currentNavigation: SessionPageNavigation? = null

    /** 값이 바뀌면 Browser가 새 WebView 인스턴스를 만듭니다. */
    internal var webViewGeneration: Int by mutableIntStateOf(0)
        private set

    /** renderer 종료 뒤 새 WebView가 처음 열 URL입니다. 없으면 Session의 시작 URL을 엽니다. */
    internal var reopenUrl: String? by mutableStateOf(null)
        private set

    val currentUrl: String get() = pageInfo?.url.orEmpty()
    val urlLabel: String get() = formatUrlLabel(pageInfo?.url ?: initialUrl().orEmpty())
    val canGoBack: Boolean
        get() {
            val recovery = pageRecovery
            // 차단 화면 뒤에 남은 이전 페이지로 돌아가는 것도 뒤로가기로 처리합니다.
            if (recovery?.isSecurityBlock == true) return recovery.action != SessionRecoveryAction.GO_HOME
            return pageInfo?.canGoBack == true
        }
    val canGoForward: Boolean get() = pageInfo?.canGoForward == true
    val canRefresh: Boolean get() = initialUrl() != null

    fun goBack() {
        val recovery = pageRecovery
        // 차단된 탐색은 이전 페이지를 덮지 않았으므로 WebView history를 이동하지 않고 화면만 닫습니다.
        if (recovery?.action == SessionRecoveryAction.RETURN_TO_PAGE) {
            returnToPage()
            return
        }
        controller.goBack()
    }

    fun goForward() = controller.goForward()

    fun reload() {
        val recovery = pageRecovery
        // 종료된 WebView는 새로고침할 수 없어 새 WebView로 다시 엽니다.
        if (recovery?.cause == SessionPageErrorCause.RENDERER_GONE) {
            if (recovery.action == SessionRecoveryAction.REOPEN) {
                performRecovery(recovery.navigation) {}
            }
            return
        }
        // 차단된 URL은 다시 요청하지 않고, 이전 페이지가 남아 있을 때만 그 페이지를 새로고침합니다.
        if (recovery?.isSecurityBlock == true) {
            if (recovery.action != SessionRecoveryAction.RETURN_TO_PAGE) return
            returnToPage()
        }
        controller.reload()
    }

    internal fun onRenderProcessGone(webViewId: Long) {
        // 새 WebView에는 이전 방문 기록이 없으므로 뒤로·앞으로 이동을 막습니다.
        pageInfo = pageInfo?.copy(canGoBack = false, canGoForward = false)
        val url = pageInfo?.url?.takeIf { it.isNotBlank() } ?: initialUrl()
        pageRecovery = SessionPageRecovery(
            navigation = SessionPageNavigation(webViewId, currentNavigation?.navigationId ?: 0L, url.orEmpty()),
            cause = SessionPageErrorCause.RENDERER_GONE,
            action = if (url != null) SessionRecoveryAction.REOPEN else SessionRecoveryAction.GO_HOME,
            showOverlay = true,
        )
        currentNavigation = null
    }

    internal fun onPageLoadChanged(load: SessionPageLoad) {
        val navigation = load.navigation
        val isTerminal = load is SessionPageLoad.Ready || load is SessionPageLoad.Stopped
        if (isTerminal && navigation != currentNavigation) return
        if (navigation.webViewId != currentNavigation?.webViewId && !isReopening(navigation)) {
            pageInfo = null
            pageRecovery = null
        }
        currentNavigation = navigation
        pageRecovery = when (load) {
            // 본문이 준비될 때까지 실패 대상과 오류 화면을 유지합니다.
            is SessionPageLoad.Loading ->
                pageRecovery?.takeIf { it.showOverlay }?.copy(isRunning = true)

            is SessionPageLoad.Failed -> {
                val failure = load.failure
                pageInfo = pageInfo?.copy(canGoBack = failure.canGoBack, canGoForward = failure.canGoForward)
                    ?: PageInfo(navigation.url, failure.canGoBack, failure.canGoForward)
                SessionPageRecoveryPolicy.decide(failure)
            }

            is SessionPageLoad.Ready -> null
            is SessionPageLoad.Stopped -> pageRecovery?.copy(isRunning = false)
        }
    }

    internal fun performRecovery(navigation: SessionPageNavigation, onHomeRequested: () -> Unit) {
        val recovery = pageRecovery ?: return
        if (recovery.navigation != navigation || recovery.isRunning || recovery.action == null) return
        val running = recovery.copy(isRunning = true)
        pageRecovery = running
        val accepted = when (recovery.action) {
            SessionRecoveryAction.TRY_AGAIN -> controller.retry(recovery.navigation)
            SessionRecoveryAction.GO_BACK -> controller.goBack(recovery.navigation.webViewId)
            SessionRecoveryAction.GO_HOME -> {
                onHomeRequested()
                true
            }
            SessionRecoveryAction.REOPEN -> {
                reopenUrl = recovery.navigation.url
                webViewGeneration += 1
                true
            }
            SessionRecoveryAction.RETURN_TO_PAGE -> {
                returnToPage()
                true
            }
        }
        if (!accepted && pageRecovery == running) {
            pageRecovery = recovery
        }
    }

    private fun returnToPage() {
        pageRecovery = null
        // 차단된 탐색의 완료 callback이 바꾼 주소 대신 WebView가 실제로 표시 중인 페이지를 반영합니다.
        controller.currentPage()?.let { pageInfo = it }
    }

    /** 새 WebView가 본문을 표시할 때까지 renderer 종료 화면을 유지합니다. */
    private fun isReopening(navigation: SessionPageNavigation): Boolean {
        val recovery = pageRecovery ?: return false
        return recovery.cause == SessionPageErrorCause.RENDERER_GONE &&
            recovery.isRunning &&
            navigation.webViewId != recovery.navigation.webViewId
    }

    internal fun dismissRecoverySnackbar() {
        if (pageRecovery?.showOverlay == false) {
            pageRecovery = null
        }
    }

    internal fun updatePageInfo(pageInfo: PageInfo) {
        this.pageInfo = pageInfo
    }

    internal fun reset() {
        pageInfo = null
        pageRecovery = null
        currentNavigation = null
        reopenUrl = null
    }
}

/** 실제 WebView와 해당 인스턴스의 본문 탐색을 함께 식별합니다. */
internal data class SessionPageNavigation(
    val webViewId: Long,
    val navigationId: Long,
    val url: String,
)

/** 현재 본문 탐색의 진행 상태를 나타냅니다. */
internal sealed interface SessionPageLoad {
    val navigation: SessionPageNavigation

    data class Loading(
        override val navigation: SessionPageNavigation,
    ) : SessionPageLoad

    // 문서나 history 근거가 바뀌면 같은 탐색의 실패를 다시 전달합니다.
    data class Failed(
        val failure: SessionPageFailure,
    ) : SessionPageLoad {
        override val navigation: SessionPageNavigation get() = failure.navigation
    }

    // 전체 로드 완료가 아닌 본문 표시 준비를 뜻합니다.
    data class Ready(
        override val navigation: SessionPageNavigation,
    ) : SessionPageLoad

    // 외부 앱 이동이나 본문 다운로드로 종료된 탐색입니다.
    data class Stopped(
        override val navigation: SessionPageNavigation,
    ) : SessionPageLoad
}

internal enum class SessionLoadError {
    CONNECTION,
    UNAVAILABLE,
    SSL,
    UNSAFE_SITE,
    UNKNOWN,
}

internal enum class SessionNetworkAvailability {
    VALIDATED,
    NOT_VALIDATED,
    UNAVAILABLE,
    UNKNOWN,
}

internal data class SessionPageFailure(
    val navigation: SessionPageNavigation,
    val error: SessionLoadError,
    val method: String,
    val hasVisibleDocument: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val network: SessionNetworkAvailability,
)

internal enum class SessionPageErrorCause {
    NO_NETWORK,
    SERVER,
    UNKNOWN,
    UNAVAILABLE,
    RENDERER_GONE,
    SSL,
    UNSAFE_SITE,
}

internal enum class SessionRecoveryAction {
    TRY_AGAIN,
    GO_BACK,
    GO_HOME,
    REOPEN,

    // 차단 화면 뒤에 그대로 남은 이전 페이지를 다시 보여 줍니다. 화면 문구는 Go back입니다.
    RETURN_TO_PAGE,
}

internal data class SessionPageRecovery(
    val navigation: SessionPageNavigation,
    val cause: SessionPageErrorCause,
    val action: SessionRecoveryAction?,
    val showOverlay: Boolean,
    val isRunning: Boolean = false,
) {
    val isSecurityBlock: Boolean get() = cause.isSecurity
}

/** 재시도·계속 진행 없이 차단 화면으로만 안내하는 보안 원인입니다. */
internal val SessionPageErrorCause.isSecurity: Boolean
    get() = when (this) {
        SessionPageErrorCause.SSL,
        SessionPageErrorCause.UNSAFE_SITE,
        -> true

        SessionPageErrorCause.NO_NETWORK,
        SessionPageErrorCause.SERVER,
        SessionPageErrorCause.UNKNOWN,
        SessionPageErrorCause.UNAVAILABLE,
        SessionPageErrorCause.RENDERER_GONE,
        -> false
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
