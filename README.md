# Nijatva Gallery

A local-first gallery for Android (`com.nijatva.gallery`): folders as albums, photos and
videos in one grid, and the EXIF stripped out of a photo before it is shared.

Kotlin, no Compose, no image-loading library, no database. Plain programmatic views and
`MediaStore`, so every screen is a class you can read in one sitting.

## What it does

| Screen | What it shows |
|---|---|
| Photos | Every photo and video on the phone, square thumbnails, video length in the corner |
| Folders | One card per folder with a four-up cover, item count and total size |
| Album | One folder, same grid, same viewer |
| Viewer | Swipe between items, pinch and double-tap to zoom, video with its own controls |
| Details | Size, dimensions, date, and the camera settings read from the file's own EXIF |
| Edit | Hands the photo to Nijatva Photo Editor, or to any other editor through the chooser |
| Settings | Appearance, sort order, grid columns, videos on or off, EXIF stripping, autoplay |
| Opened from elsewhere | `VIEW` of a photo or video from another app, with Back returning to the grid |

Sharing never hands out the original. The app copies the file into its own cache, removes
the identifying tags from the copy, and shares that — so the photo in your library is
untouched and the copy that leaves the phone carries no location, no camera model and no
capture time. See [docs/EXIF-STRIPPING.md](docs/EXIF-STRIPPING.md) for the exact tag list.

## Being the default gallery

The manifest declares what makes an app a gallery rather than just an app with a grid:

```xml
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.APP_GALLERY" />

<action android:name="android.intent.action.VIEW" />   <!-- image/* and video/* -->
<category android:name="android.intent.category.DEFAULT" />
```

With those, the app shows up under **Settings -> Default apps -> Photos** and in the
"Open with" sheet. A file opened that way is shown in the viewer with the library stacked
underneath it, so Back goes to the grid.

## What it deliberately does not do

- **No cloud, no sync, no backup.** There is no account, no upload and no server. The
  library is read from the phone on each launch and kept in memory.
- **No editing.** The app has read access only; it cannot crop, rotate, move or delete a
  file. That is why there is no delete button: a gallery that can silently destroy a photo
  is a worse gallery.
- **No INTERNET permission.** No analytics, no ads, no crash reporting service, no fonts
  fetched at runtime. The privacy policy and the licence list ship inside the app because
  there is no network to fetch them over.
- **No face grouping, no AI search, no "memories".** Those need a model and an index; this
  app keeps neither.
- **No custom album database.** Folders are albums. If you want a photo in an album, move
  it into a folder in your file manager.

## Appearance

The same four themes and three accents as the rest of the suite, matching the Nijatva
site's Appearance panel:

- Themes: System, Dark, Light, Black
- Accents: Green, Saffron (default), Indigo

The Developer page carries the site's figures with the date they were read, because the
app has no way to fetch them.

## Build

1. Open the project folder in Android Studio (Ladybug or newer).
2. Android Studio fetches the Gradle distribution on first sync; no `gradlew` binary is
   committed here.
3. JDK 17, Android SDK 35, AGP 8.7.3, Kotlin 2.0.21.
4. `minSdk` is 29 (Android 10): the app uses `ContentResolver.loadThumbnail` and
   `MediaStore` volume APIs that arrived with scoped storage.

```
gradle :app:assembleDebug        # Gradle 8.10.2, JDK 17
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`.github/workflows/build.yml` builds the debug APK on every push, verifies its signature
and uploads it as a workflow artifact, so a build does not depend on any one machine.

### Signing

`keystore/nijatva-debug.jks` is committed on purpose. Every APK this project hands out is
a debug build, and a key that changes with every machine makes every install an uninstall.
Alias `nijatva`, store and key password `nijatva`. It signs nothing that goes to a store.

## Permissions

| Android version | Asked for | Why |
|---|---|---|
| 14+ | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED` | Read the library, and let you pick a subset |
| 13 | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` | Read the library |
| 10–12 | `READ_EXTERNAL_STORAGE` | Read the library |

No write permission, no location, no camera, no internet.

## Layout

```
app/src/main/java/com/nijatva/gallery/
  MainActivity.kt          the screen stack, the library, permissions, sharing
  NijatvaApp.kt            writes the last crash to a file
  model/                   MediaItem and Album - two data classes, no database
  library/                 MediaStore queries, the thumbnail cache, external uris
  settings/                Appearance and the preferences store
  share/                   the EXIF-stripping copy that actually gets shared
  ui/                      Palette, Glyph, and one class per screen
```

## Status

Version 0.1.2. Working: the library, folders, the viewer with zoom, video playback, EXIF
stripping on share, appearance, sorting, columns, the three text pages, and opening photos
and videos from other apps. Not in yet: multi-select, delete, rename, a search field, and a
web build. It is an APK, not a store listing.

## Licence

Apache License 2.0. See [LICENSE](LICENSE).

The launcher icon is a white flower mark on dark slate, kept as `assets/icon-source.png`
with generated adaptive-icon layers in `res/mipmap-*`.

Nijatva · a DEVIRA project · India
