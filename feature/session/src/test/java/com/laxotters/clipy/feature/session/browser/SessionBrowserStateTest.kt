package com.laxotters.clipy.feature.session.browser

import com.laxotters.clipy.feature.session.browser.webview.PageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBrowserStateTest {
    @Test
    fun beforeFirstPage_usesInitialUrlLabelWithoutInventingHistory() {
        val state = SessionBrowserState(initialUrl = { "https://www.google.com" })

        assertEquals("", state.currentUrl)
        assertEquals("google.com", state.urlLabel)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
        assertTrue(state.canRefresh)
    }

    @Test
    fun pageInfo_updatesObservedUrlAndHistoryWithoutChangingInput() {
        val initialUrl = "https://google.com"
        val state = SessionBrowserState(initialUrl = { initialUrl })

        state.updatePageInfo(PageInfo("https://www.example.com/product", true, false))

        assertEquals("https://www.example.com/product", state.currentUrl)
        assertEquals("example.com", state.urlLabel)
        assertTrue(state.canGoBack)
        assertFalse(state.canGoForward)
        assertEquals("https://google.com", initialUrl)
    }

    @Test
    fun changedInitialInput_doesNotReplaceObservedPageOrItsHistory() {
        var initialUrl: String? = null
        val state = SessionBrowserState(initialUrl = { initialUrl })
        assertEquals("", state.urlLabel)
        assertFalse(state.canRefresh)

        initialUrl = "https://google.com"
        assertEquals("google.com", state.urlLabel)
        assertTrue(state.canRefresh)
        state.updatePageInfo(PageInfo("https://example.com/product", true, true))
        initialUrl = "https://other.example"

        assertEquals("example.com", state.urlLabel)
        assertTrue(state.canGoBack)
        assertTrue(state.canGoForward)
    }

    @Test
    fun reset_clearsObservedHistoryAndRestoresInputLabel() {
        val state = SessionBrowserState(initialUrl = { "https://google.com" })
        state.updatePageInfo(PageInfo("https://example.com/product", true, true))

        state.reset()

        assertEquals("", state.currentUrl)
        assertEquals("google.com", state.urlLabel)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
    }

    @Test
    fun replacementBrowser_doesNotInheritPreviousWebViewHistory() {
        val first = SessionBrowserState(initialUrl = { "https://google.com" })
        first.updatePageInfo(PageInfo("https://example.com/product", true, true))
        val replacement = SessionBrowserState(initialUrl = { "https://google.com" })

        assertFalse(replacement.canGoBack)
        assertFalse(replacement.canGoForward)
        assertEquals("google.com", replacement.urlLabel)
        assertTrue(first.canGoBack)
    }
}
