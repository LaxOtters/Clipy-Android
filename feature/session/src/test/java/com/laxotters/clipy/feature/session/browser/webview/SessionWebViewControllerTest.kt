package com.laxotters.clipy.feature.session.browser.webview

import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.feature.session.browser.SessionNetworkAvailability
import com.laxotters.clipy.feature.session.browser.SessionPageNavigation
import com.laxotters.clipy.feature.session.browser.webview.client.SessionWebViewClient
import com.laxotters.clipy.feature.session.browser.webview.download.DownloadSourceTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class SessionWebViewControllerTest {
    @Test
    fun replacement_rejectsOldTargetAndOldDetachKeepsCurrentConnection() {
        val controller = SessionWebViewController()
        val old = WebView(ApplicationProvider.getApplicationContext())
        val current = WebView(ApplicationProvider.getApplicationContext())
        val calls = mutableListOf<Long>()
        val oldClient = client(1, calls)
        val currentClient = client(2, calls)
        try {
            controller.attach(old, 1, oldClient)
            controller.attach(current, 2, currentClient)
            controller.detach(old)
            assertFalse(controller.retry(SessionPageNavigation(1, 1, "https://example.com/old")))
            assertTrue(controller.retry(SessionPageNavigation(2, 1, "https://example.com/current")))
            assertEquals(listOf(2L), calls)
            controller.detach(current)
            assertFalse(controller.retry(SessionPageNavigation(2, 1, "https://example.com/current")))
            assertEquals(listOf(2L), calls)
        } finally {
            oldClient.close()
            currentClient.close()
            old.destroy()
            current.destroy()
        }
    }

    @Test
    fun recoveryBack_requiresMatchingWebViewAndActualHistory() {
        val controller = SessionWebViewController()
        val view = object : WebView(ApplicationProvider.getApplicationContext()) {
            var historyAvailable = false
            var backCalls = 0
            override fun canGoBack() = historyAvailable
            override fun goBack() {
                backCalls += 1
            }
        }
        val client = client(1, mutableListOf())
        try {
            controller.attach(view, 1, client)
            assertFalse(controller.goBack(1))
            view.historyAvailable = true
            assertFalse(controller.goBack(2))
            assertTrue(controller.goBack(1))
            assertEquals(1, view.backCalls)
        } finally {
            controller.detach(view)
            client.close()
            view.destroy()
        }
    }

    private fun client(id: Long, calls: MutableList<Long>) = SessionWebViewClient(
        callbacks = SessionWebViewCallbacks({}, {}, onPageLoadChanged = { calls += id }),
        downloadSourceTracker = DownloadSourceTracker(),
        onExternalAppRequested = {},
        documentTracker = SessionDocumentTracker(id),
        networkAvailability = { SessionNetworkAvailability.UNAVAILABLE },
    )
}
