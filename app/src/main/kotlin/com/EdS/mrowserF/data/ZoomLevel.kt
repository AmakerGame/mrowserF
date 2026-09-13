package com.EdS.mrowserF.data

/**
 * Page zoom setting: TVs are viewed from further away than a phone or monitor, so a fixed
 * 100% page zoom can be hard to read. Maps to [android.webkit.WebSettings.setTextZoom].
 */
enum class ZoomLevel(val percent: Int) {
    SMALL(85),
    NORMAL(100),
    LARGE(115),
    EXTRA_LARGE(130)
}
