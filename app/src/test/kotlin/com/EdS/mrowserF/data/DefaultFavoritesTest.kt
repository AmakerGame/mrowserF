package com.EdS.mrowserF.data

import com.EdS.mrowserF.web.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFavoritesTest {

    @Test fun `every shipped url is already canonical`() {
        DefaultFavorites.ALL.forEach { assertEquals(it.url, UrlNormalizer.normalize(it.url)) }
    }

    @Test fun `every shipped favorite has a title`() {
        DefaultFavorites.ALL.forEach { assertTrue(it.title.isNotBlank()) }
    }

    @Test fun `shipped urls are unique`() {
        assertEquals(DefaultFavorites.ALL.size, DefaultFavorites.ALL.map { it.url }.toSet().size)
    }
}
