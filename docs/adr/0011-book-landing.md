# 0011: The page landing moves like a book, not a ball

- **Status:** accepted, 2026-10-25. Supersedes decision 2 of [0010](0010-page-landing.md); its triggers stand, except
  that a cover Folio watched close no longer waits for the hinge to read shut.

## Context

0010 made both arrivals the same ballistic hop: the page started flat, rose 7 degrees and fell back in two or three
bounces with a tap on each touchdown. It worked but read as a rubber ball, the same on opening and on closing. A book
does neither. Shutting one, the cover speeds up under its own weight, claps once and stops dead. Opening one, the pages
spring up off the spine when it is laid flat and settle slowly, and the light reaches them last.

Measured on the Fold8: the hinge reads 0 degrees, the display switches to the cover about 100 to 170 ms later, and the
cover lights about 85 ms after that. The open screen is already lit and flat from about 125 degrees, so an open
landing cannot start lifted without popping. The cover is dark until it lights, so it can.

The taps 0010 played through `performHapticFeedback` did fire, but One UI plays them as a soft touch tick, and the
closing one came about 270 ms after the hinge shut, so it was not felt.

## Decision

1. **Closing falls shut.** When Folio watched the hinge close, the cover's fall starts at the display switch, not when
   it lights: 7 degrees to flat on gravity's curve, faster and faster for 140 ms, then a dead stop. The cover lights
   partway through and lands about 50 ms later. The crease darkens as it falls and lets go over 100 ms after. No
   bounce. The cover counts as shut once it lights, without waiting for the hinge reading; a later shut reading does
   not land it again. A cover that was already showing flat skips the fall and plays only the contact.
2. **Closing is felt when the hinge shuts.** A click and a thud (`PRIMITIVE_CLICK` then `PRIMITIVE_THUD`, or a heavy
   click where the motor lacks them) through `Vibrator` with `USAGE_PHYSICAL_EMULATION`, at the shut reading, before
   the cover lights. Without that reading it plays when the cover lands. Once per close.
3. **Opening springs up and settles once.** From flat, the moving half rises to 70% of the peak in 120 ms with a crisp
   tick (`PRIMITIVE_TICK`), then settles over 300 ms with no second bounce. The crease shade clears over 360 ms, after
   the page, so the light seems to come in last.

## Consequences

- Folio holds `android.permission.VIBRATE`, a normal permission, to play composed effects. Folio's Haptics switch and
  the Page Landing switch still turn the fold haptics off.
- The second, lighter tap is gone, and `FolioHaptic.Land` is removed.
- The display switch is the floor: the cover cannot show anything sooner than it lights.
