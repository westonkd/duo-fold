# Store listing

What Google Play shows for Folio, kept in the layout Play Console's bulk tools and F-Droid read.

- `title.txt` (30 characters at most), `short_description.txt` (80) and `full_description.txt` (4000).
- `images/icon.png`: 512 x 512, rendered from the launcher icon's own vectors.
- `images/featureGraphic.png`: 1024 x 500.
- `images/phoneScreenshots/`: the Fold8 cover screen. `images/sevenInchScreenshots/` and `images/tenInchScreenshots/`:
  the Fold8 inner screen. Taken from the release build on the emulator with Folio's own Hiroshige background and
  Android's demo-mode status bar. Retake them when a release changes what they show (REL-29).
- `changelogs/<versionCode>.txt`: what's new, 500 characters at most. 1.0.0 is `100000`.

Nothing here names Apple's products. Play's metadata and impersonation policies are strict about other companies'
trademarks, so keep it that way.
