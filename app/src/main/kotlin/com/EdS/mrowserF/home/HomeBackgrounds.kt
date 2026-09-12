package com.EdS.mrowserF.home

/**
 * Pure: the home overlay's background gradient cycle. No Android types.
 *
 * The home screen does not pick one of these and hold it — it starts at
 * [indexFor] and lerps continuously through the whole list via [blend],
 * wrapping forever. The Android side owns only the clock.
 */
object HomeBackgrounds {

    /** A two-stop vertical gradient, top to bottom, as ARGB ints. */
    data class Gradient(val top: Int, val bottom: Int)

    /**
     * Top stops stay near-black so text keeps its contrast wherever the cycle is
     * caught; the bottom stops carry the colour. They are spread far enough apart
     * that the crossfade is actually visible — see the neighbour-distance test.
     */
    val ALL: List<Gradient> = listOf(
        Gradient(0xFF0B0B0F.toInt(), 0xFF2E0D16.toInt()), // ember
        Gradient(0xFF080F14.toInt(), 0xFF0C2438.toInt()), // deep sea
        Gradient(0xFF100A18.toInt(), 0xFF261043.toInt()), // violet
        Gradient(0xFF0A0F0C.toInt(), 0xFF0E2C1F.toInt()), // pine
        Gradient(0xFF120C0A.toInt(), 0xFF38180D.toInt()), // rust
        Gradient(0xFF0C0C0C.toInt(), 0xFF20202C.toInt())  // graphite
    )

    /** Where the cycle starts. Deterministic for a given [seed]; safe for negative seeds. */
    fun indexFor(seed: Long): Int = (((seed % ALL.size) + ALL.size) % ALL.size).toInt()

    /** The gradient [fraction] of the way from [from] to [to]; clamped to 0..1. */
    fun blend(from: Gradient, to: Gradient, fraction: Float): Gradient {
        val t = fraction.coerceIn(0f, 1f)
        return Gradient(lerpColor(from.top, to.top, t), lerpColor(from.bottom, to.bottom, t))
    }

    /** Per-channel ARGB interpolation. Plain sRGB: every colour here is dark and
     *  barely saturated, so the perceptually-correct variants buy nothing. */
    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        var out = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            val v = (a + (b - a) * t).toInt().coerceIn(0, 255)
            out = out or (v shl shift)
        }
        return out
    }
}
