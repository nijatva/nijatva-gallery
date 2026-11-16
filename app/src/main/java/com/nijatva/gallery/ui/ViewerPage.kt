package com.nijatva.gallery.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.ScrollView
import android.widget.VideoView
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.util.Format
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * One photo or video at a time, swiped left and right.
 *
 * Photos are decoded at screen size off the main thread rather than handed to
 * `setImageURI`: a 50 MP photo decoded full size is 200 MB of bitmap and a crash on a
 * mid-range phone. Videos get the platform [VideoView] with its own controls, which
 * keeps a media library dependency out of the build.
 */
class ViewerPage(
    context: Context,
    private val host: Host,
    albumId: Long?,
    startIndex: Int,
    explicit: List<MediaItem>? = null,
) : FrameLayout(context) {

    private val items: List<MediaItem> = explicit ?: host.itemsFor(albumId)
    private val pager = ViewPager2(context)
    private val counter = label(context, "", 13f, 0xFFFFFFFF.toInt(), medium = true).apply {
        gravity = Gravity.CENTER
    }
    private val decoder = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val adapter = PagerAdapter()

    init {
        setBackgroundColor(0xFF000000.toInt())

        if (items.isEmpty()) {
            addView(
                label(context, "Nothing to show.", 14f, 0xFFA8A8AE.toInt()).apply { gravity = Gravity.CENTER },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        } else {
            buildPager(startIndex)
        }
    }

    private fun buildPager(startIndex: Int) {
        pager.adapter = adapter
        pager.offscreenPageLimit = 1
        pager.setBackgroundColor(0xFF000000.toInt())
        addView(
            pager,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        addView(
            buildBar(),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP),
        )

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                showCounter(position)
            }
        })

        val start = startIndex.coerceIn(0, max(0, items.size - 1))
        pager.setCurrentItem(start, false)
        showCounter(start)
    }

    private fun showCounter(position: Int) {
        counter.text = (position + 1).toString() + " / " + items.size
    }

    private fun current(): MediaItem? = items.getOrNull(pager.currentItem)

    private fun buildBar(): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xC0000000.toInt(), 0x00000000),
            )
        }
        bar.addView(iconButton(Glyph.Kind.BACK) { host.back() })
        bar.addView(counter, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(iconButton(Glyph.Kind.SHARE) { current()?.let { host.share(it) } })
        bar.addView(iconButton(Glyph.Kind.INFO) { current()?.let { showInfo(it) } })
        return bar
    }

    private fun iconButton(kind: Glyph.Kind, onClick: () -> Unit): View = FrameLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(context.dp(44), context.dp(44))
        isClickable = true
        setOnClickListener { onClick() }
        addView(
            Glyph(context, kind, 0xFFFFFFFF.toInt(), 21),
            FrameLayout.LayoutParams(context.dp(21), context.dp(21), Gravity.CENTER),
        )
    }

    private inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.PageHolder>() {

        inner class PageHolder(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
            PageHolder(
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                },
            )

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val frame = holder.frame
            frame.removeAllViews()
            val item = items[position]
            val page: View = if (item.isVideo) buildVideo(item) else buildPhoto(item)
            frame.addView(
                page,
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
            )
        }

        override fun getItemCount(): Int = items.size
    }

    private fun buildPhoto(item: MediaItem): View {
        val image = ZoomImageView(context)
        val key = item.uri.toString()
        image.tag = key
        image.contentDescription = item.name

        val metrics = resources.displayMetrics
        val target = max(metrics.widthPixels, metrics.heightPixels)

        decoder.execute {
            val bitmap = decode(item, target)
            main.post {
                if (image.tag == key && bitmap != null) image.setImageBitmap(bitmap)
            }
        }
        return image
    }

    private fun buildVideo(item: MediaItem): View {
        val video = VideoView(context)
        video.setBackgroundColor(0xFF000000.toInt())
        val controller = MediaController(context)
        controller.setAnchorView(video)
        video.setMediaController(controller)
        video.setVideoURI(item.uri)
        video.setOnPreparedListener {
            if (host.settings.autoplayVideos) video.start()
        }
        video.setOnErrorListener { _, _, _ ->
            host.toast("This video will not play here.")
            true
        }
        return video
    }

    /** Screen-sized decode: ImageDecoder downsamples while it reads, not after. */
    private fun decode(item: MediaItem, targetPx: Int): Bitmap? = try {
        val source = ImageDecoder.createSource(context.contentResolver, item.uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > targetPx && longest > 0) {
                val ratio = targetPx.toFloat() / longest
                decoder.setTargetSize(
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1),
                )
            }
        }
    } catch (t: Throwable) {
        null
    }

    private fun showInfo(item: MediaItem) {
        val dialog = Dialog(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(22), context.dp(20), context.dp(22), context.dp(16))
            background = rounded(context, Palette.surface, 20)
        }

        content.addView(sectionHeading(context, "Details"))
        content.addView(gap(context, 10))

        row(content, "Name", item.name)
        row(content, "Folder", item.bucketName)
        row(content, "Type", (if (item.isVideo) "Video · " else "Photo · ") + item.mimeType)
        row(content, "Size", Format.bytes(item.sizeBytes))
        row(content, "Dimensions", Format.dimensions(item.width, item.height))
        row(content, "Taken", Format.date(item.dateSeconds))
        if (item.isVideo) row(content, "Length", Format.duration(item.durationMs))
        exifRows(item).forEach { entry -> row(content, entry.first, entry.second) }

        content.addView(
            label(context, "Close", 14f, Palette.accent, medium = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(16), 0, 0)
                isClickable = true
                setOnClickListener { dialog.dismiss() }
            },
        )

        val scroll = ScrollView(context).apply { addView(content) }
        dialog.setContentView(
            scroll,
            ViewGroup.LayoutParams(context.dp(320), ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun row(parent: LinearLayout, key: String, value: String) {
        val line = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(6), 0, context.dp(6))
        }
        line.addView(
            label(context, key, 13f, Palette.muted),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        line.addView(
            label(context, value, 13f, Palette.onSurface, medium = true).apply { gravity = Gravity.END },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.25f),
        )
        parent.addView(line)
    }

    /**
     * The camera settings, read from the original file. This is also the honest answer
     * to "what does stripping remove": everything listed here except the date and the
     * pixel size goes when the photo is shared.
     */
    private fun exifRows(item: MediaItem): List<Pair<String, String>> {
        if (item.isVideo) return emptyList()
        val rows = ArrayList<Pair<String, String>>()
        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                if (make != null || model != null) {
                    rows += "Camera" to listOfNotNull(make, model).joinToString(" ")
                }
                exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let { rows += "Lens" to it }
                exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { rows += "Aperture" to ("f/" + it) }
                exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { rows += "Shutter" to (it + " s") }
                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { rows += "Focal length" to (it + " mm") }
                exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { rows += "ISO" to it }
                exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { rows += "Software" to it }

                val coordinates = FloatArray(2)
                if (exif.getLatLong(coordinates)) rows += "Location" to "Present in EXIF"
            }
        } catch (t: Throwable) {
            rows += "EXIF" to "Not readable"
        }
        return rows
    }
}
