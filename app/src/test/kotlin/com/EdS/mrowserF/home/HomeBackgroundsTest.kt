package com.EdS.mrowserF.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBackgroundsTest {

    private fun channels(color: Int) = listOf(
        (color ushr 24) and 0xFF,
        (color ushr 16) and 0xFF,
        (color ushr 8) and 0xFF,
        color and 0xFF
    )

    @Test fun `indexFor is deterministic for a given seed`() {
        assertEquals(HomeBackgrounds.indexFor(42L), HomeBackgrounds.indexFor(42L))
    }

    @Test fun `indexFor always lands inside the list`() {
        (0L until 100L).forEach {
            assertTrue(HomeBackgrounds.indexFor(it) in HomeBackgrounds.ALL.indices)
        }
    }

    @Test fun `indexFor survives negative seeds`() {
        assertTrue(HomeBackgrounds.indexFor(-7L) in HomeBackgrounds.ALL.indices)
        assertTrue(HomeBackgrounds.indexFor(Long.MIN_VALUE) in HomeBackgrounds.ALL.indices)
    }

    @Test fun `consecutive seeds walk the whole list`() {
        val seen = (0L until HomeBackgrounds.ALL.size.toLong())
            .map { HomeBackgrounds.indexFor(it) }.toSet()
        assertEquals(HomeBackgrounds.ALL.size, seen.size)
    }

    @Test fun `blend at zero is the start gradient`() {
        val a = HomeBackgrounds.ALL[0]
        val b = HomeBackgrounds.ALL[1]
        assertEquals(a, HomeBackgrounds.blend(a, b, 0f))
    }

    @Test fun `blend at one is the end gradient`() {
        val a = HomeBackgrounds.ALL[0]
        val b = HomeBackgrounds.ALL[1]
        assertEquals(b, HomeBackgrounds.blend(a, b, 1f))
    }

    @Test fun `blend at the midpoint averages every channel`() {
        val a = HomeBackgrounds.Gradient(0xFF000000.toInt(), 0xFF204060.toInt())
        val b = HomeBackgrounds.Gradient(0xFF808080.toInt(), 0xFF608020.toInt())
        val mid = HomeBackgrounds.blend(a, b, 0.5f)
        assertEquals(listOf(255, 64, 64, 64), channels(mid.top))
        assertEquals(listOf(255, 64, 96, 64), channels(mid.bottom))
    }

    @Test fun `blend clamps a fraction outside zero to one`() {
        val a = HomeBackgrounds.ALL[0]
        val b = HomeBackgrounds.ALL[1]
        assertEquals(a, HomeBackgrounds.blend(a, b, -3f))
        assertEquals(b, HomeBackgrounds.blend(a, b, 4f))
    }

    @Test fun `blend keeps the alpha channel opaque`() {
        val mid = HomeBackgrounds.blend(HomeBackgrounds.ALL[0], HomeBackgrounds.ALL[3], 0.5f)
        assertEquals(255, channels(mid.top)[0])
        assertEquals(255, channels(mid.bottom)[0])
    }

    /** The whole point of the cycle: if neighbours are too close, the animation
     *  runs but reads as a static screen. The shipped palettes used to differ by
     *  at most 17/255, which is below the perceptual floor over a 20s fade. */
    @Test fun `every neighbouring palette is visibly apart`() {
        val cycle = HomeBackgrounds.ALL + HomeBackgrounds.ALL.first()
        cycle.zipWithNext { a, b ->
            val step = channels(a.bottom).zip(channels(b.bottom))
                .maxOf { (x, y) -> kotlin.math.abs(x - y) }
            assertTrue(
                "bottom stops ${Integer.toHexString(a.bottom)} -> " +
                    "${Integer.toHexString(b.bottom)} differ by only $step/255",
                step >= 20
            )
        }
    }

    /** Top stops stay near-black so the wordmark and URL pill keep their contrast
     *  no matter where in the cycle the screen is caught. */
    @Test fun `every top stop stays near black`() {
        HomeBackgrounds.ALL.forEach { g ->
            channels(g.top).drop(1).forEach { assertTrue("top stop too bright: $it", it <= 0x20) }
        }
    }
}
