package com.EdS.mrowserF.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsJsonTest {

    @Test fun `round trips all fields`() {
        val s = Settings(autoOpenPlayer = false, cursorSpeed = CursorSpeed.FAST)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `round trips the pop-up blocker`() {
        val s = Settings(blockPopups = false)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `the pop-up blocker is on when the field is absent`() {
        assertTrue(SettingsJson.fromJson("{\"autoOpenPlayer\":true}").blockPopups)
    }

    @Test fun `missing fields fall back to defaults`() {
        assertEquals(Settings(), SettingsJson.fromJson("{}"))
    }

    @Test fun `blank input is all defaults`() {
        assertEquals(Settings(), SettingsJson.fromJson(""))
    }

    @Test fun `corrupt input is all defaults`() {
        assertEquals(Settings(), SettingsJson.fromJson("not json"))
    }

    @Test fun `unknown enum name falls back to default`() {
        val s = SettingsJson.fromJson("""{"autoOpenPlayer":true,"cursorSpeed":"WARP"}""")
        assertEquals(CursorSpeed.NORMAL, s.cursorSpeed)
    }

    @Test fun `round trips the language setting`() {
        val s = Settings(language = AppLanguage.UKRAINIAN)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `unknown language name falls back to system default`() {
        val s = SettingsJson.fromJson("""{"language":"KLINGON"}""")
        assertEquals(AppLanguage.SYSTEM, s.language)
    }

    @Test fun `round trips desktop mode and clear-on-exit`() {
        val s = Settings(desktopMode = true, clearHistoryOnExit = true)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `new fields default to off for a pre-upgrade file`() {
        val s = SettingsJson.fromJson("""{"autoOpenPlayer":true,"cursorSpeed":"NORMAL"}""")
        assertEquals(AppLanguage.SYSTEM, s.language)
        assertFalse(s.desktopMode)
        assertFalse(s.clearHistoryOnExit)
    }

    @Test fun `round trips the search engine and zoom level`() {
        val s = Settings(searchEngine = SearchEngine.DUCKDUCKGO, zoomLevel = ZoomLevel.LARGE)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `search engine and zoom default for a pre-upgrade file`() {
        val s = SettingsJson.fromJson("""{"autoOpenPlayer":true,"cursorSpeed":"NORMAL"}""")
        assertEquals(SearchEngine.GOOGLE, s.searchEngine)
        assertEquals(ZoomLevel.NORMAL, s.zoomLevel)
    }

    @Test fun `unknown search engine or zoom name falls back to default`() {
        val s = SettingsJson.fromJson("""{"searchEngine":"YAHOO","zoomLevel":"HUGE"}""")
        assertEquals(SearchEngine.GOOGLE, s.searchEngine)
        assertEquals(ZoomLevel.NORMAL, s.zoomLevel)
    }

    @Test fun `round trips the internal flags`() {
        val s = Settings(seeded = true, navHintShown = true)
        assertEquals(s, SettingsJson.fromJson(SettingsJson.toJson(s)))
    }

    @Test fun `internal flags default to false for a pre-upgrade file`() {
        val s = SettingsJson.fromJson("""{"autoOpenPlayer":true,"cursorSpeed":"NORMAL"}""")
        assertFalse(s.seeded)
        assertFalse(s.navHintShown)
    }
}
