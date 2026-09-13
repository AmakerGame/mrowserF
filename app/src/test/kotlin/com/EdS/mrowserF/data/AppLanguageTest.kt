package com.EdS.mrowserF.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test fun `system has no locale override`() {
        assertNull(AppLanguage.SYSTEM.toLocale())
    }

    @Test fun `ukrainian maps to the uk locale`() {
        assertEquals("uk", AppLanguage.UKRAINIAN.toLocale()?.language)
    }

    @Test fun `english maps to the en locale`() {
        assertEquals("en", AppLanguage.ENGLISH.toLocale()?.language)
    }

    @Test fun `russian maps to the ru locale`() {
        assertEquals("ru", AppLanguage.RUSSIAN.toLocale()?.language)
    }

    @Test fun `polish maps to the pl locale`() {
        assertEquals("pl", AppLanguage.POLISH.toLocale()?.language)
    }

    @Test fun `german maps to the de locale`() {
        assertEquals("de", AppLanguage.GERMAN.toLocale()?.language)
    }

    @Test fun `spanish maps to the es locale`() {
        assertEquals("es", AppLanguage.SPANISH.toLocale()?.language)
    }

    @Test fun `french maps to the fr locale`() {
        assertEquals("fr", AppLanguage.FRENCH.toLocale()?.language)
    }
}
