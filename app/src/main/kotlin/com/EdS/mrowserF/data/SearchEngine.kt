package com.EdS.mrowserF.data

import java.net.URLEncoder

/**
 * Search engine used when address-bar text isn't a loadable URL (see
 * [com.EdS.mrowserF.web.UrlNormalizer.resolve]). Pure: just builds a query URL, no Android
 * dependency, so it's covered by JVM tests.
 */
enum class SearchEngine(private val queryUrlPrefix: String) {
    GOOGLE("https://www.google.com/search?q="),
    BING("https://www.bing.com/search?q="),
    DUCKDUCKGO("https://duckduckgo.com/?q=");

    fun searchUrl(query: String): String =
        queryUrlPrefix + URLEncoder.encode(query, "UTF-8")
}
