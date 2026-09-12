package com.EdS.mrowserF.data

import android.content.Context
import android.content.res.Configuration
import java.io.File
import java.util.Locale

/**
 * Applies the persisted [AppLanguage] override to an Activity's base context. Reads
 * settings.json directly (rather than going through JsonSettingsStore) because this runs in
 * attachBaseContext, before the activity has a chance to construct its own dependencies.
 */
object LocaleHelper {

    fun wrap(context: Context): Context {
        val settingsFile = File(context.filesDir, "settings.json")
        val settings = if (settingsFile.exists()) {
            runCatching { SettingsJson.fromJson(settingsFile.readText()) }.getOrDefault(Settings())
        } else {
            Settings()
        }
        val locale = settings.language.toLocale() ?: return context

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
