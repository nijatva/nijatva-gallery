# Architecture

One activity, a stack of routes, and views built in Kotlin. There is no Compose, no
navigation library, no dependency injection and no database.

## The screen stack

`MainActivity` holds an `ArrayDeque<Route>`. `Route` is a sealed interface with one case
per screen (`Home`, `Album`, `Viewer`, `External`, `Settings`, `Developer`, `Legal`).
Pushing a route rebuilds the top view; going back pops and rebuilds the one below.

Screens are rebuilt rather than kept alive for one reason: a theme change has to apply
everywhere at once. The alternative is a `restyle()` on every screen, and the certainty
that one of them gets forgotten.

The cost is real and accepted: scroll position is not preserved across a back press or a
theme change. A gallery of a few thousand items reads in well under a second from
`MediaStore`, so a rebuild is cheap.

## The library

`MediaRepository` reads two `MediaStore` queries — images, then videos — into a
`List<MediaItem>`, once per launch and again on resume. There is no index of our own, so
there is nothing to get out of step with the phone.

- Albums are `items.groupBy { bucketId }`. A folder is an album; that is the whole model.
- Sorting is in memory, on the list already read.
- Filtering videos is a `filter` on the same list.

`Thumbnails` decodes with `ContentResolver.loadThumbnail` on three worker threads behind an
`LruCache` of an eighth of the heap. Every request tags its target `ImageView`; a decoded
bitmap is only applied if the tag still matches, which is what stops a fast scroll from
showing the wrong photo in a recycled cell.

The viewer decodes full-size photos with `ImageDecoder` at screen resolution, downsampled
during the read rather than after it.

## Files from other apps

`ACTION_VIEW` hands over a uri the app has no `MediaStore` row for, so `ExternalItems`
describes it: display name and size from `OpenableColumns`, mime type and dimensions when
the provider answers, and a `MediaItem` that the same viewer can page through. A uri that
answers nothing still opens.

## Colour

`Palette` is a mutable singleton of seven ARGB values plus the accent. `MainActivity`
applies it from the saved settings on every rebuild; views read it at construction time. A
theme change therefore takes effect on the next frame, with no observer to subscribe.

The palette is applied **after** `setContentView`, and skipped while `window.peekDecorView()`
is null: reading the window's insets controller before there is a decor view throws a
`NullPointerException` on Android 15 and later.

`Glyph` draws the app's icons on a canvas instead of shipping vector drawables: a dozen
glyphs is a hundred lines, and each one is tinted from `Palette` at construction.

## Sharing

`share/ExifStrip.kt` owns the only file the app ever writes. See
[EXIF-STRIPPING.md](EXIF-STRIPPING.md).

## What is not here, and why

| Missing | Why |
|---|---|
| Database / Room | Nothing to store. The library is the phone's, read fresh. |
| Compose | The suite's other Android apps are programmatic views; matching them keeps one mental model. |
| Coil / Glide | `loadThumbnail` plus an `LruCache` is thirty lines and handles video posters too. |
| Media3 | `VideoView` plays an MP4 with controls and adds no dependency. |
| WorkManager | Nothing runs in the background. Nothing syncs. |
