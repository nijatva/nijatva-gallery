package com.nijatva.gallery.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nijatva.gallery.library.Thumbnails
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.util.Format

/**
 * The square grid of photos and videos.
 *
 * It is used twice: for the whole library and for one album. The only difference is
 * the [albumId] handed to the viewer, so the two screens cannot drift apart.
 */
class MediaGridView(
    context: Context,
    private val host: Host,
    private val albumId: Long?,
) : FrameLayout(context) {

    private val recycler = RecyclerView(context).apply {
        layoutManager = GridLayoutManager(context, host.settings.columns)
        setBackgroundColor(Palette.background)
        setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(12))
        clipToPadding = false
        setHasFixedSize(true)
        itemAnimator = null
    }

    private val adapter = MediaGridAdapter(context, host.thumbs()) { index ->
        host.openViewer(albumId, index)
    }

    private val empty = label(context, "", 14f, Palette.muted).apply {
        gravity = Gravity.CENTER
        setPadding(context.dp(32), 0, context.dp(32), 0)
    }

    init {
        addView(recycler, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        addView(empty, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        recycler.adapter = adapter
        refresh()
    }

    fun refresh() {
        val items = host.itemsFor(albumId)
        adapter.submit(items)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        empty.text = when {
            host.isLibraryLoading() -> "Reading your library…"
            albumId == null && !host.settings.includeVideos -> "No photos yet. Videos are hidden in Settings."
            else -> "Nothing here yet."
        }
    }
}

private class MediaGridAdapter(
    private val context: Context,
    private val thumbs: Thumbnails,
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<MediaGridAdapter.Holder>() {

    private var items: List<MediaItem> = emptyList()

    fun submit(next: List<MediaItem>) {
        items = next
        notifyDataSetChanged()
    }

    class Holder(val cell: MediaCell) : RecyclerView.ViewHolder(cell)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(MediaCell(context, thumbs))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.cell.bind(item) { onClick(position) }
    }

    override fun getItemCount(): Int = items.size
}

/**
 * One square cell: the thumbnail, a play glyph on videos, and the length in the
 * corner. The cell measures itself square from the width the grid gives it, so the
 * columns setting needs no other change.
 */
private class MediaCell(context: Context, private val thumbs: Thumbnails) : FrameLayout(context) {

    private val image = AppCompatImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = rounded(context, Palette.raised, 10)
        clipToOutline = true
    }

    private val play = Glyph(context, Glyph.Kind.PLAY, 0xFFFFFFFF.toInt(), 22).apply {
        alpha = 0.9f
    }

    private val length = chip(context, "", 0x99000000.toInt(), 0xFFFFFFFF.toInt())

    init {
        setPadding(context.dp(2), context.dp(2), context.dp(2), context.dp(2))
        addView(image)
        addView(
            play,
            FrameLayout.LayoutParams(context.dp(22), context.dp(22), Gravity.CENTER),
        )
        addView(
            length,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END,
            ).apply { setMargins(0, 0, context.dp(6), context.dp(6)) },
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Square: the height is whatever width the grid column handed over.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    fun bind(item: MediaItem, onClick: () -> Unit) {
        thumbs.load(item, context.dp(260), image)

        if (item.isVideo) {
            play.visibility = View.VISIBLE
            length.visibility = View.VISIBLE
            length.text = Format.duration(item.durationMs)
        } else {
            play.visibility = View.GONE
            length.visibility = View.GONE
        }

        contentDescription = item.name
        setOnClickListener { onClick() }
    }
}
