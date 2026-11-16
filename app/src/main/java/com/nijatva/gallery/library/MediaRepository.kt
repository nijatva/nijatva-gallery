package com.nijatva.gallery.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.nijatva.gallery.model.Album
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.model.MediaKind
import com.nijatva.gallery.settings.SortOrder

/**
 * Reads the phone's media library out of MediaStore.
 *
 * There is no database of our own and no index file: every launch asks MediaStore for
 * the images and the videos and builds the list in memory. That is what makes the app
 * local-first rather than a sync client - a photo added by any other app, or by the
 * camera, is simply there the next time this runs.
 *
 * Read on a background thread. A library of a few thousand items is a few thousand
 * cursor rows and a couple of hundred milliseconds.
 */
class MediaRepository(private val context: Context) {

    fun load(): List<MediaItem> {
        val items = ArrayList<MediaItem>(2048)
        items.addAll(read(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), MediaKind.IMAGE))
        items.addAll(read(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), MediaKind.VIDEO))
        return items
    }

    /**
     * Albums are folders. MediaStore already groups files by the directory they sit in
     * (its "bucket"), so the grouping here is a [groupBy] and nothing more.
     */
    fun albums(items: List<MediaItem>): List<Album> =
        items.groupBy { it.bucketId }
            .map { (id, group) ->
                val newestFirst = group.sortedByDescending { it.dateSeconds }
                Album(id = id, name = newestFirst.first().bucketName, items = newestFirst)
            }
            .sortedByDescending { it.newest }

    fun sorted(items: List<MediaItem>, order: SortOrder): List<MediaItem> = when (order) {
        SortOrder.NEWEST -> items.sortedByDescending { it.dateSeconds }
        SortOrder.OLDEST -> items.sortedBy { it.dateSeconds }
        SortOrder.NAME -> items.sortedBy { it.name.lowercase() }
        SortOrder.LARGEST -> items.sortedByDescending { it.sizeBytes }
    }

    private fun read(collection: Uri, kind: MediaKind): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            DATE_TAKEN,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            DURATION,
        )

        try {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                // Both are optional: a file may carry no capture date and no duration.
                val takenIndex = cursor.getColumnIndex(DATE_TAKEN)
                val durationIndex = cursor.getColumnIndex(DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val added = cursor.getLong(addedIndex)
                    val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                    val bucketName = cursor.getString(bucketNameIndex)

                    out += MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        kind = kind,
                        name = cursor.getString(nameIndex) ?: "Untitled",
                        bucketId = cursor.getLong(bucketIndex),
                        bucketName = if (bucketName.isNullOrBlank()) "Other" else bucketName,
                        dateSeconds = if (taken > 0L) taken / 1000L else added,
                        sizeBytes = cursor.getLong(sizeIndex),
                        width = cursor.getInt(widthIndex),
                        height = cursor.getInt(heightIndex),
                        durationMs = if (durationIndex >= 0) cursor.getLong(durationIndex) else 0L,
                        mimeType = cursor.getString(mimeIndex) ?: "",
                    )
                }
            }
        } catch (t: Throwable) {
            // A permission revoked mid-session, or a volume pulled out. An empty album is
            // a better answer than a crash.
            Log.w(TAG, "Could not read $collection", t)
        }

        return out
    }

    private companion object {
        const val TAG = "NijatvaGallery"

        /**
         * Written out rather than taken from a constant: DATE_TAKEN and DURATION have
         * lived on three different column classes across API levels, and the SQLite
         * column name has not changed once.
         */
        const val DATE_TAKEN = "datetaken"
        const val DURATION = "duration"
    }
}
