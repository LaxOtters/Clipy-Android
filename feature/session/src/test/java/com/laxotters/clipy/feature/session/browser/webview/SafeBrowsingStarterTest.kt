package com.laxotters.clipy.feature.session.browser.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SafeBrowsingStarterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun supported_waitsForInitializationOnceAndRunsQueuedLoadsInOrder() {
        var starts = 0
        var finish: () -> Unit = {}
        val starter = SafeBrowsingStarter(
            isSupported = { true },
            start = { _, onFinished ->
                starts += 1
                finish = onFinished
            },
        )
        val loads = mutableListOf<String>()
        starter.runWhenReady(context) { loads += "first" }
        starter.runWhenReady(context) { loads += "second" }
        assertEquals(emptyList<String>(), loads)

        finish()
        starter.runWhenReady(context) { loads += "after" }

        assertEquals(1, starts)
        assertEquals(listOf("first", "second", "after"), loads)
    }

    @Test
    fun unsupported_loadsImmediatelyWithoutStarting() {
        val starter = SafeBrowsingStarter(
            isSupported = { false },
            start = { _, _ -> error("Unsupported WebView must not start Safe Browsing") },
        )
        val loads = mutableListOf<String>()
        starter.runWhenReady(context) { loads += "first" }
        assertEquals(listOf("first"), loads)
    }
}
