package com.EdS.mrowserF.data

interface SettingsRepository {
    fun get(): Settings
    fun update(settings: Settings)
}
