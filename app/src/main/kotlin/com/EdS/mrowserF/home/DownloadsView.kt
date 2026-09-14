package com.EdS.mrowserF.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.EdS.mrowserF.R
import com.EdS.mrowserF.data.DownloadEntry
import com.EdS.mrowserF.data.DownloadRepository
import com.EdS.mrowserF.data.DownloadStatus

/** Downloads overlay: heading + clear-all + a list of downloads, newest first. Tap opens a
 *  completed file (or does nothing useful for one still in flight); long-press removes it
 *  from the list without touching the file. */
class DownloadsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val list: LinearLayout
    private val emptyHint: TextView
    private val clearButton: Button

    private var repository: DownloadRepository? = null
    private var onOpen: (DownloadEntry) -> Unit = {}
    private var onRemove: (DownloadEntry) -> Unit = {}
    private var onClear: () -> Unit = {}

    init {
        LayoutInflater.from(context).inflate(R.layout.downloads_view, this, true)
        list = findViewById(R.id.downloadsList)
        emptyHint = findViewById(R.id.downloadsEmptyHint)
        clearButton = findViewById(R.id.clearDownloadsButton)
        clearButton.setOnClickListener { onClear() }
    }

    fun bind(
        repository: DownloadRepository,
        onOpen: (DownloadEntry) -> Unit,
        onRemove: (DownloadEntry) -> Unit,
        onClear: () -> Unit
    ) {
        this.repository = repository
        this.onOpen = onOpen
        this.onRemove = onRemove
        this.onClear = onClear
    }

    fun show() {
        visibility = View.VISIBLE
        refresh()
        // Post: see HomeView.show — a sync requestFocus before layout can no-op.
        post { restoreFocus() }
    }

    /** Re-seat D-pad focus on the first row (or clear-all). Returns false on failure. */
    fun restoreFocus(): Boolean = (list.getChildAt(0) ?: clearButton).requestFocus()

    fun hide() {
        visibility = View.GONE
    }

    fun refresh() {
        val items = repository?.findAll().orEmpty()
        list.removeAllViews()
        emptyHint.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        items.forEach { list.addView(row(it)) }
    }

    private fun row(entry: DownloadEntry): View {
        val v = LayoutInflater.from(context).inflate(R.layout.history_row, list, false)
        val letter = v.findViewById<TextView>(R.id.rowLetter)
        val title = v.findViewById<TextView>(R.id.rowTitle)
        val url = v.findViewById<TextView>(R.id.rowUrl)
        val status = v.findViewById<TextView>(R.id.rowTime)
        letter.text = entry.title.trim().take(1).uppercase().ifEmpty { "•" }
        letter.backgroundTintList = ColorStateList.valueOf(colorFor(entry.url))
        title.text = entry.title
        url.text = entry.url
        status.text = statusLabel(entry.status)
        v.setOnClickListener { onOpen(entry) }
        v.setOnLongClickListener { onRemove(entry); true }
        return v
    }

    private fun statusLabel(status: DownloadStatus): String = when (status) {
        DownloadStatus.DOWNLOADING -> context.getString(R.string.download_status_downloading)
        DownloadStatus.COMPLETE -> context.getString(R.string.download_status_complete)
        DownloadStatus.FAILED -> context.getString(R.string.download_status_failed)
    }

    /** Stable pleasant color derived from the url (matches HomeView/HistoryView). */
    private fun colorFor(url: String): Int {
        val hue = ((url.hashCode() % 360) + 360) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.55f, 0.80f))
    }
}
