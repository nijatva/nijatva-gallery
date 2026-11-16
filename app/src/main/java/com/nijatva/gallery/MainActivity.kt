package com.nijatva.gallery

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nijatva.gallery.library.ExternalItems
import com.nijatva.gallery.library.MediaRepository
import com.nijatva.gallery.library.Thumbnails
import com.nijatva.gallery.model.Album
import com.nijatva.gallery.model.MediaItem
import com.nijatva.gallery.settings.Settings
import com.nijatva.gallery.settings.SettingsStore
import com.nijatva.gallery.share.ExifStrip
import com.nijatva.gallery.ui.AlbumPage
import com.nijatva.gallery.ui.DeveloperPage
import com.nijatva.gallery.ui.Host
import com.nijatva.gallery.ui.HomePage
import com.nijatva.gallery.ui.LegalPage
import com.nijatva.gallery.ui.Palette
import com.nijatva.gallery.ui.Route
import com.nijatva.gallery.ui.SettingsPage
import com.nijatva.gallery.ui.ViewerPage
import com.nijatva.gallery.ui.dp
import com.nijatva.gallery.ui.label
import com.nijatva.gallery.ui.rounded
import java.util.concurrent.Executors

/**
 * The only activity. It owns the screen stack, the library and the settings, and it is
 * the one place that can talk to the system: permissions, the share sheet, a browser.
 *
 * Screens are rebuilt from a [Route] rather than kept alive. That is what makes a theme
 * change apply everywhere at once - the alternative is a restyle method on every screen
 * and one of them being forgotten.
 */
class MainActivity : AppCompatActivity(), Host {

    private lateinit var container: FrameLayout
    private lateinit var store: SettingsStore
    private lateinit var repository: MediaRepository
    private lateinit var thumbnails: Thumbnails

