# GLOW UP: notes for Claude

The user's daily workout game for Android (plain Java, no third-party libraries; README.md for the code map).
Two apps from one code (product flavors, app/build.gradle): **GLOW UP** (`glow`, hers, com.glowup.app) and
**TRAINING ARC** (`arc`, her boyfriend's, com.trainingarc.app, since 1.3.0). Flavor.java / FlavorText.java hold what
differs (BuildConfig.MALE): names, ranks (ROOKIE ... GOD TIER), stat labels, men's norms (Science.anchorsM), his week
(push / legs / pull / conditioning / upper), colours, wording, the male demo figure. Body.java is his BODY tab (US Navy
body fat, GOD FORM goal, weight trend, arc length) and FUEL missions.

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

## Exercise demonstrations (1.2.0: the user, 2026-11-21: "visual examples... I have no idea how to do any of these")
- Demo.java: each exercise (and the tests' sit_rise / jog) as keyframed poses of a jointed figure (angles: 0 down,
  90 forward, 180 up; footTo / handTo IK, line() for straight-body planks and push-ups, ground()); DemoView.java draws
  and loops them (muscles in pink, a heart for cardio, cue words); HowTo.java is the how-to page and EXERCISE LIBRARY.
- A new exercise in Plan.ALL needs its Demo.get case. Check poses by eye: AppTest.demoSheets draws every keyframe into
  app/build/screens/demo_*.png.

## TRAINING ARC: the user's brief (2026-11-24)
- For her boyfriend: ~230-240 lb, high body fat but lots of muscle, does push-ups daily, eats chocolate / pizza /
  donuts and feels bad about it; goal "the Body of a God". Motivating like Tekken / Dragon Ball, appealing to the
  masculine **without feeling patronizing**: never shame food (FUEL habits, no calorie counting, treats allowed after a
  meal), use only generic fighting-game / anime language (ROUND, FIGHT!, K.O., POWER LEVEL), never any game's names.
- Jumping moves wait for the rating (Plan.minRating) because of his weight. Realistic like hers: men's norms, honest
  arc length (0.5-1% of body weight a week).

## Data
- State.SCHEMA is the save format. A change to how ratings are computed raises it and converts old saves in
  Logic.migrate (re-rate tests from their raw results, shift ratings and history by the difference). Never drop data.

## Releases (the app updates itself from this repo's GitHub releases: Updater.java)
- Raise versionName in app/build.gradle, add release_notes/<version>.md (GLOW UP) and release_notes/arc/<version>.md
  (TRAINING ARC) (players' words: what changed in the app), push to main. Both apps share the version; each release
  carries GlowUp-<v>.apk and TrainingArc-<v>.apk, and each app's updater takes its own APK and its "## <APP>" section. .github/workflows/release.yml tests, builds, signs and publishes `v<version>` with the APK.
- Signing: keystore/glowup-release.jks (committed, protected by a long random password). The password is the
  GLOWUP_KEY_PASSWORD repository secret, or keystore/keystore.properties locally (never committed). The key must
  never change: phones refuse an update signed by another key (the workflow checks the certificate's SHA-256).
- Tests: `gradle testGlowReleaseUnitTest testArcReleaseUnitTest` (pictures in app/build/screens/glow and /arc) (16-week rating simulation, her baseline, the old-save conversion, every screen
  drawn into app/build/screens).
