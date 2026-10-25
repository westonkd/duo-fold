# Architecture decision records

Short records of decisions that shape Folio, why they were made, and what they cost. New records get the next number, and old ones are never rewritten: a later record supersedes an earlier one.

| # | Decision | Status |
|---|---|---|
| [0006](0006-standards.md) | Folio Standards are the build contract; outside AI agents only test and fix bugs | Proposed |
| [0007](0007-release-trains.md) | One trunk, two audiences: features gated by the `beta` scope, betas are tags, and `versionCode` gets hotfix headroom | Proposed |
| [0008](0008-platform-architecture.md) | A launcher first, then an extension platform, then an optional system bridge: six core primitives, privilege tiers A0 to A5, packages never inherit privilege | Proposed |
| [0009](0009-duet-effect-engine.md) | Duet is the fold animation; its looks are a catalog ported only from pinned, MIT-licensed code | Superseded by 0010 |
| [0010](0010-page-landing.md) | The fold animation is a page landing triggered by the hinge reaching flat or shut, not a timed fade | Accepted |
