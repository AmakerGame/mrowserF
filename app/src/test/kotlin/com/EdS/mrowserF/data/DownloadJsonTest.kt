package com.EdS.mrowserF.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadJsonTest {

    @Test fun `round trips a list`() {
        val items = listOf(
            DownloadEntry(1L, "a.apk", "https://a.net/a.apk", DownloadStatus.COMPLETE, 1000L),
            DownloadEntry(2L, "b.pdf", "https://b.net/b.pdf", DownloadStatus.DOWNLOADING, 2000L)
        )
        assertEquals(items, DownloadJson.fromJson(DownloadJson.toJson(items)))
    }

    @Test fun `empty list round trips`() {
        assertEquals(emptyList<DownloadEntry>(), DownloadJson.fromJson(DownloadJson.toJson(emptyList())))
    }

    @Test fun `blank input is an empty list`() {
        assertEquals(emptyList<DownloadEntry>(), DownloadJson.fromJson("   "))
    }

    @Test fun `corrupt input is an empty list`() {
        assertEquals(emptyList<DownloadEntry>(), DownloadJson.fromJson("not json"))
    }

    @Test fun `an unrecognized status name falls back to FAILED instead of crashing`() {
        val json = """[{"managerId":1,"title":"x","url":"https://a.net/x","status":"PAUSED","startedAt":1}]"""
        assertEquals(DownloadStatus.FAILED, DownloadJson.fromJson(json).single().status)
    }
}
