package com.EdS.mrowserF.web

/**
 * Pure decision table for the BACK key's tap-versus-hold gesture.
 *
 * BACK is the most-used key on a TV remote and carries several meanings (exit
 * fullscreen / close the bar / page back / close the tab), so the routing lives
 * here where it can be tested on the JVM. [CursorLayout] owns the Handler, the
 * `backDown` / `backLongPressed` flags and the actual BACK chain; it only applies
 * the [Decision] this returns.
 *
 * The key action arrives as a raw Int (`android.view.KeyEvent.ACTION_DOWN` == 0,
 * `ACTION_UP` == 1) so this module stays free of Android imports.
 */
object BackGesture {

    /** Mirrors `android.view.KeyEvent.ACTION_DOWN`. */
    const val ACTION_DOWN = 0

    /** Mirrors `android.view.KeyEvent.ACTION_UP`. */
    const val ACTION_UP = 1

    enum class Decision {
        /** First ACTION_DOWN with the bar hidden: begin the gesture and arm the long-press timer. */
        ArmLongPress,

        /** First ACTION_DOWN with the bar already up: begin the gesture, but leave the timer
         *  unarmed — BACK keeps its "close the bar" meaning for as long as it's showing. */
        BeginWithoutArming,

        /** Nothing to do: an auto-repeat ACTION_DOWN while the key is still held, or an
         *  action we don't model. Consume the event and leave the state alone. */
        Ignore,

        /** The gesture ended without a long-press having fired (a tap, or a hold whose
         *  reveal was refused): end it and run the normal BACK chain. */
        RunTapChain,

        /** The gesture ended after a long-press already fired: end it and swallow the key,
         *  so the hold doesn't also navigate back. */
        Swallow
    }

    /**
     * @param action the raw key action ([ACTION_DOWN] / [ACTION_UP]).
     * @param alreadyDown true if a BACK gesture is already in progress.
     * @param longPressed true if this gesture's long-press already fired *and was accepted*.
     * @param barVisible true if the chrome bar is currently showing.
     */
    fun decide(action: Int, alreadyDown: Boolean, longPressed: Boolean, barVisible: Boolean): Decision =
        when (action) {
            ACTION_DOWN -> when {
                alreadyDown -> Decision.Ignore
                barVisible -> Decision.BeginWithoutArming
                else -> Decision.ArmLongPress
            }
            ACTION_UP -> if (longPressed) Decision.Swallow else Decision.RunTapChain
            else -> Decision.Ignore
        }
}
