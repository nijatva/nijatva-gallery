package com.nijatva.gallery.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.SwitchCompat
import com.nijatva.gallery.BuildConfig
import com.nijatva.gallery.settings.SortOrder
import com.nijatva.gallery.settings.ThemeChoice

/**
 * The settings screen.
 *
 * Every switch here changes something that happens: sorting changes the query order,
 * columns change the grid, the EXIF switch changes what leaves the phone when you
 * share, autoplay changes whether a video starts by itself. Nothing is listed that the
 * app cannot do - there is no "cloud backup" row, because there is no cloud.
 */
class SettingsPage(context: Context, private val host: Host) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)

        addView(toolbar(context, "Settings", host))

        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, 0, 0, context.dp(26))
        }

        content.addView(heading("App settings"))
        content.addView(
            row(
                "Appearance",
                host.settings.theme.label + " · " + host.settings.accent.label,
            ) { openAppearance() },
        )
        content.addView(
            toggleRow(
                "Dark theme",
                themeSubtitle(),
                isDarkNow(),
            ) { on ->
                host.updateSettings(
                    host.settings.copy(theme = if (on) ThemeChoice.DARK else ThemeChoice.LIGHT),
                )
            },
        )
        content.addView(divider(context))

        content.addView(heading("Library"))
        content.addView(row("Sort order", host.settings.sort.label) { chooseSort() })
        content.addView(row("Grid columns", host.settings.columns.toString() + " per row") { chooseColumns() })
        content.addView(
            toggleRow(
                "Show videos",
                "Videos play in the same grid as photos",
                host.settings.includeVideos,
            ) { on -> host.updateSettings(host.settings.copy(includeVideos = on)) },
        )
        content.addView(divider(context))

        content.addView(heading("Sharing"))
        content.addView(
            toggleRow(
                "Strip EXIF on share",
                "Camera, lens, GPS and the exact time are removed from the copy you share. The original is untouched.",
                host.settings.stripExif,
            ) { on -> host.updateSettings(host.settings.copy(stripExif = on)) },
        )
        content.addView(divider(context))

        content.addView(heading("Playback"))
        content.addView(
            toggleRow(
                "Autoplay videos",
                "Start playing as soon as a video is opened",
                host.settings.autoplayVideos,
            ) { on -> host.updateSettings(host.settings.copy(autoplayVideos = on)) },
        )
        content.addView(divider(context))

        content.addView(heading("Other Settings"))
        content.addView(row("Submit feedback", "Opens your mail app") { sendFeedback() })
        content.addView(
            row(
                "Version number",
                BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
            ),
        )
        content.addView(row("Open-source licences") { host.openLegal(LegalKind.LICENCES) })
        content.addView(row("Privacy policy") { host.openLegal(LegalKind.PRIVACY) })
        content.addView(row("Terms of Service") { host.openLegal(LegalKind.TERMS) })
        content.addView(row("Developer", "Vasudev Dangaya") { host.openDeveloper() })

        content.addView(footer())

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun openAppearance() {
        AppearanceSheet(context, host.settings) { next -> host.updateSettings(next) }.show()
    }

    private fun isDarkNow(): Boolean = when (host.settings.theme) {
        ThemeChoice.BLACK, ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
        ThemeChoice.SYSTEM ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun themeSubtitle(): String = when (host.settings.theme) {
        ThemeChoice.SYSTEM -> "Automatic, following the phone"
        ThemeChoice.DARK -> "Dark surfaces, one step up from black"
        ThemeChoice.BLACK -> "Pure black, for OLED screens"
        ThemeChoice.LIGHT -> "Light surfaces"
    }

    private fun chooseSort() {
        val values = SortOrder.values()
        AlertDialog.Builder(context)
            .setTitle("Sort order")
            .setSingleChoiceItems(
                values.map { it.label }.toTypedArray(),
                host.settings.sort.ordinal,
            ) { dialog, which ->
                host.updateSettings(host.settings.copy(sort = values[which]))
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseColumns() {
        val choices = intArrayOf(2, 3, 4, 5)
        val labels = choices.map { "$it per row" }.toTypedArray()
        val current = choices.indexOf(host.settings.columns).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle("Grid columns")
            .setSingleChoiceItems(labels, current) { dialog, which ->
                host.updateSettings(host.settings.copy(columns = choices[which]))
                dialog.dismiss()
            }
            .show()
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:Vasudev.work@rediffmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Nijatva Gallery feedback")
            putExtra(
                Intent.EXTRA_TEXT,
                "App version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n\n",
            )
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            host.toast("No mail app on this phone. Write to Vasudev.work@rediffmail.com")
        }
    }

    private fun heading(text: String): View = sectionHeading(context, text).apply {
        setPadding(context.dp(20), context.dp(22), context.dp(20), context.dp(8))
    }

    private fun row(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null): View {
        val line = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(20), context.dp(13), context.dp(20), context.dp(13))
            val action = onClick
            if (action != null) {
                isClickable = true
                setOnClickListener { action() }
            }
        }

        val text = LinearLayout(context).apply { orientation = VERTICAL }
        text.addView(label(context, title, 15.5f, Palette.onSurface, medium = true))
        if (subtitle != null) {
            text.addView(
                label(context, subtitle, 12.5f, Palette.muted).apply {
                    setPadding(0, context.dp(3), 0, 0)
                },
            )
        }
        line.addView(
            text,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        return line
    }

    private fun toggleRow(
        title: String,
        subtitle: String?,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ): View {
        val line = row(title, subtitle)
        val switch = SwitchCompat(context).apply {
            isChecked = checked
            thumbTintList = ColorStateList.valueOf(Palette.accent)
            trackTintList = ColorStateList.valueOf(Palette.hairline)
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        (line as LinearLayout).addView(switch)
        return line
    }

    private fun footer(): View {
        val footer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dp(20), context.dp(30), context.dp(20), 0)
        }

        footer.addView(
            label(context, "Designed & developed by Vasudev Dangaya", 12.5f, Palette.muted).apply {
                gravity = Gravity.CENTER
            },
        )

        val bar = label(context, "MAKE IN INDIA", 12f, 0xFFFFFFFF.toInt(), medium = true, spacing = 0.12f).apply {
            gravity = Gravity.CENTER
            setPadding(0, context.dp(11), 0, context.dp(11))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFF5A623.toInt(), 0xFF22A06B.toInt()),
            ).apply { cornerRadius = context.dp(100).toFloat() }
        }
        footer.addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = context.dp(14) },
        )
        return footer
    }
}
