package com.EdS.mrowserF.web

import com.EdS.mrowserF.data.SearchEngine

/** Turns raw URL-bar input into a loadable URL, or null if it is not one. */
object UrlNormalizer {

    private val SCHEME = Regex("(?i)^[a-z][a-z0-9+.-]*://")

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(' ')) return null

        val withScheme = if (SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val host = hostOf(withScheme) ?: return null
        val isValidHost = host == "localhost" || host.contains('.')
        return if (isValidHost) withScheme else null
    }

    /**
     * Resolves address-bar text the way a real browser bar does: load it as a URL if it
     * looks like one, otherwise treat it as a search query against [engine]. Returns null
     * only for blank input — every other input resolves to *something* loadable.
     */
    fun resolve(input: String, engine: SearchEngine): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return normalize(trimmed) ?: engine.searchUrl(trimmed)
    }

    private fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", "")
        if (afterScheme.isEmpty()) return null
        val authority = afterScheme.substringBefore('/').substringBefore('?')
        val host = authority.substringAfter('@', authority).substringBefore(':')
        return host.ifEmpty { null }
    }
}
