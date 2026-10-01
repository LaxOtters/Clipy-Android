package com.laxotters.clipy.feature.session.browser.recovery

import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageErrorCause
import com.laxotters.clipy.feature.session.browser.SessionPageFailure
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.SessionRecoveryAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPageRecoveryPolicyTest {
    @Test
    fun connectionFailure_networkCapabilities_selectCauseWithoutProbe() {
        val expectations = mapOf(
            SessionNetworkAvailability.VALIDATED to SessionPageErrorCause.SERVER,
            SessionNetworkAvailability.UNAVAILABLE to SessionPageErrorCause.NO_NETWORK,
            SessionNetworkAvailability.NOT_VALIDATED to SessionPageErrorCause.NO_NETWORK,
            SessionNetworkAvailability.UNKNOWN to SessionPageErrorCause.UNKNOWN,
        )
        expectations.forEach { (network, cause) ->
            val recovery = SessionPageRecoveryPolicy.decide(failure().copy(network = network))
            assertEquals(cause, recovery?.cause)
            assertEquals(SessionRecoveryAction.TRY_AGAIN, recovery?.action)
        }
    }

    @Test
    fun unavailableFailure_validatedNetwork_neverOffersRetry() {
        val recovery =
            checkNotNull(SessionPageRecoveryPolicy.decide(failure().copy(error = SessionLoadError.UNAVAILABLE)))
        assertEquals(SessionPageErrorCause.UNAVAILABLE, recovery.cause)
        assertEquals(SessionRecoveryAction.GO_HOME, recovery.action)
    }

    @Test
    fun unknownFailure_noNetwork_remainsUnknown() {
        val recovery = SessionPageRecoveryPolicy.decide(
            failure().copy(error = SessionLoadError.UNKNOWN, network = SessionNetworkAvailability.UNAVAILABLE),
        )
        assertEquals(SessionPageErrorCause.UNKNOWN, recovery?.cause)
    }

    @Test
    fun visibleDocument_getFailure_offersSnackbarRetryForFailedUrl() {
        val recovery = checkNotNull(SessionPageRecoveryPolicy.decide(failure().copy(hasVisibleDocument = true)))
        assertFalse(recovery.showOverlay)
        assertEquals(SessionRecoveryAction.TRY_AGAIN, recovery.action)
        assertEquals("https://example.com/failed", recovery.navigation.url)
    }

    @Test
    fun visibleDocument_postOrUnavailableFailure_hasNoAction() {
        listOf(failure().copy(method = "POST"), failure().copy(error = SessionLoadError.UNAVAILABLE)).forEach {
            val recovery =
                checkNotNull(SessionPageRecoveryPolicy.decide(it.copy(hasVisibleDocument = true, canGoBack = true)))
            assertFalse(recovery.showOverlay)
            assertNull(recovery.action)
        }
    }

    @Test
    fun noDocument_postFailure_usesHistoryForBackOrHome() {
        val back = checkNotNull(SessionPageRecoveryPolicy.decide(failure().copy(method = "POST", canGoBack = true)))
        val home = checkNotNull(SessionPageRecoveryPolicy.decide(failure().copy(method = "POST", canGoBack = false)))
        assertTrue(back.showOverlay)
        assertTrue(home.showOverlay)
        assertEquals(SessionRecoveryAction.GO_BACK, back.action)
        assertEquals(SessionRecoveryAction.GO_HOME, home.action)
        assertEquals(SessionPageErrorCause.SERVER, back.cause)
    }

    private fun failure() = SessionPageFailure(
        navigation = SessionPageNavigation(1, 1, "https://example.com/failed"),
        error = SessionLoadError.CONNECTION,
        method = "GET",
        hasVisibleDocument = false,
        canGoBack = false,
        canGoForward = false,
        network = SessionNetworkAvailability.VALIDATED,
    )
}
