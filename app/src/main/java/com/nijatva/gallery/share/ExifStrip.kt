package com.nijatva.gallery.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.nijatva.gallery.model.MediaItem
import java.io.File

/**
 * Builds the file that actually gets shared.
 *
 * Two things happen here, and the order matters. The file is copied into the app's own
 * cache first, because MediaStore will not hand out a writable stream for a photo the
 * app only has read access to — and because editing the original to hide its EXIF
 * would be a destructive thing for a gallery to do. Then, if stripping is on, the
 * identifying tags are removed from the copy and the copy is saved.
 *
 * What goes: everything that names the device or the person or the place. What stays:
 * orientation (without it the photo would display sideways), pixel dimensions, and the
 * exposure settings a photo's own look depends on.
 */
object ExifStrip {

    private val REMOVED = listOf(
        // Where
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        // Which phone and which lens
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        // When, exactly
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        // Who
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_USER_COMMENT,
    )

    /**
     * @return a `content://` uri the share sheet can grant to another app.
     * @throws IllegalStateException when the source cannot be read at all.
     */
    fun prepare(context: Context, item: MediaItem, strip: Boolean): Uri {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        // One shared file at a time: the cache should not grow with every share.
        directory.listFiles()?.forEach { it.delete() }

        val safeName = item.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "shared" }
        val copy = File(directory, safeName)

        val input = context.contentResolver.openInputStream(item.uri)
            ?: throw IllegalStateException("Could not read " + item.name)
        input.use { stream ->
            copy.outputStream().use { output -> stream.copyTo(output) }
        }

        if (strip && !item.isVideo) {
            try {
                val exif = ExifInterface(copy.absolutePath)
                REMOVED.forEach { tag -> exif.setAttribute(tag, null) }
                exif.saveAttributes()
            } catch (t: Throwable) {
                // No EXIF, or a container this library will not rewrite. The copy is
                // still shared; the note in the share text is what the app can promise.
            }
        }

        return FileProvider.getUriForFile(context, context.packageName + ".share", copy)
    }
}
