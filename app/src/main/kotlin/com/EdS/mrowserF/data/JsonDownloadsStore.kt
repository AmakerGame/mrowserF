package com.EdS.mrowserF.data

import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/** DownloadRepository backed by a JSON file; pure logic delegated to DownloadOps/DownloadJson. */
class JsonDownloadsStore(private val file: File) : DownloadRepository {

    private var items: List<DownloadEntry> =
        if (file.exists()) DownloadJson.fromJson(file.readText()) else emptyList()

    private val io = Executors.newSingleThreadExecutor()

    override fun findAll(): List<DownloadEntry> = items

    override fun record(entry: DownloadEntry) {
        items = DownloadOps.record(items, entry); persist()
    }

    override fun updateStatus(managerId: Long, status: DownloadStatus) {
        items = DownloadOps.updateStatus(items, managerId, status); persist()
    }

    override fun remove(managerId: Long) {
        items = DownloadOps.remove(items, managerId); persist()
    }

    override fun clear() {
        items = DownloadOps.clear(); persist()
    }

    /** Serialize the (immutable) snapshot on the caller, then write off the UI thread.
     *  The single-thread executor preserves write order; failures are logged, not swallowed. */
    private fun persist() {
        val snapshot = DownloadJson.toJson(items)
        io.execute {
            runCatching { file.writeText(snapshot) }
                .onFailure { Log.w(TAG, "persist failed: ${file.name}", it) }
        }
    }

    private companion object {
        private const val TAG = "JsonDownloadsStore"
    }
}
