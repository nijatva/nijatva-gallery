package com.nijatva.gallery.ui

import android.content.Context
import android.widget.LinearLayout
import com.nijatva.gallery.model.Album
import com.nijatva.gallery.util.Format

/**
 * One folder, opened. The toolbar carries the folder name, and the grid underneath is
 * the same [MediaGridView] the Photos tab uses, scoped to this album id.
 */
class AlbumPage(context: Context, private val host: Host, private val album: Album) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)

        addView(toolbar(context, album.name, host))
        addView(
            label(context, Format.itemsAndSize(album.count, album.totalBytes), 13f, Palette.muted).apply {
                setPadding(context.dp(20), 0, context.dp(20), context.dp(8))
            },
        )
        addView(
            MediaGridView(context, host, album.id),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f),
        )
    }
}
