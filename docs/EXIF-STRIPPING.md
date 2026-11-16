# EXIF stripping on share

The one promise this app makes about privacy is that the copy you share does not carry
your location or your phone's identity. This is exactly what happens.

## Why a copy

Two reasons, and both matter.

1. **The original is not ours to edit.** A gallery with read access to `MediaStore` can
   rewrite a JPEG's metadata in place, and that would be a destructive edit triggered by
   pressing "Share". So the app never touches the original.
2. **A share target cannot read a `content://` uri it was not granted.** Copying into
   `cacheDir/share/` and handing out a `FileProvider` uri means the receiving app gets a
   real file with a real name, and the grant dies with the share sheet.

`ExifStrip.prepare()` in `share/ExifStrip.kt` does the work:

```
MediaStore uri -> cacheDir/share/<name> -> ExifInterface(remove tags) -> saveAttributes()
               -> FileProvider.getUriForFile(...) -> ACTION_SEND
```

The cache directory is emptied at the start of every share, so the copy of the last photo
does not sit on disk indefinitely.

## What is removed

| Group | Tags |
|---|---|
| Where | `GPSLatitude`, `GPSLatitudeRef`, `GPSLongitude`, `GPSLongitudeRef`, `GPSAltitude`, `GPSAltitudeRef`, `GPSTimeStamp`, `GPSDateStamp`, `GPSProcessingMethod` |
| Which phone, which lens | `Make`, `Model`, `LensMake`, `LensModel`, `BodySerialNumber`, `LensSerialNumber`, `ImageUniqueID` |
| When, exactly | `DateTime`, `DateTimeOriginal`, `DateTimeDigitized`, `OffsetTime`, `OffsetTimeOriginal`, `OffsetTimeDigitized` |
| Who | `Artist`, `Copyright`, `Software`, `ImageDescription`, `UserComment` |

## What is kept

- **Orientation.** Remove it and every portrait photo arrives sideways in the other app.
- **Pixel dimensions.** Needed to display the file at all.
- **Exposure settings** — aperture, shutter, ISO, focal length. They describe the picture,
  not the person holding the phone, and a photo's own look depends on them.

## Limits, stated plainly

- **Videos are not stripped.** MP4 metadata is a different container with a different
  library, and a half-done job would be worse than none. A shared video keeps its own
  metadata; the setting's subtitle says so.
- **PNG and WebP usually carry no EXIF** to begin with; the copy is simply passed through.
- **A file whose EXIF cannot be parsed is still shared**, unstripped. The alternative —
  refusing to share — would be a worse failure, and the share text never claims more than
  the app did.
- **The app cannot strip what the receiving app re-adds.** If the target app uploads with
  its own device metadata, that is outside this app.

## The switch

`Settings -> Sharing -> Strip EXIF on share` is **on** by default. Turn it off and the copy
is still made — the original is still never handed out — but the tags are left alone.
