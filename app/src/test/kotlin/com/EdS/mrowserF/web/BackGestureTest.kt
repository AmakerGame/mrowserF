package com.EdS.mrowserF.web

import com.EdS.mrowserF.web.BackGesture.ACTION_DOWN
import com.EdS.mrowserF.web.BackGesture.ACTION_UP
import com.EdS.mrowserF.web.BackGesture.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class BackGestureTest {

    @Test fun `first press with the bar hidden arms the long-press timer`() {
        assertEquals(
            Decision.ArmLongPress,
            BackGesture.decide(ACTION_DOWN, alreadyDown = false, longPressed = false, barVisible = false)
        )
    }

    @Test fun `the timer does not arm while the bar is already visible`() {
        assertEquals(
            Decision.BeginWithoutArming,
            BackGesture.decide(ACTION_DOWN, alreadyDown = false, longPressed = false, barVisible = true)
        )
    }

    @Test fun `a repeated press while already held does not re-arm`() {
        assertEquals(
            Decision.Ignore,
            BackGesture.decide(ACTION_DOWN, alreadyDown = true, longPressed = false, barVisible = false)
        )
        assertEquals(
            Decision.Ignore,
            BackGesture.decide(ACTION_DOWN, alreadyDown = true, longPressed = false, barVisible = true)
        )
    }

    @Test fun `a tap runs the back chain`() {
        assertEquals(
            Decision.RunTapChain,
            BackGesture.decide(ACTION_UP, alreadyDown = true, longPressed = false, barVisible = false)
        )
    }

    @Test fun `a tap runs the back chain with the bar up too`() {
        assertEquals(
            Decision.RunTapChain,
            BackGesture.decide(ACTION_UP, alreadyDown = true, longPressed = false, barVisible = true)
        )
    }

    @Test fun `the trailing release of a fired long-press is swallowed`() {
        assertEquals(
            Decision.Swallow,
            BackGesture.decide(ACTION_UP, alreadyDown = true, longPressed = true, barVisible = true)
        )
    }

    /** In HTML5 fullscreen the reveal is refused, so `longPressed` stays false and the
     *  hold must still fall through to the back chain — which exits fullscreen. */
    @Test fun `a hold whose reveal was refused still runs the back chain`() {
        assertEquals(
            Decision.RunTapChain,
            BackGesture.decide(ACTION_UP, alreadyDown = true, longPressed = false, barVisible = false)
        )
    }

    @Test fun `an unmodelled action is ignored`() {
        assertEquals(
            Decision.Ignore,
            BackGesture.decide(2, alreadyDown = true, longPressed = false, barVisible = false)
        )
    }

    @Test fun `a full tap sequence never swallows the release`() {
        var down = false
        var longPressed = false

        val onDown = BackGesture.decide(ACTION_DOWN, down, longPressed, barVisible = false)
        assertEquals(Decision.ArmLongPress, onDown)
        down = true
        longPressed = false // timer armed but not yet fired

        assertEquals(
            Decision.RunTapChain,
            BackGesture.decide(ACTION_UP, down, longPressed, barVisible = false)
        )
    }

    @Test fun `a full hold sequence opens the bar and swallows the release`() {
        var down = false
        var longPressed = false

        assertEquals(
            Decision.ArmLongPress,
            BackGesture.decide(ACTION_DOWN, down, longPressed, barVisible = false)
        )
        down = true

        // Auto-repeat while the key is held must not re-arm the timer.
        assertEquals(
            Decision.Ignore,
            BackGesture.decide(ACTION_DOWN, down, longPressed, barVisible = false)
        )

        longPressed = true // the timer fired and the reveal was accepted

        assertEquals(
            Decision.Swallow,
            BackGesture.decide(ACTION_UP, down, longPressed, barVisible = true)
        )
    }
}
