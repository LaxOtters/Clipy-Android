package com.laxotters.clipy.feature.session.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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

    val currentUrl: String get() = pageInfo?.url.orEmpty()
    val urlLabel: String get() = formatUrlLabel(pageInfo?.url ?: initialUrl().orEmpty())
    val canGoBack: Boolean get() = pageInfo?.canGoBack == true
    val canGoForward: Boolean get() = pageInfo?.canGoForward == true
    val canRefresh: Boolean get() = initialUrl() != null

    fun goBack() = controller.goBack()

    fun goForward() = controller.goForward()

    fun reload() = controller.reload()

    internal fun onPageLoadChanged(load: SessionPageLoad) {
        val navigation = load.navigation
        val isTerminal = load is SessionPageLoad.Ready || load is SessionPageLoad.Stopped
        if (isTerminal && navigation != currentNavigation) return
        if (navigation.webViewId != currentNavigation?.webViewId) {
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
        }
        if (!accepted && pageRecovery == running) {
            pageRecovery = recovery
        }
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
    SECURITY,
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
}

internal enum class SessionRecoveryAction {
    TRY_AGAIN,
    GO_BACK,
    GO_HOME,
}

internal data class SessionPageRecovery(
    val navigation: SessionPageNavigation,
    val cause: SessionPageErrorCause,
    val action: SessionRecoveryAction?,
    val showOverlay: Boolean,
    val isRunning: Boolean = false,
)

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
