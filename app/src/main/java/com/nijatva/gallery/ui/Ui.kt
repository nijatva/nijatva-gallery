package com.nijatva.gallery.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

/** One line of text with the app's two weights and nothing else to configure. */
fun label(
    context: Context,
    value: CharSequence,
    sizeSp: Float,
    color: Int,
    medium: Boolean = false,
    spacing: Float = 0f,
): TextView = TextView(context).apply {
    text = value
    setTextColor(color)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    typeface = Typeface.create("sans-serif", if (medium) Typeface.BOLD else Typeface.NORMAL)
    letterSpacing = spacing
    includeFontPadding = false
}

/** "APP SETTINGS": the uppercase accent heading the site and the other apps use. */
fun sectionHeading(context: Context, value: CharSequence): TextView =
    label(context, value.toString().uppercase(), 11.5f, Palette.accent, medium = true, spacing = 0.14f)

fun divider(context: Context): View = View(context).apply {
    setBackgroundColor(Palette.hairline)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(1))
}

fun rounded(context: Context, fill: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = context.dp(radiusDp).toFloat()
        if (stroke != null) setStroke(context.dp(1), stroke)
    }

/**
 * A theme or accent pill, filled with the accent when it is the selected one.
 *
 * One line, and the text shrinks to fit rather than wrapping: with four choices -
 * System, Dark, Light, Black - the row is wider than a phone, and a wrapped label
 * turns the last pill into a circle reading "Bl ac k".
 */
fun pill(context: Context, value: CharSequence, selected: Boolean): TextView =
    label(
        context,
        value,
        14f,
        if (selected) 0xFF121214.toInt() else Palette.onSurface,
        medium = true,
    ).apply {
        gravity = android.view.Gravity.CENTER
        maxLines = 1
        isSingleLine = true
        ellipsize = android.text.TextUtils.TruncateAt.END
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            this, 10, 14, 1, TypedValue.COMPLEX_UNIT_SP,
        )
        setPadding(context.dp(12), context.dp(9), context.dp(12), context.dp(9))
        background = if (selected) {
            rounded(context, Palette.accent, 100)
        } else {
            rounded(context, 0x00000000, 100, Palette.hairline)
        }
    }

/** A small filled chip: the video length badge, the "+21" count on the developer page. */
fun chip(context: Context, value: CharSequence, fill: Int, textColor: Int): TextView =
    label(context, value, 11f, textColor, medium = true).apply {
        setPadding(context.dp(7), context.dp(3), context.dp(7), context.dp(3))
        background = rounded(context, fill, 7)
    }
