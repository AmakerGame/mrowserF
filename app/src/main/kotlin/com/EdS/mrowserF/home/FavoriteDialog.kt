package com.EdS.mrowserF.home

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import com.EdS.mrowserF.R
import com.EdS.mrowserF.data.Favorite
import com.EdS.mrowserF.data.FavoritesRepository
import com.EdS.mrowserF.web.SoftKeyboard

/** Edit (title + url) or delete a favorite, then invoke onChanged. */
object FavoriteDialog {

    fun show(context: Context, repository: FavoritesRepository, fav: Favorite, onChanged: () -> Unit) {
        val titleField = EditText(context).apply {
            setText(fav.title); hint = context.getString(R.string.title_hint)
            setOnClickListener { SoftKeyboard.showFor(this) }
        }
        val urlField = EditText(context).apply {
            setText(fav.url); hint = context.getString(R.string.url_hint)
            setOnClickListener { SoftKeyboard.showFor(this) }
        }
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(titleField)
            addView(urlField)
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.edit_favorite)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                repository.update(fav.url, Favorite(titleField.text.toString(), urlField.text.toString()))
                onChanged()
            }
            .setNegativeButton(R.string.delete) { _, _ ->
                repository.remove(fav.url)
                onChanged()
            }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }
}
