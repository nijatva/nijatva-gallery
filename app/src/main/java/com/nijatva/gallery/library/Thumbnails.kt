package com.nijatva.gallery.library

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.Size
import android.widget.ImageView
import com.nijatva.gallery.model.MediaItem
import java.util.concurrent.Executors

/**
 * Decodes thumbnails off the main thread and keeps the recent ones.
 *
 * Every request is tagged with the key it asked for, and a decoded bitmap is only set
 * if the view still carries that tag. Without that check a fast scroll shows a photo
 * from three rows ago in the cell that has already been reused - the classic gallery
 * bug, and the reason this class exists rather than a plain thread per cell.
 *
 * [android.content.ContentResolver.loadThumbnail] does the decoding: it reads the
 * embedded thumbnail when there is one and downsamples the full image when there is
 * not, and it works for video posters too.
 */
class Thumbnails private constructor(private val context: Context) {

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val workers = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun load(item: MediaItem, sizePx: Int, into: ImageView) {
        val key = item.uri.toString() + "@" + sizePx
        into.tag = key

        val cached = cache.get(key)
        if (cached != null) {
            into.setImageBitmap(cached)
            return
        }

        into.setImageDrawable(null)
        workers.execute {
            val bitmap = try {
                context.contentResolver.loadThumbnail(item.uri, Size(sizePx, sizePx), null)
            } catch (t: Throwable) {
                // A file that vanished, or a format the decoder will not open. The cell
                // stays empty rather than taking the grid down with it.
                null
            }
            if (bitmap != null) cache.put(key, bitmap)
            main.post {
                if (into.tag == key) into.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: Thumbnails? = null

        fun get(context: Context): Thumbnails =
            instance ?: synchronized(this) {
                instance ?: Thumbnails(context.applicationContext).also { instance = it }
            }

        /** An eighth of the heap: enough for a screenful at every zoom level. */
        private fun cacheSizeKb(): Int =
            (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceAtLeast(4096)
    }
}
