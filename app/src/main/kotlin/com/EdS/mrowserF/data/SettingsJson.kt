package com.EdS.mrowserF.data

import org.json.JSONException
import org.json.JSONObject

/** Pure JSON (de)serialization of Settings. Missing/unparseable fields → defaults. */
object SettingsJson {

    fun toJson(settings: Settings): String =
        JSONObject()
            .put("autoOpenPlayer", settings.autoOpenPlayer)
            .put("cursorSpeed", settings.cursorSpeed.name)
            .put("blockPopups", settings.blockPopups)
            .put("language", settings.language.name)
            .put("desktopMode", settings.desktopMode)
            .put("clearHistoryOnExit", settings.clearHistoryOnExit)
            .put("searchEngine", settings.searchEngine.name)
            .put("zoomLevel", settings.zoomLevel.name)
            .put("seeded", settings.seeded)
            .put("navHintShown", settings.navHintShown)
            .toString()

    fun fromJson(json: String): Settings {
        if (json.isBlank()) return Settings()
        return try {
            val o = JSONObject(json)
            val defaults = Settings()
            Settings(
                autoOpenPlayer = o.optBoolean("autoOpenPlayer", defaults.autoOpenPlayer),
                cursorSpeed = enumOrDefault(o.optString("cursorSpeed"), defaults.cursorSpeed),
                blockPopups = o.optBoolean("blockPopups", defaults.blockPopups),
                language = enumOrDefault(o.optString("language"), defaults.language),
                desktopMode = o.optBoolean("desktopMode", defaults.desktopMode),
                clearHistoryOnExit = o.optBoolean("clearHistoryOnExit", defaults.clearHistoryOnExit),
                searchEngine = enumOrDefault(o.optString("searchEngine"), defaults.searchEngine),
                zoomLevel = enumOrDefault(o.optString("zoomLevel"), defaults.zoomLevel),
                seeded = o.optBoolean("seeded", defaults.seeded),
                navHintShown = o.optBoolean("navHintShown", defaults.navHintShown)
            )
        } catch (e: JSONException) {
            Settings()
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        T::class.java.enumConstants?.firstOrNull { it.name == name } ?: default
}
