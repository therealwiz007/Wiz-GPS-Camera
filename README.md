# Wiz GPS Camera

Wiz Labs Android GPS camera starter project.

## Identity
- Application ID: `com.wizlabs.gpscamera`
- Version: 1.0.0 (versionCode 1)
- Minimum Android: 8.0 / API 26
- Compile / target SDK: 36

## Current implementation
- CameraX live preview and capture
- Fused Location Provider last-known location
- Address lookup through Android Geocoder
- Burned-in address, date/time and coordinates stamp
- OSMdroid map dependency and local tile-cache configuration foundation
- GitHub Actions debug APK build; signed release APK/AAB when secrets are configured

## Important MVP scope notes
This is the first implementation scaffold, not a finished Play Store release. The current camera screen shows a GPS information card, and the stamped photo burns address, date/time and coordinates. The next implementation pass must add the actual mini-map snapshot into the burned-in stamp, gallery browsing/sharing, explicit location freshness/accuracy handling, and metadata writing (EXIF). OSMDroid's tile cache is configured, but a visible map panel and offline region prefetch UI are not yet wired into the camera screen.

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
