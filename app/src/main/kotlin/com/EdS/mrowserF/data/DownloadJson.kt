package com.EdS.mrowserF.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Pure JSON (de)serialization of the downloads list. */
object DownloadJson {

    fun toJson(items: List<DownloadEntry>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("managerId", it.managerId)
                    .put("title", it.title)
                    .put("url", it.url)
                    .put("status", it.status.name)
                    .put("startedAt", it.startedAt)
            )
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<DownloadEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                DownloadEntry(
                    managerId = o.getLong("managerId"),
                    title = o.getString("title"),
                    url = o.getString("url"),
                    // A status name from a future version this build doesn't know about
                    // shouldn't crash the list — treat it as failed rather than throw.
                    status = runCatching { DownloadStatus.valueOf(o.getString("status")) }
                        .getOrDefault(DownloadStatus.FAILED),
                    startedAt = o.getLong("startedAt")
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
