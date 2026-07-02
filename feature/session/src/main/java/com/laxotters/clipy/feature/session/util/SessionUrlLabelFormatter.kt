package com.laxotters.clipy.feature.session.util

import java.net.URI

internal fun formatUrlLabel(url: String): String {
    val trimmedUrl = url.trim()
    if (trimmedUrl.isEmpty()) return ""

    val parseTarget = when {
        "://" in trimmedUrl -> trimmedUrl
        trimmedUrl.hasExplicitScheme() -> trimmedUrl
        else -> "https://$trimmedUrl"
    }
    val host = runCatching { URI(parseTarget).host }
        .getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }

    return host ?: trimmedUrl
}

private fun String.hasExplicitScheme(): Boolean =
    ":" in substringBefore('/')
