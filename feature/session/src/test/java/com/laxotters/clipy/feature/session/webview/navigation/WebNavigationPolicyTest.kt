package com.laxotters.clipy.feature.session.webview.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationPolicyTest {
    @Test
    fun httpAndHttpsMainFrame_decide_delegatesToWebView() {
        assertEquals(
            WebNavigationDecision.ContinueInWebView,
            WebNavigationPolicy.decide("http://example.com", isForMainFrame = true),
        )
        assertEquals(
            WebNavigationDecision.ContinueInWebView,
            WebNavigationPolicy.decide("HTTPS://example.com", isForMainFrame = true),
        )
    }

    @Test
    fun localAndExecutableSchemes_decide_excludesExternalHandoff() {
        val excludedUrls = listOf(
            "about:blank",
            "javascript:alert('message')",
            "blob:https://example.com/id",
            "data:text/plain,clipy",
            "file:///tmp/file",
            "content://com.example/item/1",
        )

        excludedUrls.forEach { url ->
            assertEquals(
                WebNavigationDecision.ContinueInWebView,
                WebNavigationPolicy.decide(url, isForMainFrame = true),
            )
        }
    }

    @Test
    fun appSchemeMainFrame_decide_requestsExternalApp() {
        val urls = listOf(
            "mailto:hello@example.com",
            "tel:+821012345678",
            "geo:37.5,127.0",
            "intent://scan/#Intent;scheme=zxing;end",
            "clipy-auth://callback?token=value",
        )

        urls.forEach { url ->
            assertEquals(
                WebNavigationDecision.RequestExternalApp(url),
                WebNavigationPolicy.decide(url, isForMainFrame = true),
            )
        }
    }

    @Test
    fun appSchemeSubframe_decide_delegatesToWebView() {
        assertEquals(
            WebNavigationDecision.ContinueInWebView,
            WebNavigationPolicy.decide("clipy-auth://callback", isForMainFrame = false),
        )
    }

    @Test
    fun missingOrInvalidScheme_decide_delegatesToWebView() {
        assertEquals(
            WebNavigationDecision.ContinueInWebView,
            WebNavigationPolicy.decide("/relative/path", isForMainFrame = true),
        )
        assertEquals(
            WebNavigationDecision.ContinueInWebView,
            WebNavigationPolicy.decide("1invalid:value", isForMainFrame = true),
        )
    }

    @Test
    fun httpUrlWithHost_isHttpUrlWithHost_acceptsNetworkShapeWithoutPublicityGuess() {
        val candidates = listOf(
            "https://example.com/file.zip",
            "http://localhost/file.zip",
            "http://intranet/file.zip",
            "http://127.0.0.1/file.zip",
            "https://user:password@example.com/file.zip",
        )

        candidates.forEach { url ->
            assertTrue(WebNavigationPolicy.isHttpUrlWithHost(url))
        }
    }

    @Test
    fun nonHttpOrMissingHost_isHttpUrlWithHost_rejectsExternalFallback() {
        val rejected = listOf(
            "https:///file.zip",
            "http:file.zip",
            "file:///tmp/file.zip",
            "content://com.example/file/1",
            "clipy://download/file.zip",
            "not a url",
        )

        rejected.forEach { url ->
            assertFalse(WebNavigationPolicy.isHttpUrlWithHost(url))
        }
    }
}
