# Folio

An iPhone-style Home app for the Galaxy Z Fold8. This is a personal fork of
[McCal-Codes/folio](https://github.com/McCal-Codes/folio), made for one phone and one person, and it is not
published anywhere.

## What it is

Folio replaces One UI's Home screen. On both screens it gives you iPhone Duo's layout: Home pages of apps and widgets,
a Side Bar along the edge with the time, a battery Ring and the dock, an App Library, Spotlight, folders and Duet, the
iPhone Duo fold animation. Android still owns the lock screen, recents and the notification shade.

## Opinionated choices

- Light appearance and Liquid glass only. There are no themes, accents or glass styles.
- No Dynamic Island, no Control Center and no Notification Center of Folio's own. Pulling down opens Android's shade.
- Sizes and spacing are fixed. Settings is five short pages: Home, Search, Gestures, Fold and General.
- No Market, codes, update checks or network use.

## Build

Needs JDK 17 (`mise` pins it here; run `eval "$(mise env -s bash)"` first).

    ./gradlew :app:assembleFast

## Install

With the phone connected over USB or wireless debugging:

    adb install --user 0 -r app/build/outputs/apk/fast/app-fast.apk

Then pick Folio Dev as the Home app in Settings › Apps › Default apps.

## Licence and credits

MIT, see [LICENSE](LICENSE). Folio started from [DuoLauncher](https://github.com/jakesgoodapps/DuoLauncher) by
jakesgoodapps (MIT, commit `f1bc0f1`), whose notice is kept in `LICENSE`. Duet's shader includes MIT code from
[Duo Fold Live](https://github.com/joeconsorti/duo-fold-live), [iphone-duo](https://github.com/chuspeeism/iphone-duo)
and [hingewave](https://github.com/Ant-lib/hingewave); see `third_party/duet/PROVENANCE.md`. Dependency licences are
in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Apple, iPhone, Google, Android, Samsung and Galaxy are trademarks
of their owners; this project is not affiliated with them.
