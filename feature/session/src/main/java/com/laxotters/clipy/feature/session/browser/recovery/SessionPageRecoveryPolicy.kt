package com.laxotters.clipy.feature.session.browser.recovery

import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageFailure
import com.laxotters.clipy.feature.session.browser.SessionPageRecovery
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction
import com.laxotters.clipy.feature.session.browser.isSecurity

internal object SessionPageRecoveryPolicy {
    fun decide(failure: SessionPageFailure): SessionPageRecovery {
        val cause = when (failure.error) {
            SessionLoadError.SSL -> SessionPageErrorCause.SSL
            SessionLoadError.UNSAFE_SITE -> SessionPageErrorCause.UNSAFE_SITE
            SessionLoadError.UNAVAILABLE -> SessionPageErrorCause.UNAVAILABLE
            SessionLoadError.UNKNOWN -> SessionPageErrorCause.UNKNOWN
            SessionLoadError.CONNECTION -> when (failure.network) {
                SessionNetworkAvailability.VALIDATED -> SessionPageErrorCause.SERVER
                SessionNetworkAvailability.NOT_VALIDATED,
                SessionNetworkAvailability.UNAVAILABLE,
                -> SessionPageErrorCause.NO_NETWORK
                SessionNetworkAvailability.UNKNOWN -> SessionPageErrorCause.UNKNOWN
            }
        }
        val isSecurity = cause.isSecurity
        val canRetry = !isSecurity &&
            cause != SessionPageErrorCause.UNAVAILABLE &&
            failure.method.equals("GET", ignoreCase = true)
        val action = when {
            canRetry -> SessionRecoveryAction.TRY_AGAIN
            // 보안 차단은 이전 페이지가 남아 있어도 차단 화면으로 알리고 그 페이지로 돌려보냅니다.
            isSecurity && failure.hasVisibleDocument -> SessionRecoveryAction.RETURN_TO_PAGE
            failure.hasVisibleDocument -> null
            failure.canGoBack -> SessionRecoveryAction.GO_BACK
            else -> SessionRecoveryAction.GO_HOME
        }
        return SessionPageRecovery(
            navigation = failure.navigation,
            cause = cause,
            action = action,
            showOverlay = isSecurity || !failure.hasVisibleDocument,
        )
    }

    /** 표시할 문서가 없을 때 WebView 기본 오류 문서가 Compose 오류 화면보다 먼저 보이지 않게 숨깁니다. */
    fun hidesWebView(failure: SessionPageFailure): Boolean = !failure.hasVisibleDocument
}
