package com.EdS.mrowserF.data

import java.util.Locale

/**
 * In-app UI language override. SYSTEM defers to whatever locale Android is already running in
 * (no override applied); the others force a specific locale regardless of device settings.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    UKRAINIAN("uk"),
    ENGLISH("en"),
    RUSSIAN("ru"),
    POLISH("pl"),
    GERMAN("de"),
    SPANISH("es"),
    FRENCH("fr");

    /** The Locale to apply, or null for SYSTEM (meaning: don't override). */
    fun toLocale(): Locale? = tag?.let { Locale(it) }
}
