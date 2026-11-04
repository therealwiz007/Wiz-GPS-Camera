# Wiz GPS Camera

Wiz Labs Android GPS camera project.

## App identity
- Application ID: `com.wizlabs.gpscamera`
- Version: 1.0.1 (versionCode 2)
- minSdk 26 / targetSdk 36 / compileSdk 36

## Changes in this source update
- Android 13+ asynchronous Geocoder callback, and Android 8–12 legacy Geocoder call on a worker thread (guards broad linkage/runtime errors).
- Fused high-accuracy `getCurrentLocation` with last-known-location fallback.
- Fix timestamp and age information tracked from the location's actual fix time; mock-location signal surfaced in UI/stamp.
- EXIF orientation is applied to pixels before stamping; EXIF latitude/longitude and capture timestamp/comment are written to the temporary image before the stamped output is saved.
- Image decode/stamp/encode moved off the main thread.
- CameraX capture resolution selector targets 4032×3024 (12 MP) with lower-resolution fallback.
- R8 and resource shrinking enabled.
- GitHub Actions always produces a debug APK; signed release APK/AAB are built only when all four signing secrets are present.
- Privacy policy contact changed to `blazewizofficial@gmail.com` and third-party map/geocoding disclosure expanded.

## Known unfinished work
This update is a reliability patch and does **not** claim the full wishlist is complete. The in-app photo grid with delete/original-file share, stamp template/field editor and user logo, tap-to-focus/pinch zoom/grid/level/timer/burst, landscape stamp-layout adaptation, robust legacy Android media-store permission behavior, hash-based verification UI, network-time comparison, weather/temperature and Plus Code are not yet implemented. A map tile image is not yet composited into the photo stamp. Camera startup/binding on physical OPPO devices still requires device testing.

## Build
GitHub Actions uses JDK 17 and Gradle 8.11.1. Run the workflow manually from Actions. Debug artifact is available without signing secrets. Signed release outputs require repo secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Do not commit the JKS or passwords.
