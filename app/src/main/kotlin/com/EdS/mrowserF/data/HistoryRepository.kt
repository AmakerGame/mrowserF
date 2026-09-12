package com.EdS.mrowserF.data

interface HistoryRepository {
    fun findAll(): List<HistoryEntry>
    fun record(entry: HistoryEntry)
    fun clear()
}
