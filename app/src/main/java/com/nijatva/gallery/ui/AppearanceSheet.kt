package com.nijatva.gallery.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import com.nijatva.gallery.settings.AccentChoice
import com.nijatva.gallery.settings.Settings
import com.nijatva.gallery.settings.ThemeChoice

/**
 * The Appearance panel, laid out the way the Nijatva site lays it out: an uppercase
 * heading in the accent colour, a Theme row of pills, an Accent colour row of swatches,
 * and a one-line note that nothing leaves the device.
 *
 * Every choice is applied and saved immediately. There is no Apply button, because a
 * theme is something you judge by looking at it.
 */
class AppearanceSheet(
    context: Context,
    private var settings: Settings,
    private val onChange: (Settings) -> Unit,
) : Dialog(context) {

    private val themePills = mutableListOf<Pair<ThemeChoice, TextView>>()
    private val accentPills = mutableListOf<Pair<AccentChoice, TextView>>()

    init {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Palette.surface)
                cornerRadius = context.dp(22).toFloat()
                setStroke(context.dp(1), Palette.hairline)
            }
            setPadding(context.dp(20), context.dp(20), context.dp(20), context.dp(18))
        }

        root.addView(
            TextView(context).apply {
                text = "APPEARANCE"
                setTextColor(Palette.accent)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                letterSpacing = 0.16f
                includeFontPadding = false
            },
        )

        root.addView(
            label(context, "Theme", 15f, Palette.onSurface).apply {
                setPadding(0, context.dp(18), 0, 0)
            },
        )

        val themeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(10), 0, context.dp(20))
        }
        ThemeChoice.values().forEachIndexed { index, choice ->
            val pill = pill(context, choice.label, choice == settings.theme)
            pill.setOnClickListener {
                settings = settings.copy(theme = choice)
                refresh()
                onChange(settings)
            }
            themePills += choice to pill
            // Equal shares of the row, so four choices fit a narrow phone instead of
            // pushing the last one off the edge.
            themeRow.addView(
                pill,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index < ThemeChoice.values().lastIndex) marginEnd = context.dp(6)
                },
            )
        }
        root.addView(themeRow)

        root.addView(label(context, "Accent colour", 15f, Palette.onSurface))

        val accentRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(10), 0, context.dp(16))
        }
        AccentChoice.values().forEachIndexed { index, choice ->
            val swatch = pill(context, choice.label, choice == settings.accent)
            swatch.setOnClickListener {
                settings = settings.copy(accent = choice)
                refresh()
                onChange(settings)
            }
            accentPills += choice to swatch
            accentRow.addView(
                swatch,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index < AccentChoice.values().lastIndex) marginEnd = context.dp(6)
                },
            )
        }
        root.addView(accentRow)

        root.addView(
            label(
                context,
                "Saved on this device only. Nothing about your choice leaves the phone.",
                12f,
                Palette.muted,
            ),
        )

        setContentView(root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCanceledOnTouchOutside(true)
    }

    /** Recolours the pills in place so the change is visible before the screen rebuilds. */
    private fun refresh() {
        themePills.forEach { (choice, pillView) ->
            val selected = choice == settings.theme
            pillView.setTextColor(if (selected) 0xFF121214.toInt() else Palette.onSurface)
            pillView.background = if (selected) {
                rounded(context, Palette.accent, 100)
            } else {
                rounded(context, 0x00000000, 100, Palette.hairline)
            }
        }
        accentPills.forEach { (choice, pillView) ->
            val selected = choice == settings.accent
            pillView.setTextColor(if (selected) 0xFF121214.toInt() else Palette.onSurface)
            pillView.background = if (selected) {
                rounded(context, choice.argb, 100)
            } else {
                rounded(context, 0x00000000, 100, Palette.hairline)
            }
        }
    }
}
