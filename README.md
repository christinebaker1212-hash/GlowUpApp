# GLOW UP and TRAINING ARC

Two apps from one code: GLOW UP (`glow`) and TRAINING ARC (`arc`, a fighting-game style training arc toward a lean,
strong physique). `gradle assembleRelease` builds both.

A daily workout game for Android: an Elo-style FITNESS RATING from COUCH POTATO to OLYMPIAN, XP and levels,
stat bars (STAMINA, STRENGTH, CURVES, CORE, BALANCE), a plan built around glutes, hips, waist and posture,
the phone's step counter, and a healthspan scorecard. Plain Java, no third-party libraries, data in versioned JSON.

## Build
    echo "sdk.dir=/path/to/android-sdk" > local.properties
    gradle assembleRelease          # app/build/outputs/apk/release/app-release.apk
    gradle testReleaseUnitTest      # 16-week rating simulation + every screen drawn into app/build/screens

## Releases and updates
Raise versionName in app/build.gradle, add release_notes/<version>.md and push to main: the Release workflow tests,
builds, signs and publishes the APK as a GitHub release. The app checks releases/latest and installs updates itself.

## Signing key
keystore/glowup-release.jks signs the app; its password is the GLOWUP_KEY_PASSWORD repository secret (or
keystore/keystore.properties locally, never committed). Android only installs an update signed with the same key.
The key is valid for 100 years.

## Where things are
- Science.java: test curves (result -> rating), age grading, Elo, ranks, levels, step goals
- Plan.java: the exercise library, the weekly plan, targets from ratings, automatic harder versions
- Logic.java: daily upkeep (steps, streaks, detraining), workout and test results, trophies
- Demo.java / DemoView.java / HowTo.java: animated exercise demonstrations and how-to pages
- Flavor.java / FlavorText.java: everything that differs between the two apps; Body.java: TRAINING ARC's BODY tab and FUEL
- Updater.java: in-app updates from GitHub releases
- Steps.java: the hardware step counter, read every 15 minutes; Reminder.java: the daily notification
- MainActivity / TestFlow / WorkoutFlow / Screens: the screens; State / Store: data, backup and restore
