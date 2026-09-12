package com.EdS.mrowserF.data

/** App-wide settings. Defaults are the shipped values. Immutable — update via copy(). */
data class Settings(
    val autoOpenPlayer: Boolean = true,
    val cursorSpeed: CursorSpeed = CursorSpeed.NORMAL,
    /**
     * Refuse pages that try to open a new window. On a browser with no tabs there is nowhere to
     * put one, so the page would otherwise be replaced — which is how sites turn a click on their
     * video into an unrelated page. Off restores that behaviour for links that rely on it.
     */
    val blockPopups: Boolean = true,
    /** UI language override. SYSTEM leaves the device locale untouched. */
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Requests the desktop layout of a page instead of its mobile/TV layout. */
    val desktopMode: Boolean = false,
    /** Wipes browsing history when the app is closed via the exit confirmation. */
    val clearHistoryOnExit: Boolean = false,
    /** Internal bookkeeping, not a user preference: default favorites written once. */
    val seeded: Boolean = false,
    /** Internal bookkeeping, not a user preference: hold-BACK hint shown once. */
    val navHintShown: Boolean = false
)
