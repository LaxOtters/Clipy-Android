package com.laxotters.clipy.feature.session.browser.webview.client

import android.net.Uri
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.TestWebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.feature.session.browser.SessionLoadError
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageLoad
import com.laxotters.clipy.feature.session.browser.webview.SessionDocumentTracker
import com.laxotters.clipy.feature.session.browser.webview.SessionWebViewCallbacks
import com.laxotters.clipy.feature.session.browser.webview.download.DownloadSourceTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class SessionWebViewClientTest {
    private val view = TestWebView()
    private val loads = mutableListOf<SessionPageLoad>()
    private val failures get() = loads.filterIsInstance<SessionPageLoad.Failed>().map { it.failure }
    private var network = SessionNetworkAvailability.VALIDATED
    private val client = SessionWebViewClient(
        callbacks = SessionWebViewCallbacks(
            onPageFinished = {},
            onRootScrolled = {},
            onPageLoadChanged = loads::add,
        ),
        downloadSourceTracker = DownloadSourceTracker(),
        onExternalAppRequested = {},
        documentTracker = SessionDocumentTracker(1),
        networkAvailability = { network },
    )

    @After
    fun tearDown() {
        client.close()
        view.destroy()
    }

    @Test
    fun subresourceFailure_doesNotReportFailure() {
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(mainFrame = false), error(WebViewClient.ERROR_CONNECT))
        client.onPageCommitVisible(view, URL)
        client.onPageFinished(view, URL)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun mainFramePostFailure_keepsMethodAndIsNeverReady() {
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(method = "POST"), error(WebViewClient.ERROR_TIMEOUT))
        client.onPageFinished(view, URL)
        assertEquals("POST", failures.last().method)
        assertEquals(SessionLoadError.CONNECTION, failures.last().error)
        assertTrue(loads.last() is SessionPageLoad.Failed)
    }

    @Test
    fun errorCodeMapping_securityAndUnavailableRemainDistinct() {
        val codes = mapOf(
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE to SessionLoadError.SECURITY,
            WebViewClient.ERROR_UNSAFE_RESOURCE to SessionLoadError.SECURITY,
            WebViewClient.ERROR_BAD_URL to SessionLoadError.UNAVAILABLE,
            WebViewClient.ERROR_UNSUPPORTED_SCHEME to SessionLoadError.UNAVAILABLE,
            WebViewClient.ERROR_FILE to SessionLoadError.UNAVAILABLE,
            WebViewClient.ERROR_FILE_NOT_FOUND to SessionLoadError.UNAVAILABLE,
            WebViewClient.ERROR_UNKNOWN to SessionLoadError.UNKNOWN,
        )
        codes.forEach { (code, expected) ->
            client.onPageStarted(view, URL, null)
            client.onReceivedError(view, request(), error(code))
            assertEquals(expected, failures.last().error)
        }
    }

    @Test
    fun errorDocumentCommit_replacesRetainedDocumentWithOverlay() {
        showDocument("https://example.com/previous")
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_HOST_LOOKUP))
        assertTrue(failures.single().hasVisibleDocument)
        assertEquals(View.VISIBLE, view.visibility)
        client.onPageCommitVisible(view, URL)
        assertFalse(failures.last().hasVisibleDocument)
        assertEquals(View.INVISIBLE, view.visibility)
    }

    @Test
    fun errorDocument_staysHiddenUntilCurrentDocumentIsReady() {
        client.onPageStarted(view, URL, null)
        client.onPageCommitVisible(view, URL)
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_CONNECT))
        view.completeFrame()
        assertEquals(View.INVISIBLE, view.visibility)
        client.onPageFinished(view, URL)
        client.onPageStarted(view, URL, null)
        client.onPageCommitVisible(view, URL)
        assertEquals(View.INVISIBLE, view.visibility)
        view.completeFrame()
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals(1, loads.count { it is SessionPageLoad.Ready })
    }

    @Test
    fun securityFailureAfterGeneralFailure_keepsWebViewVisible() {
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_CONNECT))
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_FAILED_SSL_HANDSHAKE))
        assertEquals(View.VISIBLE, view.visibility)
        assertEquals(SessionLoadError.SECURITY, failures.last().error)
    }

    @Test
    fun finishBeforeVisualReadiness_doesNotStopNavigation() {
        client.onPageStarted(view, URL, null)
        client.onPageFinished(view, URL)
        assertTrue(loads.last() is SessionPageLoad.Loading)
        client.onPageCommitVisible(view, URL)
        assertTrue(loads.last() is SessionPageLoad.Loading)
        view.completeFrame()
        assertTrue(loads.last() is SessionPageLoad.Ready)
    }

    @Test
    fun retryExternalRedirect_withoutPageStarted_stopsEachAttemptAndIgnoresLateFailure() {
        repeat(2) {
            client.retry(view, URL)
            val started = loads.last().navigation
            assertTrue(client.shouldOverrideUrlLoading(view, request(url = "clipy-auth://callback")))
            assertEquals(SessionPageLoad.Stopped(started), loads.last())
            client.onReceivedError(view, request(), error(WebViewClient.ERROR_CONNECT))
            client.onPageCommitVisible(view, URL)
            client.onPageFinished(view, URL)
            assertEquals(SessionPageLoad.Stopped(started), loads.last())
        }
    }

    @Test
    fun directRetryDownload_stopsWithoutPageStartedButSubframeEvidenceCannotStopIt() {
        client.retry(view, URL)
        val started = loads.last().navigation
        assertTrue(client.onDownloadRequested(URL))
        assertEquals(SessionPageLoad.Stopped(started), loads.last())
        client.retry(view, URL)
        client.shouldOverrideUrlLoading(view, request(mainFrame = false))
        assertFalse(client.onDownloadRequested(URL))
        assertTrue(loads.last() is SessionPageLoad.Loading)
    }

    @Test
    fun retryPageStarted_reusesCommandNavigationAndClosedClientCannotHandoff() {
        client.retry(view, URL)
        val started = loads.single()
        client.onPageStarted(view, URL, null)
        assertEquals(listOf(started), loads)
        client.close()
        assertTrue(client.shouldOverrideUrlLoading(view, request(url = "clipy-auth://callback")))
        assertFalse(client.onDownloadRequested(URL))
        assertEquals(listOf(started), loads)
    }

    @Test
    fun oldNavigationOrReleasedClient_doesNotReportFailure() {
        client.onPageStarted(view, URL, null)
        client.onPageStarted(view, "https://example.com/new", null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_CONNECT))
        assertTrue(failures.isEmpty())
        client.close()
        loads.clear()
        client.onPageStarted(view, URL, null)
        client.onReceivedError(view, request(), error(WebViewClient.ERROR_CONNECT))
        client.onPageFinished(view, URL)
        assertTrue(loads.isEmpty())
    }

    @Test
    fun onlineReload_reportsLoadingOnlyWhenNavigationStarts() {
        view.loadUrl(URL)
        client.reload(view)
        assertTrue(loads.none { it is SessionPageLoad.Loading })
        client.onPageStarted(view, URL, null)
        assertEquals(1, loads.count { it is SessionPageLoad.Loading })
    }

    @Test
    fun reload_preemptsOnlyWithoutNetworkAndKeepsMethod() {
        view.loadUrl(URL)
        network = SessionNetworkAvailability.NOT_VALIDATED
        client.reload(view)
        network = SessionNetworkAvailability.UNKNOWN
        client.reload(view)
        assertEquals(2, view.reloadCount)
        assertTrue(failures.isEmpty())
        network = SessionNetworkAvailability.UNAVAILABLE
        client.reload(view)
        assertEquals("", failures.last().method)
        client.shouldInterceptRequest(view, request(method = "POST"))
        client.reload(view)
        assertEquals("POST", failures.last().method)
        assertEquals(2, view.reloadCount)
        assertEquals(1, view.loadCount)
    }

    @Test
    fun offlineRetryFromRetainedDocument_showsOverlayWithoutNavigation() {
        view.loadUrl(URL)
        showDocument(URL)
        network = SessionNetworkAvailability.UNAVAILABLE
        client.retry(view, "https://example.com/next")
        val failure = failures.single()
        assertFalse(failure.hasVisibleDocument)
        assertEquals("GET", failure.method)
        assertEquals(SessionNetworkAvailability.UNAVAILABLE, failure.network)
        assertEquals(1, view.loadCount)
        assertEquals(View.INVISIBLE, view.visibility)
    }

    private fun showDocument(url: String) {
        client.onPageStarted(view, url, null)
        client.onPageCommitVisible(view, url)
        view.completeFrame()
        client.onPageFinished(view, url)
    }

    private fun request(
        mainFrame: Boolean = true,
        method: String = "GET",
        url: String = URL,
    ) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = method
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    @Config(sdk = [26])
    fun renderProcessGone_isHandledAndReportedOnlyWhileOpen() {
        var gone = 0
        val rendererClient = SessionWebViewClient(
            callbacks = SessionWebViewCallbacks(onPageFinished = {}, onRootScrolled = {}),
            downloadSourceTracker = DownloadSourceTracker(),
            onExternalAppRequested = {},
            documentTracker = SessionDocumentTracker(2),
            onRenderProcessGone = { gone += 1 },
        )

        // 플랫폼만 만드는 객체라 테스트에서 공개 생성자를 직접 사용합니다.
        @Suppress("DEPRECATION")
        val detail = object : RenderProcessGoneDetail() {
            override fun didCrash() = true
            override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
        }
        assertTrue(rendererClient.onRenderProcessGone(view, detail))
        assertEquals(1, gone)
        rendererClient.close()
        assertTrue(rendererClient.onRenderProcessGone(view, detail))
        assertEquals(1, gone)
    }

    private fun error(code: Int) = TestWebResourceError(code)

    private class TestWebView : WebView(ApplicationProvider.getApplicationContext()) {
        private val frames = mutableListOf<Pair<Long, VisualStateCallback>>()
        var reloadCount = 0
        var loadCount = 0

        override fun postVisualStateCallback(requestId: Long, callback: VisualStateCallback) {
            frames += requestId to callback
        }

        override fun reload() {
            reloadCount += 1
        }

        override fun loadUrl(url: String) {
            loadCount += 1
            super.loadUrl(url)
        }

        fun completeFrame() {
            val (id, callback) = frames.removeAt(0)
            callback.onComplete(id)
        }
    }

    private companion object {
        const val URL = "https://example.com/failed"
    }
}
