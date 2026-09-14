package com.EdS.mrowserF.data

/** Pure list operations on downloads, keyed by managerId. */
object DownloadOps {

    const val CAP = 50

    /** Prepend the new download, keep only the newest [cap]. */
    fun record(list: List<DownloadEntry>, entry: DownloadEntry, cap: Int = CAP): List<DownloadEntry> =
        (listOf(entry) + list).take(cap)

    /** Update the status of the entry with this [managerId], if it's still in the list — the
     *  entry may already have been removed or the list cleared by the time a completion
     *  broadcast arrives. */
    fun updateStatus(list: List<DownloadEntry>, managerId: Long, status: DownloadStatus): List<DownloadEntry> =
        list.map { if (it.managerId == managerId) it.copy(status = status) else it }

    fun remove(list: List<DownloadEntry>, managerId: Long): List<DownloadEntry> =
        list.filterNot { it.managerId == managerId }

    fun clear(): List<DownloadEntry> = emptyList()
}
