package com.nijatva.gallery.library

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.model.MediaKind

/**
 * Turns a uri handed over by another app into the one thing the viewer needs: a
 * [MediaItem].
 *
 * The uri may be a `content://` from a gallery, a download or a chat app, or a
 * `file://` from a file manager. Everything here is best effort - a uri that answers
 * none of the questions still opens, with the fields it could not answer left empty,
 * because refusing to show a photo over a missing display name would be silly.
 */
object ExternalItems {

    fun describe(context: Context, uri: Uri): MediaItem {
        val resolver: ContentResolver = context.contentResolver

        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Opened item"
        var size = 0L
        var mime = ""
        var width = 0
        var height = 0
        var date = System.currentTimeMillis() / 1000L

        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    fun text(column: String): String? {
                        val index = cursor.getColumnIndex(column)
                        return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
                    }

                    text(OpenableColumns.DISPLAY_NAME)?.let { name = it }
                    text(OpenableColumns.SIZE)?.toLongOrNull()?.let { size = it }
                    text(MediaStore.MediaColumns.MIME_TYPE)?.let { mime = it }
                    text(MediaStore.MediaColumns.WIDTH)?.toIntOrNull()?.let { width = it }
                    text(MediaStore.MediaColumns.HEIGHT)?.toIntOrNull()?.let { height = it }
                    text(MediaStore.MediaColumns.DATE_MODIFIED)?.toLongOrNull()?.let { date = it }
                }
            }
        } catch (t: Throwable) {
            // A uri that will not answer a query is still worth trying to draw.
        }

        if (mime.isBlank()) {
            mime = runCatching { resolver.getType(uri) }.getOrNull() ?: ""
        }

        return MediaItem(
            id = -1L,
            uri = uri,
            kind = if (mime.startsWith("video/")) MediaKind.VIDEO else MediaKind.IMAGE,
            name = name,
            bucketId = -1L,
            bucketName = "Opened from another app",
            dateSeconds = date,
            sizeBytes = size,
            width = width,
            height = height,
            durationMs = 0L,
            mimeType = mime,
        )
    }
}
