package com.laxotters.clipy.feature.session.browser.webview.navigation

import java.net.URI
import java.net.URISyntaxException

internal object WebNavigationPolicy {
    fun decide(
        url: String,
        isForMainFrame: Boolean,
    ): WebNavigationDecision {
        if (!isForMainFrame) return WebNavigationDecision.ContinueInWebView

        val scheme = extractScheme(url) ?: return WebNavigationDecision.ContinueInWebView
        return if (scheme in EXTERNAL_APP_EXCLUDED_SCHEMES) {
            WebNavigationDecision.ContinueInWebView
        } else {
            WebNavigationDecision.RequestExternalApp(url)
        }
    }

    fun isHttpUrlWithHost(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return false
        }
        return uri.scheme?.lowercase() in HTTP_SCHEMES && !uri.host.isNullOrBlank()
    }

    private fun extractScheme(url: String): String? {
        val separatorIndex = url.indexOf(':')
        if (separatorIndex <= 0) return null

        val scheme = url.substring(0, separatorIndex)
        return scheme
            .takeIf(SCHEME_PATTERN::matches)
            ?.lowercase()
    }

    private val HTTP_SCHEMES = setOf("http", "https")
    private val EXTERNAL_APP_EXCLUDED_SCHEMES = HTTP_SCHEMES + setOf(
        "about",
        "javascript",
        "blob",
        "data",
        "file",
        "content",
    )
    private val SCHEME_PATTERN = Regex("[A-Za-z][A-Za-z0-9+.-]*")
}

internal sealed interface WebNavigationDecision {
    data object ContinueInWebView : WebNavigationDecision

    data class RequestExternalApp(
        val url: String,
    ) : WebNavigationDecision
}
