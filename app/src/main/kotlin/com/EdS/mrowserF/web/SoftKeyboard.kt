package com.EdS.mrowserF.web

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * TV remotes have no touchscreen, so the framework's touch-driven auto-show of the soft
 * keyboard never fires for a D-pad-focused [android.widget.EditText]: `requestFocus()` alone
 * does not open the IME, and pressing OK on an already-focused field with no click listener
 * is silently swallowed (`View.performClick()` is a no-op when nothing is registered).
 *
 * Attach [showFor] as the field's click listener (OK triggers `performClick()`, which then
 * calls it) so pressing OK behaves like tapping the field on a touchscreen.
 */
object SoftKeyboard {

    fun showFor(view: View) {
        view.requestFocus()
        // Post: showSoftInput can no-op if the view isn't attached/laid out yet
        // (e.g. right after the field's container becomes visible).
        view.post {
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun hide(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
