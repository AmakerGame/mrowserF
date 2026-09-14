package com.EdS.mrowserF

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import java.io.File
import com.EdS.mrowserF.data.DefaultFavorites
import com.EdS.mrowserF.data.DownloadEntry
import com.EdS.mrowserF.data.DownloadStatus
import com.EdS.mrowserF.data.Favorite
import com.EdS.mrowserF.data.HistoryEntry
import com.EdS.mrowserF.data.JsonDownloadsStore
import com.EdS.mrowserF.data.JsonFavoritesStore
import com.EdS.mrowserF.data.JsonHistoryStore
import com.EdS.mrowserF.data.JsonSettingsStore
import com.EdS.mrowserF.data.ZoomLevel
import com.EdS.mrowserF.handoff.HandoffController
import com.EdS.mrowserF.home.DownloadsView
import com.EdS.mrowserF.home.FavoriteDialog
import com.EdS.mrowserF.home.HistoryView
import com.EdS.mrowserF.home.HomeView
import com.EdS.mrowserF.home.SettingsView
import com.EdS.mrowserF.stream.SniffingWebViewClient
import com.EdS.mrowserF.stream.StreamSniffer
import com.EdS.mrowserF.web.BrowserWebChromeClient
import com.EdS.mrowserF.web.ChromeController
import com.EdS.mrowserF.web.CursorController
import com.EdS.mrowserF.web.CursorLayout
import com.EdS.mrowserF.web.ExternalIntentLauncher
import com.EdS.mrowserF.web.IncomingUrl
import com.EdS.mrowserF.web.UrlNormalizer

