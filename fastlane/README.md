# Store listing

What Google Play shows for Twofold, kept in the layout Play Console's bulk tools and F-Droid read.

- `title.txt` (30 characters at most), `short_description.txt` (80) and `full_description.txt` (4000).
- `images/icon.png`: 512 x 512, opaque, rendered from `website/brand/twofold-icon-square.svg` (Play rounds the
  corners itself). The launcher icon in `app/src/main/res/drawable/` draws the same artwork as adaptive layers.
- `images/featureGraphic.png`: 1024 x 500, no transparency: the icon, the name and "Home, made for the fold." beside
  the devices from the last tablet screenshot, in the website's share card style.
- `images/phoneScreenshots/` (1080 x 1920) and `images/sevenInchScreenshots/` and `images/tenInchScreenshots/`
  (1920 x 1080): designed screenshots with a headline over Fold8 captures from the release build, with Twofold's own
  Hiroshige background and Android's demo-mode status bar. Retake them when a release changes what they show (REL-29).
- `changelogs/<versionCode>.txt`: what's new, 500 characters at most, named after the `versionCode` it ships with.

Nothing here names Apple's products. Play's metadata and impersonation policies are strict about other companies'
trademarks, so keep it that way.
