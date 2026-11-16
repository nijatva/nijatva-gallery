package com.nijatva.gallery.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * The Developer page, in the layout the Nijatva site uses: avatar with initials, name
 * and handle, two lines of description, a three-column figures card, contact rows, a
 * white "View GitHub Profile" button, and the projects maintained.
 *
 * The figures are the site's published numbers with the date they were read, not
 * something fetched at runtime: this app has no INTERNET permission and that is
 * deliberate.
 */
class DeveloperPage(context: Context, private val host: Host) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)

        addView(toolbar(context, "Developer", host))

        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(context.dp(20), 0, context.dp(20), context.dp(28))
        }

        content.addView(
            label(context, "About the developer", 12.5f, Palette.muted).apply {
                setPadding(context.dp(2), context.dp(6), 0, context.dp(10))
            },
        )

        val card = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dp(20), context.dp(24), context.dp(20), context.dp(20))
            background = rounded(context, Palette.surface, 22)
        }

        card.addView(
            label(context, "VD", 26f, Palette.accent, medium = true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Palette.accentSoft(46))
                }
            },
            LinearLayout.LayoutParams(context.dp(78), context.dp(78)),
        )

        card.addView(
            label(context, "Vasudev Dangaya", 21f, Palette.onSurface, medium = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(14), 0, 0)
            },
        )
        card.addView(
            label(context, "@vasudev-work", 13f, Palette.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(3), 0, context.dp(12))
            },
        )

        card.addView(
            label(
                context,
                "I build Android apps with clean UI and real, working features. " +
                    "Kotlin · Android SDK · no unnecessary dependencies.",
                13f,
                Palette.muted,
            ).apply {
                gravity = Gravity.CENTER
                setLineSpacing(context.dp(4).toFloat(), 1f)
            },
        )

        card.addView(gap(context, 18))
        card.addView(figures())
        card.addView(gap(context, 16))

        card.addView(
            label(context, "Maintains", 12f, Palette.muted).apply {
                setPadding(context.dp(2), 0, 0, context.dp(8))
            },
        )
        card.addView(maintains())

        card.addView(gap(context, 14))
        card.addView(contact("Location", "India"))
        card.addView(contact("Website", "vasudev-work.github.io"))
        card.addView(contact("Email", "Vasudev.Social@rediffmail.com"))

        card.addView(
            label(context, "View GitHub Profile", 14.5f, 0xFF121214.toInt(), medium = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(14), 0, context.dp(14))
                background = rounded(context, 0xFFF2F2F2.toInt(), 100)
                isClickable = true
                setOnClickListener { host.openLink("https://github.com/vasudev-work") }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = context.dp(18) },
        )

        content.addView(card)

        content.addView(
            label(
                context,
                "Figures read from GitHub on 15 November 2026. 3 of the 24 apps have a " +
                    "repository today; the other 21 are planned and listed on the apps page.",
                12f,
                Palette.muted,
            ).apply {
                setPadding(context.dp(2), context.dp(14), context.dp(2), 0)
                setLineSpacing(context.dp(3).toFloat(), 1f)
            },
        )

        content.addView(
            label(context, "Designed & developed by Vasudev Dangaya", 12.5f, Palette.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(26), 0, 0)
            },
        )

        content.addView(
            label(context, "MAKE IN INDIA", 12f, 0xFFFFFFFF.toInt(), medium = true, spacing = 0.12f).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(11), 0, context.dp(11))
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(0xFFF5A623.toInt(), 0xFF22A06B.toInt()),
                ).apply { cornerRadius = context.dp(100).toFloat() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = context.dp(14) },
        )

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun figures(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(14), 0, context.dp(14))
            background = rounded(context, Palette.raised, 16)
        }
        row.addView(figure("31", "Repositories"))
        row.addView(figure("24", "Apps planned"))
        row.addView(figure("3", "With code today"))
        return row
    }

    private fun figure(value: String, caption: String): View {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        column.addView(
            label(context, value, 21f, Palette.onSurface, medium = true).apply { gravity = Gravity.CENTER },
        )
        column.addView(
            label(context, caption, 11.5f, Palette.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, context.dp(3), 0, 0)
            },
        )
        column.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        return column
    }

    private fun maintains(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("N" to "Nijatva", "A" to "Aekant", "Q" to "Qmamu").forEach { (initial, name) ->
            row.addView(
                label(context, initial, 14f, Palette.accent, medium = true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(context, Palette.accentSoft(46), 12)
                    contentDescription = name
                },
                LinearLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
                    rightMargin = context.dp(8)
                },
            )
        }
        row.addView(
            chip(context, "+21", Palette.raised, Palette.muted),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return row
    }

    private fun contact(key: String, value: String): View {
        val line = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(9), 0, context.dp(9))
        }
        line.addView(
            label(context, key, 13.5f, Palette.muted),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        line.addView(
            label(context, value, 13.5f, Palette.onSurface, medium = true).apply { gravity = Gravity.END },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return line
    }
}
