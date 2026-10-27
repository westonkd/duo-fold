# Folio

A calm, two-panel Home screen for Android foldables, made first for the Galaxy Z Fold8 and published on Google Play
as **Folio: Home for Foldables** (`com.westonkd.folio`). It is a fork of
[McCal-Codes/folio](https://github.com/McCal-Codes/folio), maintained separately.

## Thank you, McCal

Folio is McCal's work far more than it is mine. In about six weeks McCal wrote more than 700 commits: the Side Bar,
the App Library, Spotlight, folders, jiggle mode, Smart Stacks, the fold animation, the glass, the motion system and
the standards in `docs/` that keep it all honest. McCal built it in the open, for fun, out of affection for iOS and the
jailbreak tweaks that made iPhones feel personal, and gave it away under MIT. Every good idea in this fork started in
that repository. If you want a launcher with all the options, go use [the real Folio](https://github.com/McCal-Codes/folio).

## Goals

Folio should feel like you are holding an iPhone Duo running iOS 27, and it should get there by doing fewer things
well instead of doing everything.

1. **Fidelity to iOS 27 and iPhone Duo.** Apple's current design is the reference for every surface: Liquid Glass as
   iOS 27 tuned it (calmer and easier to read than iOS 26), the Side Bar with its clock, Ring and vertical dock, the
   round Search button, the App Library, folders and Spotlight. When Folio and the iPhone disagree, the iPhone is
   right unless the Fold's hardware says otherwise.
2. **One good answer instead of a setting.** If a choice can be made once, it is made once. Light appearance only,
   Liquid glass only, fixed icon sizes and spacing. Settings is five short pages and should keep shrinking.
3. **Polish over features.** New work goes into consistency and motion: the same corners, glass, type and springs
   everywhere, and every surface growing out of the thing you touched and going back into it. A feature that can't be
   made to feel finished is a candidate for removal.
4. **Android does what Android is good at.** The lock screen, recents, notifications and Quick Settings are your phone's.
   Folio doesn't draw its own Dynamic Island, Control Center or Notification Center.
5. **Private and offline.** No Market, codes, update checks, accounts or network use. Folio declares no internet
   permission; [PRIVACY.md](PRIVACY.md) is the privacy policy and [PERMISSIONS.md](PERMISSIONS.md) lists every
   permission and what uses it.

[docs/ios27-alignment.md](docs/ios27-alignment.md) lists where Folio still falls short of iOS 27 and what to work on
next.

## What it is

Folio replaces your phone's Home screen. On both screens it gives you iPhone Duo's layout: Home pages of apps and widgets,
a Side Bar along the edge with the time, a battery Ring and the dock, a widgets page left of Home, an App Library,
Spotlight and folders, and Home lands like a page with a small bounce when the phone opens flat or closes. Android
still owns the lock screen, recents and the notification shade.

## Build

Needs JDK 17 (`mise` pins it here; run `eval "$(mise env -s bash)"` first).

    ./gradlew :app:assembleFast

That is Folio Dev (`com.mccal.folio.dev`): optimized like a release but signed with the local debug key, so it
installs beside the Play build. With the phone connected over USB or wireless debugging:

    adb install --user 0 -r app/build/outputs/apk/fast/app-fast.apk

Then pick Folio Dev as the Home app in Settings › Apps › Default apps.

## Release

Play releases are built with `tools/build-play-bundle.sh` from a clean `main`. The store listing lives in
`fastlane/metadata/android/en-US/`, and [docs/releases/play-1.0.0.md](docs/releases/play-1.0.0.md) is the runbook:
signing, Play Console's forms, and the testing tracks.

## Licence and credits

MIT, see [LICENSE](LICENSE). Folio was created by [McCal](https://github.com/McCal-Codes) and started from
[DuoLauncher](https://github.com/jakesgoodapps/DuoLauncher) by jakesgoodapps (MIT, commit `f1bc0f1`), whose Side Bar
made the whole thing possible and whose notice is kept in `LICENSE`. Dependency licences are in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Apple, iPhone, Google, Android, Samsung and Galaxy are trademarks of
their owners; this project is not affiliated with them.
