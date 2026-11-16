package com.nijatva.gallery.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

/**
 * A photo you can pinch, double-tap and drag, with the paging left to the pager.
 *
 * The fit-to-screen matrix is kept separately from the pinch scale and the drag
 * offset. That way a rotation or a re-layout recomputes the fit and leaves the zoom
 * where the finger left it, which is what a viewer should do.
 */
class ZoomImageView(context: Context) : AppCompatImageView(context) {

    private val fit = Matrix()
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                if (scale <= MIN_SCALE + 0.02f) {
                    scale = MIN_SCALE
                    offsetX = 0f
                    offsetY = 0f
                }
                applyTransform()
                return true
            }
        },
    )

    private val tapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(event: MotionEvent): Boolean {
                if (scale > 1.05f) {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                } else {
                    scale = 2.5f
                    offsetX = (width / 2f - event.x) * (scale - 1f)
                    offsetY = (height / 2f - event.y) * (scale - 1f)
                }
                applyTransform()
                return true
            }

            override fun onScroll(
                first: MotionEvent?,
                current: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                if (scale <= 1.05f) return false
                offsetX -= distanceX
                offsetY -= distanceY
                applyTransform()
                return true
            }
        },
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rebuildFit()
        applyTransform()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scale > 1.05f) parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun rebuildFit() {
        fit.reset()
        val drawable: Drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f || viewWidth <= 0f || viewHeight <= 0f) return

        val ratio = min(viewWidth / drawableWidth, viewHeight / drawableHeight)
        fit.postScale(ratio, ratio)
        fit.postTranslate(
            (viewWidth - drawableWidth * ratio) / 2f,
            (viewHeight - drawableHeight * ratio) / 2f,
        )
    }

    private fun applyTransform() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val maxX = (viewWidth * (scale - 1f)) / 2f
        val maxY = (viewHeight * (scale - 1f)) / 2f
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)

        val matrix = Matrix(fit)
        matrix.postScale(scale, scale, viewWidth / 2f, viewHeight / 2f)
        matrix.postTranslate(offsetX, offsetY)
        imageMatrix = matrix
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 6f
    }
}
