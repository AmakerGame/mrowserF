package com.EdS.mrowserF.home

import android.app.AlertDialog
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.EdS.mrowserF.R
import com.EdS.mrowserF.data.AppLanguage
import com.EdS.mrowserF.data.CursorSpeed
import com.EdS.mrowserF.data.SearchEngine
import com.EdS.mrowserF.data.Settings
import com.EdS.mrowserF.data.SettingsRepository
import com.EdS.mrowserF.data.ZoomLevel

/** Settings overlay: playback/browsing toggles, and pickers for cursor speed, search
 *  engine, page zoom, and language. */
class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val autoOpenRow: View
    private val popupRow: View
    private val cursorRow: View
    private val desktopModeRow: View
    private val clearOnExitRow: View
    private val searchEngineRow: View
    private val zoomRow: View
    private val languageRow: View
    private val autoOpenValue: TextView
    private val popupValue: TextView
    private val cursorValue: TextView
    private val desktopModeValue: TextView
    private val clearOnExitValue: TextView
    private val searchEngineValue: TextView
    private val zoomValue: TextView
    private val languageValue: TextView

    private var repository: SettingsRepository? = null
    private var onChanged: (Settings) -> Unit = {}

    init {
        LayoutInflater.from(context).inflate(R.layout.settings_view, this, true)
        autoOpenRow = findViewById(R.id.settingsAutoOpenRow)
        popupRow = findViewById(R.id.settingsPopupRow)
        cursorRow = findViewById(R.id.settingsCursorRow)
        desktopModeRow = findViewById(R.id.settingsDesktopModeRow)
        clearOnExitRow = findViewById(R.id.settingsClearOnExitRow)
        searchEngineRow = findViewById(R.id.settingsSearchEngineRow)
        zoomRow = findViewById(R.id.settingsZoomRow)
        languageRow = findViewById(R.id.settingsLanguageRow)
        autoOpenValue = findViewById(R.id.settingsAutoOpenValue)
        popupValue = findViewById(R.id.settingsPopupValue)
        cursorValue = findViewById(R.id.settingsCursorValue)
        desktopModeValue = findViewById(R.id.settingsDesktopModeValue)
        clearOnExitValue = findViewById(R.id.settingsClearOnExitValue)
        searchEngineValue = findViewById(R.id.settingsSearchEngineValue)
        zoomValue = findViewById(R.id.settingsZoomValue)
        languageValue = findViewById(R.id.settingsLanguageValue)

        autoOpenRow.setOnClickListener { toggleAutoOpen() }
        popupRow.setOnClickListener { toggleBlockPopups() }
        cursorRow.setOnClickListener { pickCursor() }
        desktopModeRow.setOnClickListener { toggleDesktopMode() }
        clearOnExitRow.setOnClickListener { toggleClearOnExit() }
        searchEngineRow.setOnClickListener { pickSearchEngine() }
        zoomRow.setOnClickListener { pickZoom() }
        languageRow.setOnClickListener { pickLanguage() }
    }

    /** [onChanged] fires after every update, with the settings snapshot that was just saved —
     *  used by the host activity to re-apply things that can't just be read lazily
     *  (e.g. the WebView's User-Agent string for desktop mode, or its text zoom). */
    fun bind(repository: SettingsRepository, onChanged: (Settings) -> Unit = {}) {
        this.repository = repository
        this.onChanged = onChanged
    }

    fun show() {
        visibility = View.VISIBLE
        render()
        // Post: a synchronous requestFocus right after VISIBLE can fail before the layout
        // pass, leaving nothing focused (matches HomeView/HistoryView).
        post { restoreFocus() }
    }

    fun hide() {
        visibility = View.GONE
    }

    /** Re-seat D-pad focus on the first row. Returns false if it couldn't take focus. */
    fun restoreFocus(): Boolean = autoOpenRow.requestFocus()

    private fun current(): Settings = repository?.get() ?: Settings()

    private fun render() {
        val s = current()
        autoOpenValue.setText(if (s.autoOpenPlayer) R.string.on else R.string.off)
        popupValue.setText(if (s.blockPopups) R.string.on else R.string.off)
        cursorValue.setText(cursorLabelRes(s.cursorSpeed))
        desktopModeValue.setText(if (s.desktopMode) R.string.on else R.string.off)
        clearOnExitValue.setText(if (s.clearHistoryOnExit) R.string.on else R.string.off)
        searchEngineValue.setText(searchEngineLabelRes(s.searchEngine))
        zoomValue.setText(zoomLabelRes(s.zoomLevel))
        languageValue.setText(languageLabelRes(s.language))
    }

    /** Saves, re-renders, and notifies the host in one place so every row follows the
     *  same path. */
    private fun apply(newSettings: Settings) {
        repository?.update(newSettings)
        render()
        onChanged(newSettings)
    }

    private fun toggleAutoOpen() = apply(current().copy(autoOpenPlayer = !current().autoOpenPlayer))

    private fun toggleBlockPopups() = apply(current().copy(blockPopups = !current().blockPopups))

    private fun toggleDesktopMode() = apply(current().copy(desktopMode = !current().desktopMode))

    private fun toggleClearOnExit() =
        apply(current().copy(clearHistoryOnExit = !current().clearHistoryOnExit))

    private fun pickCursor() {
        val options = listOf(CursorSpeed.SLOW, CursorSpeed.NORMAL, CursorSpeed.FAST)
        val labels = options.map { context.getString(cursorLabelRes(it)) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.cursor_speed_title)
            .setItems(labels) { _, which -> apply(current().copy(cursorSpeed = options[which])) }
            .show()
    }

    private fun pickSearchEngine() {
        val options = listOf(SearchEngine.GOOGLE, SearchEngine.BING, SearchEngine.DUCKDUCKGO)
        val labels = options.map { context.getString(searchEngineLabelRes(it)) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.search_engine_title)
            .setItems(labels) { _, which -> apply(current().copy(searchEngine = options[which])) }
            .show()
    }

    private fun pickZoom() {
        val options = listOf(ZoomLevel.SMALL, ZoomLevel.NORMAL, ZoomLevel.LARGE, ZoomLevel.EXTRA_LARGE)
        val labels = options.map { context.getString(zoomLabelRes(it)) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.zoom_title)
            .setItems(labels) { _, which -> apply(current().copy(zoomLevel = options[which])) }
            .show()
    }

    /** Language takes effect immediately: since resource lookups are resolved once per
     *  Activity via attachBaseContext, the host activity is recreated after saving. */
    private fun pickLanguage() {
        val options = listOf(
            AppLanguage.SYSTEM, AppLanguage.UKRAINIAN, AppLanguage.ENGLISH, AppLanguage.RUSSIAN,
            AppLanguage.POLISH, AppLanguage.GERMAN, AppLanguage.SPANISH, AppLanguage.FRENCH
        )
        val labels = options.map { context.getString(languageLabelRes(it)) }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.language_title)
            .setItems(labels) { _, which ->
                val chosen = options[which]
                if (chosen != current().language) {
                    apply(current().copy(language = chosen))
                    (context as? Activity)?.recreate()
                }
            }
            .show()
    }

    private fun cursorLabelRes(c: CursorSpeed): Int = when (c) {
        CursorSpeed.SLOW -> R.string.cursor_slow
        CursorSpeed.NORMAL -> R.string.cursor_normal
        CursorSpeed.FAST -> R.string.cursor_fast
    }

    private fun searchEngineLabelRes(e: SearchEngine): Int = when (e) {
        SearchEngine.GOOGLE -> R.string.search_google
        SearchEngine.BING -> R.string.search_bing
        SearchEngine.DUCKDUCKGO -> R.string.search_duckduckgo
    }

    private fun zoomLabelRes(z: ZoomLevel): Int = when (z) {
        ZoomLevel.SMALL -> R.string.zoom_small
        ZoomLevel.NORMAL -> R.string.zoom_normal
        ZoomLevel.LARGE -> R.string.zoom_large
        ZoomLevel.EXTRA_LARGE -> R.string.zoom_extra_large
    }

    private fun languageLabelRes(l: AppLanguage): Int = when (l) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.UKRAINIAN -> R.string.language_uk
        AppLanguage.ENGLISH -> R.string.language_en
        AppLanguage.RUSSIAN -> R.string.language_ru
        AppLanguage.POLISH -> R.string.language_pl
        AppLanguage.GERMAN -> R.string.language_de
        AppLanguage.SPANISH -> R.string.language_es
        AppLanguage.FRENCH -> R.string.language_fr
    }
}
