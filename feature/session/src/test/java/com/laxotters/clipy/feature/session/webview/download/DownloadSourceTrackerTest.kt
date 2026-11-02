package com.laxotters.clipy.feature.session.webview.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSourceTrackerTest {
    @Test
    fun oneMainFrameNavigation_consume_confirmsDownloadOnce() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        tracker.recordNavigation(url, isForMainFrame = true)

        assertTrue(tracker.consumeConfirmedMainFrame(url))
        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun missingNavigation_consume_rejectsDownload() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"

        assertFalse(tracker.consumeConfirmedMainFrame(url))
        tracker.recordNavigation(url, isForMainFrame = true)
        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun subframeNavigation_consume_rejectsDownload() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        tracker.recordNavigation(url, isForMainFrame = false)

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun sameUrlMainFrameAndSubframe_consume_rejectsAmbiguousDownload() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        tracker.recordNavigation(url, isForMainFrame = true)
        tracker.recordNavigation(url, isForMainFrame = false)

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun rejectedSubframeDownload_thenMainFrameSameUrl_remainsAmbiguousUntilPageFinishes() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        tracker.recordNavigation(url, isForMainFrame = false)
        assertFalse(tracker.consumeConfirmedMainFrame(url))

        tracker.recordNavigation(url, isForMainFrame = true)

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun duplicateMainFrameNavigation_consume_rejectsAmbiguousDownload() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        repeat(3) {
            tracker.recordNavigation(url, isForMainFrame = true)
        }

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun completedPage_clear_removesStaleEvidence() {
        val tracker = DownloadSourceTracker()
        val url = "https://example.com/file.zip"
        tracker.recordNavigation(url, isForMainFrame = true)

        tracker.clear()

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun nonHttpNavigation_isNeverRecorded() {
        val tracker = DownloadSourceTracker()
        val url = "file:///tmp/file.zip"
        tracker.recordNavigation(url, isForMainFrame = true)

        assertFalse(tracker.consumeConfirmedMainFrame(url))
    }

    @Test
    fun fullCapacity_recordExistingUrl_keepsSubframeEvidence() {
        val tracker = DownloadSourceTracker()
        repeat(64) { index ->
            tracker.recordNavigation("https://example.com/$index", isForMainFrame = true)
        }
        tracker.recordNavigation("https://example.com/0", isForMainFrame = false)

        assertFalse(tracker.consumeConfirmedMainFrame("https://example.com/0"))
        assertTrue(tracker.consumeConfirmedMainFrame("https://example.com/1"))
    }

    @Test
    fun capacityOverflow_recordMainFrame_rejectsDownloadsUntilReset() {
        val tracker = DownloadSourceTracker()
        val iframeUrl = "https://example.com/iframe"
        tracker.recordNavigation(iframeUrl, isForMainFrame = false)
        repeat(64) { index ->
            tracker.recordNavigation("https://example.com/$index", isForMainFrame = true)
        }

        tracker.recordNavigation(iframeUrl, isForMainFrame = true)
        assertFalse(tracker.consumeConfirmedMainFrame(iframeUrl))
        assertFalse(tracker.consumeConfirmedMainFrame("https://example.com/63"))

        tracker.clear()
        tracker.recordNavigation(iframeUrl, isForMainFrame = true)
        assertTrue(tracker.consumeConfirmedMainFrame(iframeUrl))
    }
}
