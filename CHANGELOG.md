# Changelog

## 0.1.3 — 17 November 2026

- Developer page and the feedback mail now use `Vasudev.work@rediffmail.com`, the address
  the rest of the suite and the site already publish.

## 0.1.2 — 15 November 2026

- The app now declares the gallery intent filters, so it appears in
  Settings -> Default apps -> Photos and in the "Open with" sheet for photos and videos.
  `MAIN`/`APP_GALLERY`, `VIEW` for `image/*` and `VIEW` for `video/*`.
- Opening a photo or video from another app shows it in the viewer, with the gallery
  underneath: Back returns to the grid instead of leaving the app.
- `launchMode` is `singleTop`, so a second shared file reuses the running app.
- The debug keystore is committed and wired into the build, so upgrades install over each
  other instead of needing an uninstall.

## 0.1.1 — 15 November 2026

- Fixes the launch crash on Android 15 and 16: the window's insets controller was read
  before `setContentView`, which throws on those versions. The palette is now applied
  after the content is set, and skipped when there is no decor view yet.
- Content is padded for the system bars, because Android 15+ draws edge to edge whatever
  the app asks for.
- New launcher icon: a white flower mark on dark slate, with generated adaptive-icon
  layers and a round variant.
- Startup failures now appear on screen with a copyable stack trace instead of closing
  the app silently.

## 0.1.0 — 15 November 2026

First cut.

- Photos tab: every photo and video in one square grid, newest first, video length in the
  corner.
- Folders tab: one card per folder with a four-up cover, item count and total size.
- Album screen: the same grid, scoped to one folder.
- Viewer: swipe to page, pinch and double-tap to zoom, video with the platform controls,
  details sheet with the file's own EXIF.
- Sharing through a cache copy with the identifying EXIF removed (location, camera, lens,
  serials, software, author, capture time). The original is never modified.
- Appearance: System, Dark, Light and Black themes; Green, Saffron and Indigo accents.
- Settings: sort order, grid columns, videos on or off, EXIF stripping, autoplay, plus
  in-app licences, privacy policy and terms.
- Developer page, matching the Nijatva site's layout and figures.
- No INTERNET permission, no analytics, no account, no ads.

Not in yet: multi-select, delete, rename, search, a web build, and unit tests.
