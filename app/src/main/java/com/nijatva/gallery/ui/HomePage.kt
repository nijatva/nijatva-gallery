package com.nijatva.gallery.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.nijatva.gallery.settings.SortOrder
import com.nijatva.gallery.util.Format

/**
 * The first screen: a title, the count, a sort chip, and the two tabs the reference
 * gallery has - Photos and Folders.
 *
 * Home owns no data. It asks the host for the library, so the same list feeds the
 * grid, the folder cards and the viewer's paging, and there is one answer to "how
 * many photos are there".
 */
class HomePage(context: Context, private val host: Host) : LinearLayout(context) {

    private val content = FrameLayout(context)
    private val subtitle = label(context, "", 13f, Palette.muted)
    private val sortChip = label(context, "", 12f, Palette.onSurface, medium = true)
    private val photosTab = Tab(Glyph.Kind.GRID, "Photos")
    private val foldersTab = Tab(Glyph.Kind.FOLDERS, "Folders")

    private var showingFolders = false

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)

        val header = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(context.dp(20), context.dp(18), context.dp(20), context.dp(10))
        }

        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(
            label(context, "Gallery", 27f, Palette.onSurface, medium = true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        titleRow.addView(moreButton())
        header.addView(titleRow)

        val subRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        subRow.addView(
            subtitle,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        sortChip.apply {
            setPadding(context.dp(10), context.dp(6), context.dp(10), context.dp(6))
            background = rounded(context, 0x00000000, 100, Palette.hairline)
            isClickable = true
            setOnClickListener { cycleSort() }
        }
        subRow.addView(sortChip)
        header.addView(subRow)

        addView(header)
        addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(divider(context))

        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(6))
            setBackgroundColor(Palette.surface)
        }
        bar.addView(photosTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(foldersTab, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(bar)

        photosTab.setOnClickListener { show(folders = false) }
        foldersTab.setOnClickListener { show(folders = true) }

        show(folders = false)
    }

    private fun show(folders: Boolean) {
        showingFolders = folders
        photosTab.select(!folders)
        foldersTab.select(folders)

        content.removeAllViews()
        val grid: View = if (folders) FoldersGrid(context, host) else MediaGridView(context, host, null)
        content.addView(
            grid,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        updateSubtitle()
    }

    private fun updateSubtitle() {
        val items = host.items()
        subtitle.text = if (showingFolders) {
            val albums = host.albums()
            val total = albums.sumOf { it.totalBytes }
            albums.size.toString() + " folders · " + Format.bytes(total)
        } else {
            Format.itemsAndSize(items.size, items.sumOf { it.sizeBytes })
        }
        sortChip.text = shortSort(host.settings.sort)
    }

    private fun cycleSort() {
        val values = SortOrder.values()
        val next = values[(host.settings.sort.ordinal + 1) % values.size]
        host.updateSettings(host.settings.copy(sort = next))
    }

    private fun shortSort(order: SortOrder): String = when (order) {
        SortOrder.NEWEST -> "Recent"
        SortOrder.OLDEST -> "Oldest"
        SortOrder.NAME -> "Name"
        SortOrder.LARGEST -> "Largest"
    }

    private fun moreButton(): View = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
        isClickable = true
        setOnClickListener { host.openSettings() }
        addView(
            Glyph(context, Glyph.Kind.MORE, Palette.onSurface, 20),
            FrameLayout.LayoutParams(context.dp(20), context.dp(20), Gravity.CENTER),
        )
    }

    /** One bottom-bar tab: glyph over caption, accent when it is the open one. */
    private inner class Tab(kind: Glyph.Kind, caption: String) : LinearLayout(context) {

        private val glyph = Glyph(context, kind, Palette.muted, 21)
        private val text = label(context, caption, 11.5f, Palette.muted, medium = true)

        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            setPadding(0, context.dp(8), 0, context.dp(6))
            addView(glyph, LinearLayout.LayoutParams(context.dp(21), context.dp(21)))
            addView(
                text,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = context.dp(5) },
            )
        }

        fun select(selected: Boolean) {
            val color = if (selected) Palette.accent else Palette.muted
            glyph.setTint(color)
            text.setTextColor(color)
        }
    }
}
