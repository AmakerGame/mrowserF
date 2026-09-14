package com.EdS.mrowserF.data

/** A file the WebView asked to download, tracked from request through completion. */
enum class DownloadStatus { DOWNLOADING, COMPLETE, FAILED }

/**
 * A download.
 *
 * [managerId] is the id `DownloadManager.enqueue` returned — the only handle we get back to
 * later ask the system how the download went, so it's how completion broadcasts are matched
 * back to this entry (see `MainActivity`'s download-complete receiver).
 */
data class DownloadEntry(
    val managerId: Long,
    val title: String,
    val url: String,
    val status: DownloadStatus,
    val startedAt: Long
)
