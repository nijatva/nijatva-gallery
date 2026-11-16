package com.nijatva.gallery.ui

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nijatva.gallery.library.Thumbnails
import com.nijatva.gallery.model.Album
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.util.Format

/**
 * The Folders tab: one card per folder, with a four-up cover and the count and size
 * under it.
 *
 * Folders are not created here. They are the phone's own directories, and the app has
 * no business inventing a second idea of where a photo lives.
 */
class FoldersGrid(context: Context, private val host: Host) : FrameLayout(context) {

    private val recycler = RecyclerView(context).apply {
        layoutManager = GridLayoutManager(context, 2)
        setBackgroundColor(Palette.background)
        setPadding(context.dp(14), context.dp(6), context.dp(14), context.dp(16))
        clipToPadding = false
        itemAnimator = null
    }

    private val adapter = AlbumAdapter(context, host.thumbs()) { album -> host.openAlbum(album.id) }

    private val empty = label(context, "No folders yet.", 14f, Palette.muted).apply {
        gravity = Gravity.CENTER
    }

    init {
        addView(recycler, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        addView(empty, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        recycler.adapter = adapter

        val albums = host.albums()
        adapter.submit(albums)
        empty.visibility = if (albums.isEmpty()) View.VISIBLE else View.GONE
    }
}

private class AlbumAdapter(
    private val context: Context,
    private val thumbs: Thumbnails,
    private val onClick: (Album) -> Unit,
) : RecyclerView.Adapter<AlbumAdapter.Holder>() {

    private var albums: List<Album> = emptyList()

    fun submit(next: List<Album>) {
        albums = next
        notifyDataSetChanged()
    }

    class Holder(val card: AlbumCard) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(AlbumCard(context, thumbs))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = albums[position]
        holder.card.bind(album) { onClick(album) }
    }

    override fun getItemCount(): Int = albums.size
}

private class AlbumCard(context: Context, private val thumbs: Thumbnails) : LinearLayout(context) {

    private val collage = Collage(context).apply {
        background = rounded(context, Palette.raised, 14)
        clipToOutline = true
    }

    private val title = label(context, "", 15f, Palette.onSurface, medium = true).apply {
        setPadding(context.dp(2), context.dp(9), context.dp(2), context.dp(1))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private val subtitle = label(context, "", 12.5f, Palette.muted).apply {
        setPadding(context.dp(2), 0, context.dp(2), 0)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    init {
        orientation = VERTICAL
        setPadding(context.dp(4), context.dp(4), context.dp(4), context.dp(14))
        addView(
            collage,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        addView(
            title,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        addView(
            subtitle,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
    }

    fun bind(album: Album, onClick: () -> Unit) {
        collage.removeAllViews()
        val covers = album.covers
        if (covers.size == 1) {
            collage.addView(thumb(covers.first()))
        } else {
            covers.forEach { cover -> collage.addView(thumb(cover)) }
        }
        title.text = album.name
        subtitle.text = Format.itemsAndSize(album.count, album.totalBytes)
        contentDescription = album.name + ", " + Format.items(album.count)
        setOnClickListener { onClick() }
    }

    private fun thumb(item: MediaItem): ImageView = AppCompatImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        thumbs.load(item, context.dp(220), this)
    }
}

/**
 * Two-by-two covers, or one cover filling the square when a folder holds a single
 * photo. Measuring happens here rather than with a nested grid so the card is square
 * without any layout weight arithmetic.
 */
private class Collage(context: Context) : ViewGroup(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)

        val full = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        val half = MeasureSpec.makeMeasureSpec(size / 2, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) {
            getChildAt(index).measure(if (childCount == 1) full else half, if (childCount == 1) full else half)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val size = right - left
        val half = size / 2
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (childCount == 1) {
                child.layout(0, 0, size, size)
            } else {
                val x = if (index % 2 == 0) 0 else half
                val y = if (index < 2) 0 else half
                child.layout(x, y, x + half, y + half)
            }
        }
    }
}
