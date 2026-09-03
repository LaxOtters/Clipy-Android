package com.laxotters.clipy.feature.session.webview

internal class SessionWebViewCallbacks(
    val onPageFinished: (PageInfo) -> Unit,
    val onRootScrolled: (RootScrollMetrics) -> Unit,
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
