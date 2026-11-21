# GLOW UP: notes for Claude

The user's daily workout game for Android (plain Java, no third-party libraries; README.md for the code map).

## The user's rules
- **Never mention mortality, death or a "longer life" anywhere in the app** (the user, 2026-11-21: "the constant
  mortality reminders are NOT helpful"). Health is framed as strength, energy, mood, sleep, steadiness.
- **Ratings must be realistic** (2026-11-21: "so I actually know how my body and health is changing"): every test is
  anchored on population norms for women (Science.anchors: percentiles 1/10/30/50/75/90/99 = 100/400/800/1200/1600/
  2000/2400, national elite 2600, Olympic 2800, world best 3200). OLYMPIAN must mean Olympic-level results, never a
  title; it's fine that she may never reach it. A test that can't tell people apart past a point has a ceiling
  (Science.ceiling). 40 ranks: 8 tiers x 5 divisions.
- Her baseline (2026-11-21): sit & rise 5, one-leg 53 s R / 43 s L, chair 13, push-ups 2.3 (full equivalents),
  bridges 14, plank 2 s, march 108 -> 603, OFF THE COUCH III (~20th percentile). AppTest.herBaseline keeps it.

## Data
- State.SCHEMA is the save format. A change to how ratings are computed raises it and converts old saves in
  Logic.migrate (re-rate tests from their raw results, shift ratings and history by the difference). Never drop data.

## Releases (the app updates itself from this repo's GitHub releases: Updater.java)
- Raise versionName in app/build.gradle, add release_notes/<version>.md (players' words: what changed in the app),
  push to main. .github/workflows/release.yml tests, builds, signs and publishes `v<version>` with the APK.
- Signing: keystore/glowup-release.jks (committed, protected by a long random password). The password is the
  GLOWUP_KEY_PASSWORD repository secret, or keystore/keystore.properties locally (never committed). The key must
  never change: phones refuse an update signed by another key (the workflow checks the certificate's SHA-256).
- Tests: `gradle testReleaseUnitTest` (16-week rating simulation, her baseline, the old-save conversion, every screen
  drawn into app/build/screens).
