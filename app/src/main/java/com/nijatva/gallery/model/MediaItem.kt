package com.nijatva.gallery.model

import android.net.Uri

enum class MediaKind {
    IMAGE,
    VIDEO,
}

/**
 * One row of the phone's media library, flattened out of MediaStore.
 *
 * [bucketId] and [bucketName] are the folder the file sits in: MediaStore calls it a
 * bucket, the app calls it an album. Two files with the same bucket id are in the
 * same album, and that is the only grouping rule there is - no database, no
 * sidecar file, nothing to get out of step with the phone.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val kind: MediaKind,
    val name: String,
    val bucketId: Long,
    val bucketName: String,
    val dateSeconds: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val mimeType: String,
) {
    val isVideo: Boolean get() = kind == MediaKind.VIDEO
}

/**
 * An album is a folder, counted. Nothing is stored: [items] is the album's slice of
 * the library as it was read this launch, so adding a photo to a folder on the phone
 * adds it here on the next read.
 */
data class Album(
    val id: Long,
    val name: String,
    val items: List<MediaItem>,
) {
    val count: Int get() = items.size

    val totalBytes: Long get() = items.sumOf { it.sizeBytes }

    /** Newest first, which is the order the cover collage wants. */
    val covers: List<MediaItem> get() = items.take(4)

    val newest: Long get() = items.firstOrNull()?.dateSeconds ?: 0L
}
