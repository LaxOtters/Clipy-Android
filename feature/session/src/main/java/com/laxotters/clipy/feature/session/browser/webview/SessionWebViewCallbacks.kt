package com.laxotters.clipy.feature.session.browser.webview

import com.laxotters.clipy.feature.session.browser.SessionPageLoad

internal class SessionWebViewCallbacks(
    val onPageFinished: (PageInfo) -> Unit,
    val onRootScrolled: (RootScrollMetrics) -> Unit,
    val onErrorMessage: (ErrorMessage) -> Unit = {},
    val onPageLoadChanged: (SessionPageLoad) -> Unit = {},
    val onHistoryChanged: (PageInfo) -> Unit = {},
)

internal data class ErrorMessage(
    val message: String,
    val key: String,
)

internal data class PageInfo(
    val url: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

internal data class RootScrollMetrics(
    val deltaY: Int,
    val scrollableDistance: Int,
    val viewportHeight: Int,
    val touchSlopPx: Int,
)
