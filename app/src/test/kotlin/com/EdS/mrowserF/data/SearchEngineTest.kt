package com.EdS.mrowserF.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchEngineTest {

    @Test fun `google builds a search URL`() {
        assertEquals(
            "https://www.google.com/search?q=cats",
            SearchEngine.GOOGLE.searchUrl("cats")
        )
    }

    @Test fun `bing builds a search URL`() {
        assertEquals(
            "https://www.bing.com/search?q=cats",
            SearchEngine.BING.searchUrl("cats")
        )
    }

    @Test fun `duckduckgo builds a search URL`() {
        assertEquals(
            "https://duckduckgo.com/?q=cats",
            SearchEngine.DUCKDUCKGO.searchUrl("cats")
        )
    }

    @Test fun `encodes spaces and special characters`() {
        assertEquals(
            "https://www.google.com/search?q=best+movies+2026%3F",
            SearchEngine.GOOGLE.searchUrl("best movies 2026?")
        )
    }
}
