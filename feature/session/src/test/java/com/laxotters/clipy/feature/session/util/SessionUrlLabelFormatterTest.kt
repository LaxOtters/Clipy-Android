package com.laxotters.clipy.feature.session.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionUrlLabelFormatterTest {
    @Test
    fun formatUrlLabel_httpUrl_returnsHostWithoutWww() {
        val label = formatUrlLabel("https://www.example.com/products/1?sort=recent")

        assertEquals("example.com", label)
    }

    @Test
    fun formatUrlLabel_urlWithoutScheme_returnsHost() {
        val label = formatUrlLabel("example.com/products/1")

        assertEquals("example.com", label)
    }

    @Test
    fun formatUrlLabel_unparseableUrl_returnsOriginalText() {
        val label = formatUrlLabel("about:blank")

        assertEquals("about:blank", label)
    }
}
