# How the three Nijatva apps hand work to each other

Camera, Gallery and Photos are separate apps with separate packages. They talk over
plain Android intents, so any of them can be installed alone and nothing breaks.

| From | To | Intent | Fallback |
| --- | --- | --- | --- |
| Camera | Gallery | `ACTION_VIEW` on the saved `content://` uri, `setPackage("com.nijatva.gallery")`, read grant | `ACTION_VIEW` with no package, then a chooser |
| Camera | Photos | `ACTION_EDIT` on the uri, `setPackage("com.nijatva.photos")`, read and write grant | `ACTION_EDIT` chooser |
| Camera | Gallery (app) | `ACTION_MAIN` + `CATEGORY_LAUNCHER`, package set | `ACTION_MAIN` + `CATEGORY_APP_GALLERY` |
| Gallery | Camera | `ACTION_MAIN` + `CATEGORY_LAUNCHER`, `setPackage("com.nijatva.camera")` | `INTENT_ACTION_STILL_IMAGE_CAMERA`, then `ACTION_IMAGE_CAPTURE` |

The rules behind the table:

- **Ask by package first, never assume.** A phone with all three installed goes
  straight to the right app with no chooser. A phone with one of them falls through
  to the standard action, and then to the system chooser, without an error.
- **The camera sends a uri, not a path.** Saved photos live in MediaStore, so the
  receiver gets a `content://` uri and a read grant. Nothing is copied, and the
  grant dies with the receiving activity.
- **Editing gets a write grant too**, because an editor saves over the file rather
  than beside it. A gallery viewer only ever gets read.
- **No shared code and no shared library.** The three apps keep their own palettes
  and their own settings; the only contract between them is this table.
- **The gallery accepts files as well as sending them.** Its `ACTION_VIEW` filter for
  `image/*` and `video/*` is what lets the camera's thumbnail land here, and what
  puts the app under Settings -> Default apps -> Photos.
