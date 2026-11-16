package com.nijatva.gallery.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The app's icons, drawn rather than shipped.
 *
 * A gallery needs eight or ten glyphs, and eight vector drawables plus a tint per
 * theme is more files to keep in step than a hundred lines of canvas. Everything is
 * drawn inside a square so a glyph lines up with text of the same height.
 */
class Glyph(
    context: Context,
    private val kind: Kind,
    tint: Int = Palette.onSurface,
    private val sizeDp: Int = 24,
) : View(context) {

    enum class Kind { BACK, MORE, GRID, FOLDERS, GEAR, SHARE, INFO, PLAY, CLOSE, CHECK, THEME, SETTINGS }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = tint
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = tint
    }

    init {
        setWillNotDraw(false)
    }

    fun setTint(color: Int) {
        stroke.color = color
        fill.color = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = dp(sizeDp)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        stroke.strokeWidth = s * 0.09f

        val pad = s * 0.17f
        val left = pad
        val top = pad
        val right = w - pad
        val bottom = h - pad

        when (kind) {
            Kind.BACK -> {
                canvas.drawLine(right, top, left, h / 2f, stroke)
                canvas.drawLine(left, h / 2f, right, bottom, stroke)
            }

            Kind.MORE -> {
                val radius = s * 0.055f
                listOf(0.27f, 0.5f, 0.73f).forEach { fraction ->
                    canvas.drawCircle(w / 2f, h * fraction, radius, fill)
                }
            }

            Kind.PLAY -> {
                val triangle = Path().apply {
                    moveTo(w * 0.36f, h * 0.25f)
                    lineTo(w * 0.76f, h * 0.5f)
                    lineTo(w * 0.36f, h * 0.75f)
                    close()
                }
                canvas.drawPath(triangle, fill)
            }

            Kind.CLOSE -> {
                canvas.drawLine(left, top, right, bottom, stroke)
                canvas.drawLine(right, top, left, bottom, stroke)
            }

            Kind.CHECK -> {
                val tick = Path().apply {
                    moveTo(left, h * 0.53f)
                    lineTo(w * 0.44f, bottom)
                    lineTo(right, top)
                }
                canvas.drawPath(tick, stroke)
            }

            Kind.GRID -> {
                val gap = s * 0.13f
                val cellW = (right - left - gap) / 2f
                val cellH = (bottom - top - gap) / 2f
                for (column in 0..1) {
                    for (row in 0..1) {
                        val l = left + column * (cellW + gap)
                        val t = top + row * (cellH + gap)
                        canvas.drawRoundRect(
                            RectF(l, t, l + cellW, t + cellH),
                            s * 0.05f,
                            s * 0.05f,
                            stroke,
                        )
                    }
                }
            }

            Kind.FOLDERS -> {
                canvas.drawRoundRect(
                    RectF(left, top + s * 0.13f, right, bottom),
                    s * 0.06f,
                    s * 0.06f,
                    stroke,
                )
                canvas.drawRoundRect(
                    RectF(left + s * 0.07f, top, right - s * 0.02f, bottom - s * 0.15f),
                    s * 0.06f,
                    s * 0.06f,
                    stroke,
                )
            }

            Kind.GEAR, Kind.SETTINGS -> {
                val centreX = w / 2f
                val centreY = h / 2f
                val radius = s * 0.15f
                canvas.drawCircle(centreX, centreY, radius, stroke)
                for (step in 0 until 8) {
                    val angle = Math.toRadians(step * 45.0)
                    val c = cos(angle).toFloat()
                    val sn = sin(angle).toFloat()
                    canvas.drawLine(
                        centreX + c * radius * 1.7f,
                        centreY + sn * radius * 1.7f,
                        centreX + c * radius * 2.4f,
                        centreY + sn * radius * 2.4f,
                        stroke,
                    )
                }
            }

            Kind.SHARE -> {
                canvas.drawLine(w / 2f, h * 0.14f, w / 2f, h * 0.46f, stroke)
                val head = Path().apply {
                    moveTo(w * 0.5f - s * 0.15f, h * 0.28f)
                    lineTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.5f + s * 0.15f, h * 0.28f)
                }
                canvas.drawPath(head, stroke)
                val tray = Path().apply {
                    moveTo(left, h * 0.46f)
                    lineTo(left, bottom)
                    lineTo(right, bottom)
                    lineTo(right, h * 0.46f)
                }
                canvas.drawPath(tray, stroke)
            }

            Kind.INFO -> {
                canvas.drawCircle(w / 2f, h / 2f, s * 0.34f, stroke)
                canvas.drawCircle(w / 2f, h * 0.35f, s * 0.035f, fill)
                canvas.drawLine(w / 2f, h * 0.47f, w / 2f, h * 0.66f, stroke)
            }

            Kind.THEME -> {
                canvas.drawCircle(w / 2f, h / 2f, s * 0.19f, stroke)
                for (step in 0 until 8) {
                    val angle = Math.toRadians(step * 45.0)
                    val c = cos(angle).toFloat()
                    val sn = sin(angle).toFloat()
                    canvas.drawLine(
                        w / 2f + c * s * 0.28f,
                        h / 2f + sn * s * 0.28f,
                        w / 2f + c * s * 0.37f,
                        h / 2f + sn * s * 0.37f,
                        stroke,
                    )
                }
            }
        }
    }
}
