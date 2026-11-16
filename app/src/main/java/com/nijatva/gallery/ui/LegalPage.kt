package com.nijatva.gallery.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * The three text pages in Settings: licences, privacy and terms.
 *
 * They are in the app rather than on a website because the app has no internet
 * permission. A privacy policy you cannot open without a connection is not much of a
 * promise.
 */
class LegalPage(context: Context, private val host: Host, private val kind: LegalKind) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)

        addView(toolbar(context, kind.title, host))

        val body = label(context, text(), 13.5f, Palette.onSurface).apply {
            setLineSpacing(context.dp(5).toFloat(), 1f)
            setPadding(context.dp(20), context.dp(4), context.dp(20), context.dp(30))
        }

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(body)
        }
        addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun text(): String = when (kind) {
        LegalKind.LICENCES -> LICENCES
        LegalKind.PRIVACY -> PRIVACY
        LegalKind.TERMS -> TERMS
    }

    private companion object {

        val LICENCES = """
            Nijatva Gallery is free software under the Apache License, Version 2.0. The
            full text ships with the source, in the LICENSE file.

            It is built on:

            • AndroidX Core, AppCompat, RecyclerView, ViewPager2 and ExifInterface —
              Apache License 2.0, Copyright The Android Open Source Project.
            • Kotlin standard library and coroutines — Apache License 2.0,
              Copyright JetBrains s.r.o.

            The icons in this app are drawn in Kotlin (ui/Glyph.kt) rather than taken
            from an icon set, so there is nothing else to credit. The launcher icon is a
            flower mark drawn for this app, kept in assets/.

            There is no bundled advertising, analytics or crash-reporting SDK, because
            none of them would have a network to report over: the app has no INTERNET
            permission.
        """.trimIndent()

        val PRIVACY = """
            Nijatva Gallery does not collect anything, because it cannot: the app has no
            INTERNET permission in its manifest. Nothing is uploaded, and there is no
            analytics, no advertising, no account and no crash reporting service.

            What the app reads
            Your photos and videos are read through Android's MediaStore, with read
            access only. The app cannot edit, move or delete anything in your library.
            The library is read into memory each launch; no index or database of it is
            written to disk.

            What the app stores
            Your appearance and library settings, in this app's private preferences
            file. Nothing else.

            What happens when you share
            Sharing never hands out the original file. The app writes a copy into its
            own cache and shares that. With "Strip EXIF on share" on — the default — the
            copy has the camera make and model, lens, serial numbers, software name,
            author, description and every GPS tag removed before it leaves the app, and
            the capture time is dropped as well. The original in your library is never
            modified. The cache copy is replaced the next time you share.

            What leaves the phone
            Only the file you chose, only to the app you chose in the Android share
            sheet, and only while that sheet is open.
        """.trimIndent()

        val TERMS = """
            Nijatva Gallery is provided under the Apache License 2.0, "as is", without
            warranties or conditions of any kind, express or implied. You use it at your
            own risk.

            This is a viewer. It is not a backup, a sync service or a photo organiser.
            It does not copy your library anywhere, so deleting a photo in another app
            deletes it here too — keep your own copies of anything you cannot lose.

            The app reads through MediaStore and depends on what the phone reports.
            Some files will not decode, some videos will not play, and some folders will
            not appear; those are limits of the device's own decoders, not something
            this app can patch around.

            Nothing in this app is legal, medical or financial advice, and nothing in it
            is a promise about a file's contents after it has been shared with another
            app.
        """.trimIndent()
    }
}
