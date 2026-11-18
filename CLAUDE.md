# Carousel app (working name Scrll): notes for Claude

- Product plan and roadmap: `PLAN.md`. Features first; the Grease Pencil Studio UI pass is Phase 3.
- Kotlin + Jetpack Compose, Android only, `minSdk 29`. Mirror the toolchain of `~/projects/album` (Attic).
- `~/projects/album`, `~/projects/swipe` (Pixnix/Sweepy), `~/projects/b4after` are the user's other
  Grease Pencil Studio apps. Read them for patterns, **never edit them**.
- Every feature is free. Support = Play tip jar (copy Pixnix's `TipJar` + `play`/`foss` flavors) and an
  AdMob rewarded ad shown **only** when the user taps "support with an ad". No other ads, ever.
- The `foss` flavor has no billing, no ads and no INTERNET permission. Only `play` adds INTERNET (ads).
- Never ship under the name "SCRL"/"Scrll" publicly (it's another company's app).
- Canvas/seam/layout logic stays in plain Kotlin so it's unit-tested on the computer.
