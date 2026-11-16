package com.nijatva.gallery.ui

import android.view.View
import com.nijatva.gallery.model.Album
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.settings.Settings

/**
 * One screen in the app. The stack lives in MainActivity; a screen only has to say
 * where a tap should go.
 *
 * Screens are rebuilt from the route rather than kept alive, which is what makes a
 * theme change take effect everywhere at once instead of leaving the previous screen
 * in the old colours.
 */
sealed interface Route {

    data object Home : Route

    data class Album(val id: Long) : Route

    /** @param albumId null means the viewer was opened from the whole library */
    data class Viewer(val albumId: Long?, val index: Int) : Route

    /** A photo or video handed over by another app through ACTION_VIEW. */
    data class External(val uri: String) : Route

    data object Settings : Route

    data object Developer : Route

    data class Legal(val kind: LegalKind) : Route
}

enum class LegalKind(val title: String) {
    LICENCES("Open-source licences"),
    PRIVACY("Privacy policy"),
    TERMS("Terms of Service"),
}

/**
 * What a screen is allowed to ask of the activity: the settings, the library, the
 * thumbnail cache, and the three things only an activity can do - open a link, share
 * a file and go back.
 */
interface Host {

    val settings: Settings

    fun updateSettings(next: Settings)

    fun open(route: Route)

    fun back()

    fun thumbs(): com.nijatva.gallery.library.Thumbnails

    /** The whole library, newest first, already filtered by the settings. */
    fun items(): List<MediaItem>

    fun albums(): List<Album>

    /** The list a viewer opened from [albumId] should page through. */
    fun itemsFor(albumId: Long?): List<MediaItem>

    fun reloadLibrary()

    fun openLink(url: String)

    /** Copies the file out, strips it if the setting is on, then opens the share sheet. */
    fun share(item: MediaItem)

    fun toast(message: String)

    fun openViewer(albumId: Long?, index: Int) = open(Route.Viewer(albumId, index))

    fun openSettings() = open(Route.Settings)

    fun openDeveloper() = open(Route.Developer)

    fun openLegal(kind: LegalKind) = open(Route.Legal(kind))

    fun openAlbum(id: Long) = open(Route.Album(id))

    fun isLibraryLoading(): Boolean = false
}

/** Shared chrome: the back arrow plus a title, used by every screen but Home. */
fun toolbar(context: android.content.Context, title: String, host: Host): android.widget.LinearLayout {
    val bar = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setPadding(context.dp(12), context.dp(10), context.dp(16), context.dp(10))
    }
    val backButton = android.widget.FrameLayout(context).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(context.dp(44), context.dp(44))
        isClickable = true
        setOnClickListener { host.back() }
        addView(
            Glyph(context, Glyph.Kind.BACK, Palette.onSurface, 22),
            android.widget.FrameLayout.LayoutParams(context.dp(22), context.dp(22), android.view.Gravity.CENTER),
        )
    }
    bar.addView(backButton)
    bar.addView(
        label(context, title, 21f, Palette.onSurface, medium = true).apply {
            setPadding(context.dp(6), 0, 0, 0)
        },
        android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
    )
    return bar
}

/** A view that does nothing but take up room, for the gaps a card layout needs. */
fun gap(context: android.content.Context, heightDp: Int): View =
    View(context).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            context.dp(heightDp),
        )
    }
