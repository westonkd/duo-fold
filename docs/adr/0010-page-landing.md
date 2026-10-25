# 0010: The fold animation is a page landing, triggered by the hinge, not a timed fade

- **Status:** accepted, 2026-10-24. Supersedes [0009](0009-duet-effect-engine.md).

## Context

Duet copied iPhone Duo's frost on the moving half, which Apple drives from a continuous hinge angle. The Fold8's
public hinge sensor reports only 0, 90 and 180 degrees, about 200 ms apart, and the inner panel lights at about 125
degrees. Folio filled the gaps with time: a fixed 520 ms fade from the moment the panel lit, a fold that built at a
learned speed, and a cover that eased in over a fixed time. Those timings could never line up with the hand, so the
effect looked late or early on most folds.

## Decision

1. **Nothing animates while the hinge moves.** The animation plays only when the phone arrives: the hinge reads flat
   on the open screen, or the cover lights with the hinge shut (or shuts while the cover is on). With no hinge sensor,
   the panel lighting stands in.
2. **The arrival is a page landing.** The moving half of the open screen, or the whole cover about its hinge edge,
   tilts toward the eye and falls back flat in two or three ballistic bounces (7 degrees at most, about 370 ms), with a
   soft shade in the crease while it is lifted, and a haptic tap on the first touchdown and a lighter one on the second.
3. **Folio's own shader.** `PageLandingShader` is a plain perspective projection written for Folio. Duet's shader, its
   ported code and its notices are removed, with fold motion (ripple, depth, light) and the learned fold timing.
4. **The stay-awake bridge is unchanged in spirit:** it still starts when the hinge leaves flat and is cancelled when
   the phone opens back up, but it is driven by hinge readings alone.

## Consequences

- One switch in Settings › Fold turns it off; Reduce Motion keeps only the haptic.
- Because the flat reading can trail the real hinge by up to 200 ms, the landing can start a moment after the phone is
  flat. That is a smaller and more consistent error than a fade timed from the panel lighting.
- Nothing to keep in step with third-party code any more: `third_party/duet/` is gone.
