package com.EdS.mrowserF.data

interface DownloadRepository {
    fun findAll(): List<DownloadEntry>
    fun record(entry: DownloadEntry)
    fun updateStatus(managerId: Long, status: DownloadStatus)
    fun remove(managerId: Long)
    fun clear()
}
