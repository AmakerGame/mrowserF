package com.EdS.mrowserF.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {

    @Test fun `prepends https when scheme is missing`() {
        assertEquals("https://example.com", UrlNormalizer.normalize("example.com"))
    }

    @Test fun `keeps an existing http scheme`() {
        assertEquals("http://example.com/x", UrlNormalizer.normalize("http://example.com/x"))
    }

    @Test fun `trims surrounding whitespace`() {
        assertEquals("https://example.com", UrlNormalizer.normalize("  example.com  "))
    }

    @Test fun `keeps full path and query`() {
        assertEquals(
            "https://meghan.andrei-tarkovsky.net/stream/1/2/?h=1080",
            UrlNormalizer.normalize("meghan.andrei-tarkovsky.net/stream/1/2/?h=1080")
        )
    }

    @Test fun `allows localhost without a dot`() {
        assertEquals("https://localhost", UrlNormalizer.normalize("localhost"))
    }

    @Test fun `returns null for blank input`() {
        assertNull(UrlNormalizer.normalize("   "))
    }

    @Test fun `returns null for a bare word that is not a host`() {
        assertNull(UrlNormalizer.normalize("notaurl"))
    }

    @Test fun `returns null when input contains spaces`() {
        assertNull(UrlNormalizer.normalize("foo bar baz"))
    }

    @Test fun `resolve returns the normalized URL when input looks like one`() {
        assertEquals(
            "https://example.com",
            UrlNormalizer.resolve("example.com", com.EdS.mrowserF.data.SearchEngine.GOOGLE)
        )
    }

    @Test fun `resolve falls back to a search query for a bare word`() {
        assertEquals(
            "https://www.google.com/search?q=cats",
            UrlNormalizer.resolve("cats", com.EdS.mrowserF.data.SearchEngine.GOOGLE)
        )
    }

    @Test fun `resolve falls back to a search query for text with spaces`() {
        assertEquals(
            "https://duckduckgo.com/?q=best+movies+2026",
            UrlNormalizer.resolve("best movies 2026", com.EdS.mrowserF.data.SearchEngine.DUCKDUCKGO)
        )
    }

    @Test fun `resolve returns null for blank input`() {
        assertNull(UrlNormalizer.resolve("   ", com.EdS.mrowserF.data.SearchEngine.GOOGLE))
    }
}
