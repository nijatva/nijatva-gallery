package com.nijatva.gallery.settings

import android.content.Context

/**
 * The order the grid is read in. Newest first is what a gallery is for; the other
 * three exist because finding one old photo is the other thing a gallery is for.
 */
enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name, A to Z"),
    LARGEST("Largest first"),
}

/**
 * Everything the Settings screen can change.
 *
 * Only settings that change real behaviour live here. There is no switch for cloud
 * backup, face grouping or a sync target, because the app does none of those and a
 * toggle that does nothing is a lie in a settings list.
 */
data class Settings(
    val theme: ThemeChoice = ThemeChoice.BLACK,
    val accent: AccentChoice = AccentChoice.SAFFRON,
    val sort: SortOrder = SortOrder.NEWEST,
    val columns: Int = 3,
    val includeVideos: Boolean = true,
    val stripExif: Boolean = true,
    val autoplayVideos: Boolean = true,
)

/**
 * Persists settings in SharedPreferences.
 *
 * Enums are stored by name, so reordering an enum cannot silently turn a saved value
 * into a different one.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("nijatva_gallery", Context.MODE_PRIVATE)

    fun load(): Settings {
        val defaults = Settings()
        return Settings(
            theme = readEnum("theme", defaults.theme),
            accent = readEnum("accent", defaults.accent),
            sort = readEnum("sort", defaults.sort),
            columns = prefs.getInt("columns", defaults.columns).coerceIn(2, 5),
            includeVideos = prefs.getBoolean("includeVideos", defaults.includeVideos),
            stripExif = prefs.getBoolean("stripExif", defaults.stripExif),
            autoplayVideos = prefs.getBoolean("autoplayVideos", defaults.autoplayVideos),
        )
    }

    fun save(settings: Settings) {
        prefs.edit()
            .putString("theme", settings.theme.name)
            .putString("accent", settings.accent.name)
            .putString("sort", settings.sort.name)
            .putInt("columns", settings.columns)
            .putBoolean("includeVideos", settings.includeVideos)
            .putBoolean("stripExif", settings.stripExif)
            .putBoolean("autoplayVideos", settings.autoplayVideos)
            .apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T {
        val stored = prefs.getString(key, null) ?: return fallback
        return runCatching { enumValueOf<T>(stored) }.getOrDefault(fallback)
    }
}
