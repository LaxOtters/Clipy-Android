package com.laxotters.clipy.feature.session.browser.recovery

import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageFailure
import com.laxotters.clipy.feature.session.browser.SessionPageRecovery
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction

internal object SessionPageRecoveryPolicy {
    fun decide(failure: SessionPageFailure): SessionPageRecovery? {
        val cause = when (failure.error) {
            SessionLoadError.SECURITY -> return null
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
        val canRetry = cause != SessionPageErrorCause.UNAVAILABLE && failure.method.equals("GET", ignoreCase = true)
        val action = when {
            canRetry -> SessionRecoveryAction.TRY_AGAIN
            failure.hasVisibleDocument -> null
            failure.canGoBack -> SessionRecoveryAction.GO_BACK
            else -> SessionRecoveryAction.GO_HOME
        }
        return SessionPageRecovery(
            navigation = failure.navigation,
            cause = cause,
            action = action,
            showOverlay = !failure.hasVisibleDocument,
        )
    }
}