class MainActivity : Activity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.EdS.mrowserF.data.LocaleHelper.wrap(newBase))
    }

    private lateinit var layout: CursorLayout
    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var chrome: ChromeController
    private lateinit var chromeClient: BrowserWebChromeClient
    private lateinit var sniffer: StreamSniffer
    private lateinit var playChip: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var homeView: HomeView
    private lateinit var favorites: JsonFavoritesStore
    private lateinit var history: JsonHistoryStore
    private lateinit var historyView: HistoryView
    private lateinit var settings: JsonSettingsStore
    private lateinit var settingsView: SettingsView
    private lateinit var externalLinks: ExternalIntentLauncher
    private lateinit var downloads: JsonDownloadsStore
    private lateinit var downloadsView: DownloadsView
    private lateinit var downloadManager: DownloadManager

    /** Registered dynamically (needs the RECEIVER_EXPORTED/NOT_EXPORTED flag on API 33+),
     *  so it's unregistered explicitly too rather than relying on process death. */
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            if (id == -1L) return
            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val successful = statusIdx >= 0 &&
                    cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL
                downloads.updateStatus(id, if (successful) DownloadStatus.COMPLETE else DownloadStatus.FAILED)
                downloadsView.refresh()
            }
        }
    }

    /** True when history was opened from the home overlay (BACK returns to home);
     *  false when opened from the chrome bar mid-browse (BACK returns to the page). */
    private var historyFromHome = false

    /** Set when a page is opened from home; clears the back-stack on its first load. */
    private var clearHistoryOnLoad = false
    private lateinit var handoff: HandoffController
    private val uiHandler = Handler(Looper.getMainLooper())
    private val chipHideRunnable = Runnable { playChip.visibility = View.GONE }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout = findViewById(R.id.cursorLayout)
        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        playChip = findViewById(R.id.playChip)
        homeView = findViewById(R.id.homeView)
        historyView = findViewById(R.id.historyView)
        downloadsView = findViewById(R.id.downloadsView)
        settingsView = findViewById(R.id.settingsView)
        val bar = findViewById<View>(R.id.chromeBar)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val reloadButton = findViewById<ImageButton>(R.id.reloadButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        val historyButton = findViewById<ImageButton>(R.id.historyButton)

        favorites = JsonFavoritesStore(File(filesDir, "favorites.json"))
        history = JsonHistoryStore(File(filesDir, "history.json"))
        downloads = JsonDownloadsStore(File(filesDir, "downloads.json"))
        settings = JsonSettingsStore(File(filesDir, "settings.json"))
        seedDefaultFavorites()
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        registerDownloadReceiver()

        sniffer = StreamSniffer(
            userAgent = { webView.settings.userAgentString },
            onStreamAvailable = {
                runOnUiThread {
                    showChip()
                    if (settings.get().autoOpenPlayer) handoff.play()
                }
            },
            onCleared = { playChip.visibility = View.GONE }
        )
        handoff = HandoffController(this, sniffer)

        externalLinks = ExternalIntentLauncher(
            context = this,
            onFallback = { url -> webView.loadUrl(url) },
            onNoApp = { Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show() }
        )

        webView.webViewClient = SniffingWebViewClient(
            sniffer,
            onNavigate = { url -> updateUrlText(url) },
            onLoaded = { url ->
                recordHistory(url, webView.title)
                // Opening from home starts a fresh tab: drop any prior back-stack so
                // BACK at this page reaches root (close-tab) instead of walking old
                // pages / leftover about:blank entries from a previous tab.
                if (clearHistoryOnLoad) {
                    clearHistoryOnLoad = false
                    webView.clearHistory()
                }
            },
            onExternalScheme = { url -> externalLinks.launch(url) }
        )
        chromeClient = BrowserWebChromeClient(
            activity = this,
            container = layout,
            onEnter = { bar.visibility = View.GONE; playChip.visibility = View.GONE; layout.invalidate() },
            onExit = { layout.invalidate() },
            onTitle = { url, title -> recordHistory(url, title) },
            onPopupBlocked = { Toast.makeText(this, R.string.popup_blocked, Toast.LENGTH_SHORT).show() },
            blockPopups = { settings.get().blockPopups },
            launchExternal = { url -> externalLinks.launch(url) }
        )
        webView.webChromeClient = chromeClient
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            // Route window.open / target="_blank" through onCreateWindow so it can be refused.
            // Left at the default false, the WebView silently loads the pop-up over the current
            // page instead, which is the behaviour being fixed.
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            loadWithOverviewMode = true
            useWideViewPort = true
            // Lock down file:// access (defaults to true on API 23-29): a malicious page
            // must not be able to read the app's private files via a file:// URL.
            allowFileAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            allowContentAccess = false
        }
        applyDesktopMode(settings.get().desktopMode)
        applyZoomLevel(settings.get().zoomLevel)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            startDownload(url, userAgent, contentDisposition, mimeType)
        }

        chrome = ChromeController(bar, urlInput, webView)
        val cursor = CursorController(webView, { settings.get().cursorSpeed.multiplier }) { layout.invalidate() }
        layout.webView = webView
        layout.cursor = cursor
        layout.chrome = chrome
        layout.playChip = playChip
        layout.onChipClick = { handoff.play() }
        layout.onBack = { chromeClient.exitIfFullscreen() }
        layout.onExitPage = { confirmCloseTab() }
        // Refuse the reveal in fullscreen (the bar would sit under the video) and report
        // it, so the hold falls through to the normal BACK chain and exits fullscreen.
        layout.onLongBack = {
            if (chromeClient.isFullscreen) false
            else { chrome.requestReveal(atTop = true); true }
        }

        homeView.bind(
            repository = favorites,
            onOpen = { openUrl(it.url) },
            onSubmitUrl = { text ->
                UrlNormalizer.resolve(text, settings.get().searchEngine)?.let { openUrl(it) }
            },
            onEdit = { fav -> FavoriteDialog.show(this, favorites, fav) { homeView.refresh() } },
            onHistory = { showHistory(fromHome = true) },
            onDownloads = { showDownloads() },
            onSettings = { showSettings() }
        )
        historyView.bind(
            repository = history,
            onOpen = { openUrl(it) },
            onClear = { history.clear(); historyView.refresh() },
            onAddFavorite = { entry ->
                favorites.add(Favorite(entry.title, entry.url))
                Toast.makeText(this, R.string.add_favorite, Toast.LENGTH_SHORT).show()
            }
        )
        downloadsView.bind(
            repository = downloads,
            onOpen = { entry -> openDownload(entry) },
            onRemove = { entry -> downloads.remove(entry.managerId); downloadsView.refresh() },
            onClear = { downloads.clear(); downloadsView.refresh() }
        )
        settingsView.bind(
            repository = settings,
            onOpenLink = { url -> openUrl(url) }
        ) { s ->
            applyDesktopMode(s.desktopMode)
            applyZoomLevel(s.zoomLevel)
        }

        layout.post { cursor.center(webView.width, webView.height) }

        backButton.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
            chrome.onInteracted()
        }
        reloadButton.setOnClickListener {
            webView.reload()
            chrome.onInteracted()
        }
        homeButton.setOnClickListener { showHome() }
        historyButton.setOnClickListener {
            showHistory(fromHome = false)
            chrome.onInteracted()
        }
        favoriteButton.setOnClickListener {
            toggleCurrentFavorite()
            chrome.onInteracted()
        }
        urlInput.setOnEditorActionListener { _, actionId, event ->
            // Same Leanback-keyboard quirk as HomeView's field — see the comment there.
            val pressedGo = actionId == EditorInfo.IME_ACTION_GO
            val pressedEnter = event != null && event.action == KeyEvent.ACTION_DOWN &&
                (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
            if (pressedGo || pressedEnter) {
                UrlNormalizer.resolve(urlInput.text.toString(), settings.get().searchEngine)
                    ?.let { openUrl(it) }
                true
            } else {
                false
            }
        }

        // Launched by another app's link (mrowserF is a registered browser), or from the
        // launcher / TV home row. Only the former has a page to go to.
        val link = IncomingUrl.fromViewIntent(intent?.action, intent?.dataString)
        if (link != null) openUrl(link) else showHome()
    }

    /** A link from another app while mrowserF is already running. `singleTask` sends it here
     *  instead of stacking a second browser, so the running page just navigates. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        IncomingUrl.fromViewIntent(intent?.action, intent?.dataString)?.let { openUrl(it) }
    }

    /** Show exactly one overlay at a time: a second VISIBLE overlay steals window-global
     *  D-pad focus (focus search can't move past it), so every transition hides all three
     *  first, then the caller shows the one it wants (or none, for openUrl). */
    private fun hideAllOverlays() {
        homeView.hide()
        historyView.hide()
        downloadsView.hide()
        settingsView.hide()
    }

    private fun openUrl(url: String) {
        hideAllOverlays()
        layout.requestFocus()
        clearHistoryOnLoad = true
        webView.loadUrl(url)
        chrome.onPageInteracted()
        showNavHintOnce()
    }

    /** One-time nudge: the chrome bar has no MENU key to summon it on most TV remotes. */
    private fun showNavHintOnce() {
        if (settings.get().navHintShown) return
        settings.update(settings.get().copy(navHintShown = true))
        Toast.makeText(this, R.string.nav_hint, Toast.LENGTH_LONG).show()
    }

    private fun showHome() {
        hideAllOverlays()
        homeView.show()
    }

    private fun showSettings() {
        hideAllOverlays()
        settingsView.show()
    }

    private fun showHistory(fromHome: Boolean) {
        historyFromHome = fromHome
        hideAllOverlays()
        historyView.show()
    }

    private fun showDownloads() {
        hideAllOverlays()
        downloadsView.show()
    }

    /** First launch only: write the shipped starter favorites. Guarded by a persisted
     *  flag rather than an is-empty check — a user who deletes them must not get them
     *  back. Existing installs have no flag in settings.json, so they seed once on upgrade.
     *  Added in reverse: FavoritesOps.add prepends, so seeding back-to-front leaves the
     *  grid in DefaultFavorites.ALL order. */
    private fun seedDefaultFavorites() {
        if (settings.get().seeded) return
        DefaultFavorites.ALL.asReversed().forEach { favorites.add(it) }
        settings.update(settings.get().copy(seeded = true))
    }

    private fun recordHistory(url: String, title: String?) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val label = title?.takeIf { it.isNotBlank() } ?: (Uri.parse(url).host ?: url)
        history.record(HistoryEntry(label, url, System.currentTimeMillis()))
    }

    private fun updateUrlText(url: String) {
        urlInput.setText(url)
        updateFavoriteIcon()
    }

    private fun showChip() {
        playChip.visibility = View.VISIBLE
        uiHandler.removeCallbacks(chipHideRunnable)
        uiHandler.postDelayed(chipHideRunnable, CHIP_TIMEOUT_MS)
    }

    /** Swaps the WebView's User-Agent between the TV/mobile default and a desktop one, and
     *  reloads the current page (if any) so the site re-renders under the new layout. */
    private fun applyDesktopMode(enabled: Boolean) {
        webView.settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else null
        if (::webView.isInitialized && webView.url != null) webView.reload()
    }

    /** Text zoom only, not layout zoom — resizes text without breaking page layout. */
    private fun applyZoomLevel(level: ZoomLevel) {
        webView.settings.textZoom = level.percent
    }

    /** The WebView can't save a file itself — DownloadManager does the actual fetch
     *  (survives the app closing) and posts the system "download complete" notification. */
    private fun startDownload(url: String, userAgent: String, contentDisposition: String?, mimeType: String?) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = try {
            DownloadManager.Request(Uri.parse(url))
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
            return
        }
        request.apply {
            setMimeType(mimeType)
            addRequestHeader("User-Agent", userAgent)
            // The download often needs the same session cookie the page used (e.g. a
            // logged-in file host) — DownloadManager makes a fresh request with none of it.
            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            // App-specific storage: no WRITE_EXTERNAL_STORAGE permission needed on any API
            // level. DownloadManager still serves a content:// Uri for it (getUriForDownloadedFile)
            // and still posts the normal completion notification.
            setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(fileName)
        }
        val id = try {
            downloadManager.enqueue(request)
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
            return
        }
        downloads.record(DownloadEntry(id, fileName, url, DownloadStatus.DOWNLOADING, System.currentTimeMillis()))
        downloadsView.refresh()
        Toast.makeText(this, getString(R.string.download_started, fileName), Toast.LENGTH_SHORT).show()
    }

    /** Opens a completed download with whatever app the system offers for its type; a
     *  download still in flight, or one the DownloadManager no longer knows about (cleared
     *  from the system's own downloads list), just gets a toast instead of a crash. */
    private fun openDownload(entry: DownloadEntry) {
        if (entry.status != DownloadStatus.COMPLETE) {
            Toast.makeText(this, R.string.download_in_progress, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = runCatching { downloadManager.getUriForDownloadedFile(entry.managerId) }.getOrNull()
        if (uri == null) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
            return
        }
        val mime = downloadManager.getMimeTypeForDownloadedFile(entry.managerId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
        }
    }

    /** API 33+ requires RECEIVER_EXPORTED/NOT_EXPORTED on a context-registered receiver;
     *  older platforms don't have the flag at all. mrowserF's own downloads are the only
     *  thing this needs to hear about, so NOT_EXPORTED (no other app may send it this). */
    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(downloadCompleteReceiver, filter)
        }
    }

    private fun isFavorite(url: String): Boolean = favorites.findAll().any { it.url == url }

    /** Star button: add the current page if absent, remove it if already saved. */
    private fun toggleCurrentFavorite() {
        val url = webView.url ?: return
        if (isFavorite(url)) {
            favorites.remove(url)
            Toast.makeText(this, R.string.remove_favorite, Toast.LENGTH_SHORT).show()
        } else {
            val title = webView.title?.takeIf { it.isNotBlank() } ?: (Uri.parse(url).host ?: url)
            favorites.add(Favorite(title, url))
            Toast.makeText(this, R.string.add_favorite, Toast.LENGTH_SHORT).show()
        }
        updateFavoriteIcon()
        homeView.refresh()
    }

    /** Tint the star accent (red) when the current page is a favorite, white otherwise. */
    private fun updateFavoriteIcon() {
        val saved = webView.url?.let { isFavorite(it) } ?: false
        val color = getColor(if (saved) R.color.accent else R.color.on_surface)
        favoriteButton.imageTintList = ColorStateList.valueOf(color)
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
        if (::sniffer.isInitialized && sniffer.hasStream() &&
            homeView.visibility != View.VISIBLE && historyView.visibility != View.VISIBLE &&
            downloadsView.visibility != View.VISIBLE && settingsView.visibility != View.VISIBLE) showChip()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(downloadCompleteReceiver) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Recovery: if focus was lost (intermittent on the overlays), D-pad keys have
        // no anchor for focus search and the user is stuck. Re-seat focus into
        // whatever's on screen and swallow this one press.
        if (currentFocus == null && event.action == KeyEvent.ACTION_DOWN) {
            val recovered = when {
                settingsView.visibility == View.VISIBLE -> settingsView.restoreFocus()
                historyView.visibility == View.VISIBLE -> historyView.restoreFocus()
                downloadsView.visibility == View.VISIBLE -> downloadsView.restoreFocus()
                homeView.visibility == View.VISIBLE -> homeView.restoreFocus()
                else -> layout.requestFocus()
            }
            if (recovered) return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            settingsView.visibility == View.VISIBLE -> {
                settingsView.hide()
                showHome()
            }
            historyView.visibility == View.VISIBLE -> {
                historyView.hide()
                // Return to wherever history was opened from.
                if (historyFromHome) showHome() else layout.requestFocus()
            }
            downloadsView.visibility == View.VISIBLE -> {
                downloadsView.hide()
                showHome()
            }
            homeView.visibility == View.VISIBLE -> confirmExit()
            else -> confirmCloseTab()
        }
    }

    /** BACK at the first page in history: confirm before closing the page to home. */
    private fun confirmCloseTab() {
        AlertDialog.Builder(this)
            .setTitle(R.string.close_tab_title)
            .setMessage(R.string.close_tab_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.close_tab) { _, _ ->
                webView.loadUrl("about:blank")
                showHome()
            }
            .show()
    }

    /** BACK on the home overlay is the app's root: confirm before exiting to the launcher. */
    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.exit) { _, _ ->
                if (settings.get().clearHistoryOnExit) history.clear()
                finish()
            }
            .show()
    }

    companion object {
        private const val CHIP_TIMEOUT_MS = 30_000L
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
