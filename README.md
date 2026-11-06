# Wiz GPS Camera

Wiz Labs Android GPS camera starter project.

## Identity
- Application ID: `com.wizlabs.gpscamera`
- Version: 1.2.1 (versionCode 4)
- Minimum Android: 10 / API 29
- Compile / target SDK: 36

## Current implementation (1.2.0)
**Core**
- The app stays closed until precise location is allowed, GPS is switched on and a fresh GPS fix has arrived (GPS gate screen)
- Vibrates when a photo is taken (switch off in Settings)
- Quality: maximize-quality capture mode, camera JPEG quality 100, saved at JPEG 98, photo size up to 16 MP
- CameraX preview and capture (photo size 5 / 8 / 12 MP), correct rotation in portrait and landscape
- Continuous high-accuracy GPS with a freshness dot ("±5 m · 3s ago") on the camera screen
- GPS, altitude, direction, time zone offset and a photo ID written into the JPEG's EXIF
- Offline-safe address: last resolved addresses are cached; if lookup fails the stamp shows coordinates
- In-app gallery: grid, full-screen viewer, multi-select, share (the saved file is sent as-is) and delete

**Stamp**
- Default stamp: frosted card, map with red pin on the left, divider, then place title, street line, Lat/Long and date/time
- Templates: Full with map, Compact, Text only; position Bottom / Top / Left; every field can be toggled
- Custom note, altitude, compass heading, speed, plus code, optional weather (Open-Meteo)
- Date format, 12/24-hour clock, time zone choice, optional GMT offset
- Logo (picked from the gallery) and watermark text
- The live preview and the saved photo use the same renderer

**Camera**
- Screen layout: header with logo, quick row (flash, aspect ratio 4:3 / 16:9 / 1:1, GPS status pin, grid), shaped preview, ring shutter
- Tap to focus, pinch to zoom, grid lines, level indicator, 3 s / 10 s timer, burst (3 or 5) — timer, burst and level are switched in Settings

**Trust and proof**
- Mock-location detection (flag on the photo, or block capture)
- Network time check: time is anchored to a server clock and a warning is printed if the phone clock is off
- Tamper evidence: SHA-256 of every saved photo is kept in a private ledger signed with an Android Keystore key; the gallery marks edited files. Verifiable only inside this app on the same phone (not after reinstall).

## Notes
- minSdk is now 29 (Android 10) so photos can be saved without a storage permission.
- The gallery lists photos this install created. After reinstalling, older photos remain in Pictures/Wiz GPS Camera but may not appear.
- Not yet done: wiring the splash artwork to the SplashScreen API, ads, cloud backup, device testing on a range of phones.

## Build
The workflow uses JDK 17 and Gradle 8.11.1:
- `gradle assembleDebug`
- `gradle assembleRelease bundleRelease` (when signing secrets are set)

## GitHub Actions signing secrets
For signed release output, configure:
- `KEYSTORE_BASE64`: base64-encoded JKS contents
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Never commit the JKS or passwords into the repository.

## Permissions
Camera and location permissions are requested at runtime. Location is optional for taking a photo; without a fix the camera still captures and the stamp shows the location as unavailable.


## Brand assets integrated
- Android launcher icons (legacy density PNGs and adaptive icon XML)
- Approved navy/teal Wiz GPS Camera icon and brand colors
- Splash artwork, full logo and Play Store icon in `app/src/main/assets/`
- HTML privacy policy at `privacy-policy.html`

The splash artwork and Play Store artwork are included as assets; the splash artwork is not yet wired to the Android SplashScreen API. Review the privacy policy contact email and final map/geocoding providers before publishing.

## Production builds
GitHub Actions workflow `.github/workflows/android.yml` builds a signed release APK and AAB using repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