    private val routes = ArrayDeque<Route>()
    private var library: List<MediaItem> = emptyList()
    private var albumList: List<Album> = emptyList()
    private var loading = false

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override var settings: Settings = Settings()
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A gallery that closes on launch tells you nothing. Everything below runs
        // inside a guard: if startup throws, the stack trace is written to a file and
        // shown on screen, selectable, with a Copy button.
        try {
            start()
        } catch (t: Throwable) {
            showReport("Startup failed", report(t))
        }
    }

    private fun start() {
        val previous = NijatvaApp.crashFile(this)
        if (previous.exists() && previous.length() > 0L) {
            val text = runCatching { previous.readText() }.getOrDefault("Unreadable crash file")
            showReport("The last launch crashed", text) {
                previous.delete()
                start()
            }
            return
        }

        store = SettingsStore(this)
        settings = store.load()
        repository = MediaRepository(this)
        thumbnails = Thumbnails.get(this)

        container = FrameLayout(this).apply { setBackgroundColor(Palette.background) }
        setContentView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        // Only now does the window have a decor view. Asking for the insets controller
        // before setContentView throws a NullPointerException on Android 16.
        applyPalette()

        // Android 15 and later draw edge to edge whatever the app asks for, so the bars
        // are padded in rather than drawn over.
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    back()
                }
            },
        )

        routes.addLast(Route.Home)
        // A photo opened from another app (or from the file manager) lands on top of the
        // gallery, so Back returns to the grid rather than out of the app.
        consumeViewIntent(intent)
        render()

        if (hasPermission()) reloadLibrary() else requestAccess()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeViewIntent(intent)
        render()
    }

    /** ACTION_VIEW is how another app, or the system's "Open with", hands over a file. */
    private fun consumeViewIntent(incoming: Intent?) {
        if (incoming == null || incoming.action != Intent.ACTION_VIEW) return
        val data = incoming.data ?: return
        routes.addLast(Route.External(data.toString()))
    }

    private fun report(error: Throwable): String = buildString {
        appendLine("Nijatva Gallery " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
        appendLine("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        appendLine(Build.MANUFACTURER + " " + Build.MODEL)
        appendLine()
        appendLine(error.toString())
        appendLine()
        appendLine(error.stackTraceToString())
    }

    /**
     * The failure screen. It is deliberately plain: black, monospace, selectable, and
     * with a Copy button, because the only useful thing it can do is get the trace to
     * whoever can fix it.
     */
    private fun showReport(title: String, text: String, retry: (() -> Unit)? = null) {
        runCatching { NijatvaApp.crashFile(this).writeText(text) }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            setBackgroundColor(0xFF141416.toInt())
        }

        column.addView(label(this, title, 19f, 0xFFFFFFFF.toInt(), medium = true))
        column.addView(
            label(
                this,
                "This is the error the app hit. It is also saved in the app's files folder as last_crash.txt.",
                12.5f,
                0xFFA8A8AE.toInt(),
            ).apply { setPadding(0, dp(8), 0, dp(16)) },
        )

        column.addView(
            TextView(this).apply {
                this.text = text
                setTextColor(0xFFE8E8EC.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
                setTextIsSelectable(true)
            },
        )

        column.addView(
            label(this, "Copy report", 14f, 0xFF121214.toInt(), medium = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(13), 0, dp(13))
                background = rounded(this@MainActivity, 0xFFD98431.toInt(), 100)
                isClickable = true
                setOnClickListener {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Nijatva crash", text))
                    toast("Copied")
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(22) },
        )

        if (retry != null) {
            column.addView(
                label(this, "Continue", 14f, 0xFFFFFFFF.toInt(), medium = true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(13), 0, dp(13))
                    background = rounded(this@MainActivity, 0x00000000, 100, 0xFF3A3A40.toInt())
                    isClickable = true
                    setOnClickListener { retry() }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(10) },
            )
        }

        val scroll = ScrollView(this).apply { addView(column) }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        // A photo taken in another app while this one was in the background should be
        // here when it comes back.
        if (hasPermission()) reloadLibrary()
    }

    // ---------------------------------------------------------------- Host

    override fun updateSettings(next: Settings) {
        settings = next
        store.save(next)
        applyPalette()
        render()
    }

    override fun open(route: Route) {
        routes.addLast(route)
        render()
    }

    override fun back() {
        if (routes.size > 1) {
            routes.removeLast()
            render()
        } else {
            finish()
        }
    }

    override fun thumbs(): Thumbnails = thumbnails

    override fun items(): List<MediaItem> =
        if (settings.includeVideos) library else library.filter { !it.isVideo }

    override fun albums(): List<Album> = repository.albums(items())

    override fun itemsFor(albumId: Long?): List<MediaItem> {
        val source = if (albumId == null) {
            items()
        } else {
            albums().firstOrNull { it.id == albumId }?.items ?: emptyList()
        }
        return repository.sorted(source, settings.sort)
    }

    override fun reloadLibrary() {
        if (loading) return
        loading = true
        worker.execute {
            val loaded = repository.load()
            main.post {
                library = loaded
                albumList = repository.albums(
                    if (settings.includeVideos) loaded else loaded.filter { !it.isVideo },
                )
                loading = false
                if (routes.lastOrNull() == Route.Home) render()
            }
        }
    }

    override fun isLibraryLoading(): Boolean = loading

    override fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("No app on this phone can open that link.")
        }
    }

    override fun share(item: MediaItem) {
        toast("Preparing the copy…")
        val strip = settings.stripExif
        worker.execute {
            val uri = try {
                ExifStrip.prepare(this, item, strip)
            } catch (t: Throwable) {
                null
            }
            main.post {
                if (uri == null) {
                    toast("Could not prepare this file to share.")
                } else {
                    launchShareSheet(item, uri, strip)
                }
            }
        }
    }

    override fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- screens

    private fun render() {
        container.removeAllViews()
        val page: View = if (!hasPermission()) {
            permissionPage()
        } else {
            build(routes.lastOrNull() ?: Route.Home)
        }
        container.addView(
            page,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun build(route: Route): View = when (route) {
        Route.Home -> HomePage(this, this)
        is Route.Album -> {
            val album = albumList.firstOrNull { it.id == route.id } ?: albums().firstOrNull { it.id == route.id }
            if (album == null) HomePage(this, this) else AlbumPage(this, this, album)
        }
        is Route.Viewer -> ViewerPage(this, this, route.albumId, route.index)
        is Route.External -> ViewerPage(
            this,
            this,
            albumId = null,
            startIndex = 0,
            explicit = listOf(ExternalItems.describe(this, Uri.parse(route.uri))),
        )
        Route.Settings -> SettingsPage(this, this)
        Route.Developer -> DeveloperPage(this, this)
        is Route.Legal -> LegalPage(this, this, route.kind)
    }

    // ---------------------------------------------------------------- theme

    private fun applyPalette() {
        val darkSystem =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        Palette.apply(settings.theme, settings.accent, darkSystem)

        // No decor view yet means no insets controller: the colours are applied by the
        // next call, which always runs after setContentView.
        if (window.peekDecorView() == null) return

        window.statusBarColor = Palette.background
        window.navigationBarColor = Palette.background

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val light = if (Palette.isLight) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0
            window.insetsController?.setSystemBarsAppearance(
                light,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                if (Palette.isLight) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
        }
    }

    // ---------------------------------------------------------------- permissions

    private fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun hasPermission(): Boolean =
        mediaPermissions().any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestAccess() {
        ActivityCompat.requestPermissions(this, mediaPermissions(), REQUEST_MEDIA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_MEDIA) return

        if (hasPermission()) {
            render()
            reloadLibrary()
        } else {
            toast("The gallery needs read access to show your photos.")
        }
    }

    /**
     * Shown instead of the library when there is no permission. It says what the app
     * does with the access, which is more than the system dialog can say.
     */
    private fun permissionPage(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(30), 0, dp(30), 0)
            setBackgroundColor(Palette.background)
        }

        page.addView(
            label(this, "Photos and videos", 22f, Palette.onSurface, medium = true).apply {
                gravity = Gravity.CENTER
            },
        )

        page.addView(
            label(
                this,
                "The gallery reads your library through Android's media store. It asks for " +
                    "read access only: it cannot edit or delete anything, it has no internet " +
                    "permission, and nothing it reads leaves the phone.",
                14f,
                Palette.muted,
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(dp(4).toFloat(), 1f)
            },
        )

        page.addView(
            label(this, "Allow access", 15f, 0xFF121214.toInt(), medium = true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(26), dp(13), dp(26), dp(13))
                background = rounded(this@MainActivity, Palette.accent, 100)
                isClickable = true
                setOnClickListener { requestAccess() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(22)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        return page
    }

    // ---------------------------------------------------------------- sharing

    private fun launchShareSheet(item: MediaItem, uri: Uri, stripped: Boolean) {
        val type = when {
            item.mimeType.isNotBlank() -> item.mimeType
            item.isVideo -> "video/*"
            else -> "image/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = type
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                if (stripped && !item.isVideo) {
                    "Shared from Nijatva Gallery. Location, camera and capture time removed."
                } else {
                    "Shared from Nijatva Gallery."
                },
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Share"))
    }

    private companion object {
        const val REQUEST_MEDIA = 41
    }
}
