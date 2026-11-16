package com.nijatva.gallery.settings

/**
 * Theme and accent, the same four themes and three accents the Nijatva site offers on
 * its Appearance panel.
 *
 * Theme is the surface the app draws on; accent is the one colour used for selected
 * pills, section headings and the tab indicator. Both are stored with the rest of the
 * settings, so the app opens in the theme it was left in.
 */
enum class ThemeChoice(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
    BLACK("Black"),
}

/**
 * Saffron is the default: it is the Nijatva colour, and the one the site's Appearance
 * panel starts on.
 */
enum class AccentChoice(val label: String, val argb: Int) {
    GREEN("Green", 0xFF1E7A46.toInt()),
    SAFFRON("Saffron", 0xFFD98431.toInt()),
    INDIGO("Indigo", 0xFF3F45A8.toInt()),
}
