# Twofold 1.0.0 on Google Play

The runbook for Twofold's first Play release, written before the build ([REL-26](../standards/releases.md)). The
repository side is done in the pull requests that added this file; everything below is what happens in Play Console
and on your machine.

| | |
|---|---|
| Package | `com.westonkd.folio` (permanent after the first upload) |
| Version | `1.0.0`, `versionCode` 100000 |
| Store title | Twofold: Foldable Launcher |
| Category | Personalization |
| Privacy policy | <https://github.com/westonkd/duo-fold/blob/main/PRIVACY.md> |
| Listing text and graphics | `fastlane/metadata/android/en-US/` |
| Min / target SDK | 31 (Android 12) / 36 (Android 16, Play's floor since 31 Aug 2026) |

## 1. Before anything else

- [ ] **Developer account.** A personal account made after 13 Nov 2023 has to run a **closed test with at least 12
  testers opted in for 14 days in a row** before Production unlocks. Plan for that: the earliest public date is about
  two weeks after the closed test starts. An organization account skips it.
- [ ] **Identity verification** in Play Console must be finished before the app can be published.
- [ ] **Merge** the Play pull requests in order, then the version bump.
  The privacy policy URL is a 404 until `PRIVACY.md` is on `main`, and Play checks it when you save App content,
  so merge before filling in section 3.

## 2. Upload key and the bundle

Play App Signing holds the key users' phones trust; you keep an **upload key**. Make it once, keep it and its
passwords outside the repository (`.gitignore` already refuses `*.jks`), and back it up.

```bash
keytool -genkeypair -v -keystore ~/keys/folio-upload.jks -alias folio-upload -keyalg RSA -keysize 4096 -validity 10000
```

Build the bundle from a clean `main`:

```bash
FOLIO_RELEASE_STORE_FILE=~/keys/folio-upload.jks FOLIO_RELEASE_KEY_ALIAS=folio-upload \
FOLIO_RELEASE_STORE_PASSWORD=... FOLIO_RELEASE_KEY_PASSWORD=... bash tools/build-play-bundle.sh
```

The script refuses a dirty tree, runs the unit tests and release lint, builds
`app/build/outputs/bundle/release/app-release.aab`, and checks the package, version and signer. The first upload
in Play Console asks to enroll in Play App Signing: accept Google-generated app signing key.

## 3. App content (Policy › App content)

| Form | Answer |
|---|---|
| Privacy policy | The URL above |
| Ads | No ads |
| App access | All functionality is available without special access. Note for reviewers: "Twofold is a Home screen. Open it, tap Use as Home, and choose Twofold. No account or login." |
| Content rating (IARC) | Category: Utility, Productivity, Communication or Other. Answer No to violence, sexuality, language, controlled substances, gambling, user interaction/chat, sharing location, digital purchases. Expect Everyone / PEGI 3 |
| Target audience | 13 and over (all three adult ranges). Not designed for children, so the Families policy doesn't apply |
| News app | No |
| Health, financial features, government | No |
| Data safety | See below |
| Accessibility API declaration | See below |

### Data safety

- Does your app collect or share any of the required user data types? **No.**
- Why that's true: Twofold has no `INTERNET` permission, so nothing leaves the phone by itself. Data it reads
  (installed apps, notifications, contacts, calendar, usage) is processed on the device only, which Play doesn't
  count as collection. Sharing a problem report is a user-initiated share through Android's share sheet to an app the
  user picks, not to the developer.
- Encryption in transit and deletion requests then don't apply.

### Accessibility API declaration

Twofold's gestures service is not an accessibility tool (`isAccessibilityTool` is unset). Play Console › App content ›
Sensitive permissions and APIs › Accessibility API:

- **Core functionality:** "Twofold is a Home screen. Its optional gestures service lets a swipe down on Home open
  Android's notification shade or Quick Settings, runs system actions the user assigns to gestures and on-screen
  buttons (Home, Back, Recents, close notifications, Android's app drawer, lock screen, screenshot, power menu, split screen, play/pause) through
  performGlobalAction, and shows the user's dock and navigation buttons over other apps as accessibility overlays when
  the user turns those on. It subscribes to no accessibility events, cannot retrieve window content
  (canRetrieveWindowContent=false), and collects or shares no data."
- **Personal and sensitive data:** none accessed through the API.
- **Video:** Play wants a link (an unlisted YouTube video works). One recorded from the release build on the Fold8
  emulator, installed as if from Play, is at `artifacts/play/folio-accessibility-declaration.mp4` (git-ignored, so
  only on the machine that made it). To record it again:
  1. Open Twofold, Settings › General › Privacy & Permissions › Gestures service.
  2. Show the Turn On Twofold Gestures disclosure, scroll it, tap **Agree**.
  3. Turn on "Twofold gestures & overlays" in Android's Accessibility settings and accept Android's dialog.
  4. Back on Home, swipe down from the top right: Quick Settings opens. Swipe down on Home with the setting set to
     Notifications: the shade opens.
  5. Turn on Big Buttons in Settings › Gestures and show them over another app.

### Permissions Play may ask about

None of Twofold's permissions are on Play's restricted list (no SMS, call log, all files, `QUERY_ALL_PACKAGES`,
install packages, background location or exact alarms). `PACKAGE_USAGE_STATS` and notification access are special
access the user grants in Android's settings, each behind an in-app disclosure.

## 4. Store listing

Everything is in `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt`, `full_description.txt`,
`images/icon.png` (512 x 512), `images/featureGraphic.png` (1024 x 500), and screenshots for phones and tablets
taken from the release build on the emulator. The changelog for this version is `changelogs/100000.txt`.

Keep the listing free of Apple's names (iPhone, iOS, iPad, Spotlight, Dynamic Island, Liquid Glass): Play's
metadata and impersonation policies are strict about other companies' trademarks, and a rejection costs days.

Contact details: an email address is required and shown publicly. The website can be the GitHub repository.

## 5. Tracks

1. **Internal testing:** upload the bundle, add yourself, install from the Play link on the Fold8 and check
   first run, Use as Home, the gestures disclosure, and the pre-launch report (Release › Testing › Pre-launch report).
2. **Closed testing:** at least 12 testers for 14 days if the account needs it. Countries: wherever you like.
3. **Production:** apply for access when the closed test qualifies, then roll out (a staged rollout at 20% is a
   cheap safety net).

## 5a. Devices

Twofold supports phones, foldables and tablets on Android 12 and up. Tested for this release on the emulator, with the
release build, at: Galaxy Z Fold8 inner and cover, Pixel Fold, Pixel 8 (Android 16), a 360 x 640 dp phone, a 320 dp
wide phone, a square flip cover, 1000 to 1440 dp wide tablets in landscape and portrait, and font scales 1.3 and 2.0.
Device catalog: nothing needs excluding; Android TV, Wear OS and Automotive don't match the manifest anyway.

## 6. After it's live

- The Twofold Dev build on your phone is `com.mccal.folio.dev` and stays separate. Installing Twofold from Play adds a
  second app; move your layout across with Settings › General › Save Backup, then Restore from Backup in the Play
  build.
- A Play package name and its signing key are permanent. Losing the upload key is recoverable (Play Console can reset
  it); losing access to the account is not, so keep its recovery details current.
