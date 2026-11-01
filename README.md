# Wiz GPS Camera

Wiz Labs Android GPS camera starter project.

## Identity
- Application ID: `com.wizlabs.gpscamera`
- Version: 1.0.0 (versionCode 1)
- Minimum Android: 8.0 / API 26
- Compile / target SDK: 36

## Current implementation
- CameraX live preview and capture
- Bottom-centre translucent frosted-glass geo-tag overlay on the live preview
- OSMDroid mini-map centred on the current/last-known GPS fix
- Captured photo receives a matching glass-style stamp with a rendered map snapshot, address, date/time, coordinates and reported GPS accuracy
- Fused Location Provider last-known location and Android Geocoder address lookup
- OSMDroid local tile cache can display previously cached map tiles offline; uncached areas may appear blank
- GitHub Actions debug APK build; signed release APK/AAB when secrets are configured

## Important MVP scope notes
This is an in-progress MVP, not yet a finished Play Store release. Gallery browsing/sharing, continuous location updates, location freshness indicators, EXIF GPS metadata, map prefetch/download controls, and device testing remain to be completed. Address resolution depends on Android Geocoder availability and may not work offline. The map uses cached tiles when available; no offline map region is bundled.

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
Camera and location permissions are requested at runtime. Location is optional for taking a photo; without a fix the camera should still capture, with unavailable location fields.


## Brand assets integrated
- Android launcher icons (legacy density PNGs and adaptive icon XML)
- Approved navy/teal Wiz GPS Camera icon and brand colors
- Splash artwork, full logo and Play Store icon in `app/src/main/assets/`
- HTML privacy policy at `privacy-policy.html`

The splash artwork and Play Store artwork are included as assets; the splash artwork is not yet wired to the Android SplashScreen API. Review the privacy policy contact email and final map/geocoding providers before publishing.

## Production builds
GitHub Actions workflow `.github/workflows/android.yml` builds a signed release APK and AAB using repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.
