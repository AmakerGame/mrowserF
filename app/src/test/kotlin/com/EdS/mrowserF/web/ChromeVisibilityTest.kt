package com.EdS.mrowserF.web

import com.EdS.mrowserF.web.ChromeVisibility.Event
import com.EdS.mrowserF.web.ChromeVisibility.State
import org.junit.Assert.assertEquals
import org.junit.Test

class ChromeVisibilityTest {

    @Test fun `reveal at top shows the bar`() {
        assertEquals(State.VISIBLE, ChromeVisibility.reduce(State.HIDDEN, Event.RevealRequested(atTop = true)))
    }

    @Test fun `reveal away from top keeps it hidden`() {
        assertEquals(State.HIDDEN, ChromeVisibility.reduce(State.HIDDEN, Event.RevealRequested(atTop = false)))
    }

    @Test fun `page interaction hides a visible bar`() {
        assertEquals(State.HIDDEN, ChromeVisibility.reduce(State.VISIBLE, Event.PageInteracted))
    }

    @Test fun `interaction keeps a visible bar visible`() {
        assertEquals(State.VISIBLE, ChromeVisibility.reduce(State.VISIBLE, Event.Interacted))
    }

    @Test fun `events other than reveal do nothing while hidden`() {
        assertEquals(State.HIDDEN, ChromeVisibility.reduce(State.HIDDEN, Event.Interacted))
        assertEquals(State.HIDDEN, ChromeVisibility.reduce(State.HIDDEN, Event.PageInteracted))
    }
}
