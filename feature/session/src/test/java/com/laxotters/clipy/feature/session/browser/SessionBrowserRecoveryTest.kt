package com.laxotters.clipy.feature.session.browser

import com.laxotters.clipy.feature.session.browser.webview.PageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBrowserRecoveryTest {
    private val state = SessionBrowserState({ "https://example.com" })

    @Test
    fun retryFromOverlay_keepsOverlayUntilRetryDocumentIsReady() {
        fail(navigation(1))
        val retry = navigation(2)
        state.onPageLoadChanged(SessionPageLoad.Loading(retry))
        assertTrue(state.pageRecovery!!.showOverlay)
        assertTrue(state.pageRecovery!!.isRunning)
        state.onPageLoadChanged(SessionPageLoad.Ready(retry))
        assertNull(state.pageRecovery)
    }

    @Test
    fun retryFailure_updatesCauseAndEnablesNextRetry() {
        fail(navigation(1))
        val retry = navigation(2)
        state.onPageLoadChanged(SessionPageLoad.Loading(retry))
        fail(retry, network = SessionNetworkAvailability.UNAVAILABLE)
        assertEquals(SessionPageErrorCause.NO_NETWORK, state.pageRecovery!!.cause)
        assertFalse(state.pageRecovery!!.isRunning)
    }

    @Test
    fun stoppedRetry_enablesRetryWithoutClosingOverlay() {
        fail(navigation(1))
        val retry = navigation(2)
        state.onPageLoadChanged(SessionPageLoad.Loading(retry))
        state.onPageLoadChanged(SessionPageLoad.Stopped(retry))
        assertTrue(state.pageRecovery!!.showOverlay)
        assertFalse(state.pageRecovery!!.isRunning)
    }

    @Test
    fun navigationAway_keepsFailureTargetAndRejectsActionUntilReady() {
        val failed = navigation(1)
        fail(failed)
        val previous = navigation(2).copy(url = "https://example.com/previous")
        state.onPageLoadChanged(SessionPageLoad.Loading(previous))
        assertEquals(failed, state.pageRecovery!!.navigation)
        state.performRecovery(failed) { error("Navigation must keep the action disabled") }
        assertTrue(state.pageRecovery!!.isRunning)
        state.onPageLoadChanged(SessionPageLoad.Stopped(failed))
        state.onPageLoadChanged(SessionPageLoad.Ready(failed))
        assertTrue(state.pageRecovery!!.isRunning)
        state.onPageLoadChanged(SessionPageLoad.Ready(previous))
        assertNull(state.pageRecovery)
    }

    @Test
    fun repeatedHomeAction_isAcceptedOnce() {
        val target = navigation(1)
        fail(target, error = SessionLoadError.UNAVAILABLE)
        var calls = 0
        repeat(3) { state.performRecovery(target) { calls++ } }
        assertEquals(1, calls)
    }

    @Test
    fun retryWithoutAttachedWebView_enablesRetryAgain() {
        val target = navigation(1)
        fail(target)
        state.performRecovery(target) { error("GET retry must not request Home") }
        assertFalse(state.pageRecovery!!.isRunning)
    }

    @Test
    fun replacementWebView_discardsPreviousPageAndRecovery() {
        val old = navigation(1)
        fail(old)
        state.updatePageInfo(PageInfo("https://example.com/old", true, true))
        state.onPageLoadChanged(SessionPageLoad.Loading(SessionPageNavigation(2, 1, "https://example.com/new")))
        assertNull(state.pageRecovery)
        assertFalse(state.canGoBack)
        state.performRecovery(old) { error("Old action must not navigate") }
        assertNull(state.pageRecovery)
    }

    @Test
    fun background_dismissesSnackbarButKeepsOverlay() {
        fail(navigation(1), hasVisibleDocument = true)
        assertFalse(state.pageRecovery!!.showOverlay)
        state.dismissRecoverySnackbar()
        assertNull(state.pageRecovery)
        fail(navigation(2))
        state.dismissRecoverySnackbar()
        assertTrue(state.pageRecovery!!.showOverlay)
    }

    @Test
    fun rendererGone_offersReopenForLastPageAndBlocksHistoryNavigation() {
        state.onPageLoadChanged(SessionPageLoad.Loading(SessionPageNavigation(1, 1, "https://example.com/page")))
        state.updatePageInfo(PageInfo("https://example.com/page", true, true))
        state.onRenderProcessGone(1)
        val recovery = state.pageRecovery!!
        assertEquals(SessionPageErrorCause.RENDERER_GONE, recovery.cause)
        assertEquals(SessionRecoveryAction.REOPEN, recovery.action)
        assertEquals("https://example.com/page", recovery.navigation.url)
        assertTrue(recovery.showOverlay)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
    }

    @Test
    fun rendererGoneWithoutAnyUrl_offersHome() {
        val state = SessionBrowserState({ null })
        state.onRenderProcessGone(1)
        assertEquals(SessionRecoveryAction.GO_HOME, state.pageRecovery!!.action)
    }

    @Test
    fun reopen_createsOneNewWebViewAndKeepsOverlayUntilItIsReady() {
        state.onRenderProcessGone(1)
        val target = state.pageRecovery!!.navigation
        repeat(3) { state.performRecovery(target) { error("Reopen must not request Home") } }
        assertEquals(1, state.webViewGeneration)
        assertEquals("https://example.com", state.reopenUrl)
        val reopened = SessionPageNavigation(2, 1, "https://example.com")
        state.onPageLoadChanged(SessionPageLoad.Loading(reopened))
        assertEquals(SessionPageErrorCause.RENDERER_GONE, state.pageRecovery!!.cause)
        assertTrue(state.pageRecovery!!.isRunning)
        state.onPageLoadChanged(SessionPageLoad.Ready(reopened))
        assertNull(state.pageRecovery)
    }

    @Test
    fun reopenLoadFailure_followsPageLoadPolicy() {
        state.onRenderProcessGone(1)
        state.performRecovery(state.pageRecovery!!.navigation) {}
        val reopened = SessionPageNavigation(2, 1, "https://example.com")
        state.onPageLoadChanged(SessionPageLoad.Loading(reopened))
        fail(reopened, network = SessionNetworkAvailability.UNAVAILABLE)
        assertEquals(SessionPageErrorCause.NO_NETWORK, state.pageRecovery!!.cause)
        assertEquals(SessionRecoveryAction.TRY_AGAIN, state.pageRecovery!!.action)
    }

    @Test
    fun repeatedRendererGone_offersReopenAgain() {
        state.onRenderProcessGone(1)
        state.performRecovery(state.pageRecovery!!.navigation) {}
        state.onPageLoadChanged(SessionPageLoad.Loading(SessionPageNavigation(2, 1, "https://example.com")))
        state.onRenderProcessGone(2)
        assertEquals(SessionRecoveryAction.REOPEN, state.pageRecovery!!.action)
        assertFalse(state.pageRecovery!!.isRunning)
        state.performRecovery(state.pageRecovery!!.navigation) {}
        assertEquals(2, state.webViewGeneration)
    }

    @Test
    fun reloadWhileRendererGone_reopensInsteadOfReloading() {
        state.onRenderProcessGone(1)
        state.reload()
        assertEquals(1, state.webViewGeneration)
        assertTrue(state.pageRecovery!!.isRunning)
    }

    private fun fail(
        navigation: SessionPageNavigation,
        error: SessionLoadError = SessionLoadError.CONNECTION,
        hasVisibleDocument: Boolean = false,
        network: SessionNetworkAvailability = SessionNetworkAvailability.VALIDATED,
    ) {
        state.onPageLoadChanged(
            SessionPageLoad.Failed(
                SessionPageFailure(navigation, error, "GET", hasVisibleDocument, false, false, network),
            ),
        )
    }

    private fun navigation(id: Long) = SessionPageNavigation(1, id, "https://example.com/failed")
}
