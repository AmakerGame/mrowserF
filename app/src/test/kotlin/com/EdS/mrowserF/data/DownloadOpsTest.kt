package com.EdS.mrowserF.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadOpsTest {

    private fun e(id: Long, status: DownloadStatus = DownloadStatus.DOWNLOADING, t: Long = id) =
        DownloadEntry(id, "file$id.apk", "https://example.com/file$id.apk", status, t)

    @Test fun `record puts a new entry at the front`() {
        val a = e(1)
        val b = e(2)
        assertEquals(listOf(b, a), DownloadOps.record(listOf(a), b))
    }

    @Test fun `record caps at the newest 50, dropping the oldest at the tail`() {
        val seed = (1L..50L).map { e(it) }
        val fresh = e(200)
        val result = DownloadOps.record(seed, fresh, cap = 50)
        assertEquals(50, result.size)
        assertEquals(fresh, result.first())
        assertEquals(49L, result.last().managerId)
    }

    @Test fun `updateStatus updates only the matching entry`() {
        val a = e(1, DownloadStatus.DOWNLOADING)
        val b = e(2, DownloadStatus.DOWNLOADING)
        val result = DownloadOps.updateStatus(listOf(a, b), managerId = 2, status = DownloadStatus.COMPLETE)
        assertEquals(DownloadStatus.DOWNLOADING, result.first { it.managerId == 1L }.status)
        assertEquals(DownloadStatus.COMPLETE, result.first { it.managerId == 2L }.status)
    }

    @Test fun `updateStatus is a no-op when the id is not in the list`() {
        val list = listOf(e(1))
        assertEquals(list, DownloadOps.updateStatus(list, managerId = 99, status = DownloadStatus.FAILED))
    }

    @Test fun `remove drops only the matching entry`() {
        val a = e(1)
        val b = e(2)
        assertEquals(listOf(b), DownloadOps.remove(listOf(a, b), managerId = 1))
    }

    @Test fun `clear empties the list`() {
        assertEquals(emptyList<DownloadEntry>(), DownloadOps.clear())
    }
}
