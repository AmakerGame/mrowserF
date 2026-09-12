package com.EdS.mrowserF.web

import android.view.View
import android.widget.EditText

/**
 * Drives the address-bar overlay: visibility state, slide/fade animation, and
 * focus handoff. Visibility decisions delegate to ChromeVisibility.
 *
 * The bar is only ever opened for interaction (MENU, or a BACK hold), so it never
 * auto-hides: it stays up while the user types / navigates and closes on BACK,
 * MENU, or submit.
 */
class ChromeController(
    private val bar: View,
    private val urlInput: EditText,
    private val webView: View
) {
    private var state = ChromeVisibility.State.HIDDEN

    val isVisible: Boolean get() = state == ChromeVisibility.State.VISIBLE

    init {
        // Same D-pad-OK-is-a-no-op issue as HomeView's URL field — see SoftKeyboard.
        urlInput.setOnClickListener { SoftKeyboard.showFor(urlInput) }
    }

    fun requestReveal(atTop: Boolean) = dispatch(ChromeVisibility.Event.RevealRequested(atTop))
    fun onInteracted() = dispatch(ChromeVisibility.Event.Interacted)
    fun onPageInteracted() = dispatch(ChromeVisibility.Event.PageInteracted)

    private fun dispatch(event: ChromeVisibility.Event) {
        val next = ChromeVisibility.reduce(state, event)
        if (next == state) return
        state = next
        if (next == ChromeVisibility.State.VISIBLE) animateIn() else animateOut()
    }

    /** The bar is only ever opened to be typed into, so it earns its keep by opening
     *  the keyboard immediately — no separate "click the field" step needed. */
    private fun animateIn() {
        bar.visibility = View.VISIBLE
        bar.translationY = -bar.height.toFloat()
        bar.alpha = 0f
        bar.animate().translationY(0f).alpha(1f).setDuration(180).start()
        SoftKeyboard.showFor(urlInput)
    }

    private fun animateOut() {
        bar.animate().translationY(-bar.height.toFloat()).alpha(0f).setDuration(160)
            .withEndAction { bar.visibility = View.GONE }.start()
        SoftKeyboard.hide(urlInput)
        webView.requestFocus()
    }
}
