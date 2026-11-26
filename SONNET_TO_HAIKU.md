# SONNET → HAIKU handoff — Wiz GPS Camera 3.0.0 (9)

| Field | Content |
|---|---|
| Branch + SHA | `redesign/wiz-gps-camera-v3.0.0`. Release commit `7ab2267` "chore(release): 3.0.0 (9)"; docs commits follow it. Release the **branch tip** (`git log -1`), `7ab2267` must be an ancestor. Not merged into `main`. |
| Application ID | `com.wizlabs.gpscamera` |
| Version | `versionName = "3.0.0"`, `versionCode = 9` (`app/build.gradle.kts`) |
| SDKs | minSdk 29, targetSdk 36, compileSdk 36 |
| Signing | Not signed locally (`app-release-unsigned.apk`). CI `.github/workflows/android.yml` signs with secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (decoded to `KEYSTORE_FILE` in the job). Unchanged. No secrets touched. |
| Build results | `~/.local/opt/gradle-8.11.1/bin/gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease` PASS. Release APK 4,382,059 bytes (4.18 MiB, limit 9 MB). |
| Test results | 56 unit tests, 0 failures (baseline before: 11). Emulator checklist: see STATUS.md "Results" table (PASS / NOT TESTED with reasons). |

## Implementation summary (by handoff section)
- §9–11 tokens/type: `GpsTokens.kt`, `GpsType.kt`, fonts `res/font/gps_sora.ttf`, `gps_mono.ttf`; theme in `Ui.kt`.
- §15 icon/splash: vector adaptive icon + monochrome, AVD splash, wordmark branding (`values-v31`).
- §13–14 Iris: `ui/mascot/{Iris,IrisArt,IrisPose}.kt`, `engine/IrisBrain.kt`, `IrisBrainTest`; `store-assets/mascot/iris.svg` + PNG 512/1024. Used on onboarding (150 dp), gate (120), empty gallery (140), export card (56). Not on camera/viewer/stamp/icon.
- §12 screens: camera (top bar, quick row, shutter bar, teal ring), onboarding (3 pages, real stamp sample), GPS gate (3 steps, accuracy meter), gallery (badges, mono dates, export card), settings (About + privacy link), all with the new tokens.
- N4: `camera/{TopBar,QuickRow,ShutterBar,CameraSheets,Viewfinder}.kt`. N5: `wiz-ads` 0.3 `--no-sdk`, `wiz-update` 1.0, `AdsSetup`, `WizHooks`. N7: decorative loops off when GPS locked (existing); CPU target not met on emulator.
- Also fixed: gallery map zoomed to the world for photos taken at one spot.

## Final architecture
Single-activity Compose app, unchanged core (CameraX, `LocationEngine`, `GpsGate`, `StampRenderer`, `PhotoPipeline`, `Integrity`, `Export`). Frozen: `Integrity.kt`, `PhotoPipeline.kt` (diff empty).

## Modified files (`git diff --stat main...HEAD`: 151 files changed, 3782 insertions, 1215 deletions)
```
   2.6% app/src/main/assets/
   0.6% app/src/main/java/com/wizlabs/ads/compose/
   3.9% app/src/main/java/com/wizlabs/ads/core/
   1.9% app/src/main/java/com/wizlabs/ads/
   3.3% app/src/main/java/com/wizlabs/gpscamera/camera/
   0.6% app/src/main/java/com/wizlabs/gpscamera/engine/
   1.9% app/src/main/java/com/wizlabs/gpscamera/ui/mascot/
  10.5% app/src/main/java/com/wizlabs/gpscamera/
   5.9% app/src/main/java/com/wizlabs/update/
   0.6% app/src/main/res/drawable-nodpi/
   0.6% app/src/main/res/drawable-xxhdpi/
   3.3% app/src/main/res/drawable/
   1.3% app/src/main/res/font/
   1.3% app/src/main/res/mipmap-anydpi-v26/
   1.3% app/src/main/res/mipmap-hdpi/
   1.3% app/src/main/res/mipmap-mdpi/
   1.3% app/src/main/res/mipmap-xhdpi/
   1.3% app/src/main/res/mipmap-xxhdpi/
   1.3% app/src/main/res/mipmap-xxxhdpi/
   0.6% app/src/main/res/values-v31/
   1.9% app/src/main/res/values/
   0.6% app/src/main/
   0.6% app/src/test/java/com/wizlabs/ads/core/
   0.6% app/src/test/java/com/wizlabs/gpscamera/engine/
   1.3% app/src/test/java/com/wizlabs/gpscamera/
   1.3% app/src/test/java/com/wizlabs/update/
   0.6% app/
   0.6% fastlane/metadata/android/en-US/changelogs/
   9.9% fastlane/metadata/android/en-US/images/phoneScreenshots/
   1.3% fastlane/metadata/android/en-US/images/
```
Run `git diff --stat main...HEAD` for the full list.

## New assets (replaced, not added)
- Deleted: `app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher*.png`, `res/drawable-nodpi/ic_launcher_foreground.png`, `app/src/main/assets/wiz-gps-camera-{full-logo,play-store-512,splash}.png`, `store-assets/screenshots/01…07*.png`, `fastlane/.../images/phoneScreenshots/01…07*.png`, old `store-assets/feature-graphic-1024x500.png` and `fastlane/.../featureGraphic.png`, `tools/StoreArt.java`.
- New icon: `res/drawable/ic_launcher_{foreground,background,monochrome}.xml`, `res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`; splash `res/drawable/{avd_splash,ic_splash_icon}.xml`, `res/drawable-xxhdpi/splash_branding.png`.
- Play icon: `store-assets/icon-512.png` (= `fastlane/metadata/android/en-US/images/icon.png`), source `store-assets/icon.svg`, concepts `store-assets/icon-concepts/`.
- Mascot: `store-assets/mascot/iris.svg`, `iris-512.png`, `iris-1024.png`.
- Feature graphic: `store-assets/feature-graphic-1024x500.png` (= `fastlane/.../images/featureGraphic.png`), source `store-assets/_src/feature.html`.
- Screenshots (1080×1920, §22 order): `store-assets/screenshots/1.png`…`8.png` (= `fastlane/.../images/phoneScreenshots/1.png`…`8.png`), raw emulator captures in `store-assets/raw/`, frame `store-assets/_src/frame.html`, captions `store-assets/CAPTIONS.md`.
- No tablet or other-locale screenshots existed; none added. Fixed on-purpose unchanged image: none.
- QA evidence: `store-assets/qa/2026-11-26/`.

## Acceptance checklist (§25)
- [x] F1–F16: tested items PASS, untested listed NOT TESTED in STATUS.md. Stamp geometry code unchanged (only colour literal).
- [ ] Integrity after upgrade 2.2.0 → 3.0.0: NOT TESTED (data wiped); `Integrity.kt`/key alias unchanged; edited-file detection PASS.
- [x] No `#FFC400` in app code; colour names honest (`Gps*`).
- [x] Vector text-free launcher + monochrome; 512 icon matches the SVG.
- [x] Iris never on camera/viewer (code review + screenshots).
- [x] Ads slots per §16A, zero height, `AdsSetupTest` green; update popup blocked on camera (PASS); popup on other screens NOT TESTED.
- [ ] N7 CPU ≤ 8%: NOT MET on emulator (50–80% with emulated camera; Settings 3–14%). APK ≤ 9 MB: PASS. Other §19 rows: NOT TESTED (see STATUS.md).

## Permissions diff (merged release manifest vs 2.2.0)
Unchanged: CAMERA, VIBRATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET, ACCESS_NETWORK_STATE. AD_ID and ADSERVICES permissions explicitly removed. New dependency: Play Core `app-update` + `review` (no extra permission).

## Data collected/shared (for Data safety)
Same as 2.2.0 plus Google Play in-app update checks (no personal data to Wiz Labs). Precise location is stored in photos (EXIF + stamp) on the device and used for the address (platform Geocoder) and map tiles (OpenStreetMap); weather lookup sends coordinates to Open-Meteo only when enabled; network time request to Google. No account, no analytics, no ad SDK. Code refs: `LocationEngine.kt`, `TimeSync.kt`, `PhotoPipeline.kt`, `WizHooks.kt`.

## Privacy policy
https://therealwiz007.github.io/Wiz-GPS-Camera/privacy-policy.html (live copy is the OLD version until the branch is merged and Pages rebuilds). Both copies updated in the repo (root + `app/src/main/assets/`): v1.2, 26 Nov 2026, adds Google Play updates and "no ads in this version". Contact email in the policy is still the placeholder `privacy@wizlabs.app` with a "Release note" paragraph: the user must confirm it.

## ASO content (character counts)
- `fastlane/metadata/android/en-US/title.txt` — 29 (`Wiz GPS Camera: Geotag Photos`)
- `short_description.txt` — 70
- `full_description.txt` — 1273 (limit 4000)
- `changelogs/9.txt` — 383 (limit 500)
- No other locales.

## Known issues / blockers
- N7 not met on emulator; needs a real-phone check. Screenshots use the emulator's low-res virtual scene as the "photo".
- Light theme not shipped (dark-only). Stamp sheet has no live mini previews. Default osmdroid map pins.
- Upgrade-over-2.2.0 ledger check not run.
- The Play listing for this package is not recorded in the repo: read the track holding 2.2.0 (8) in Play Console.

## Release instructions
Track: the one holding 2.2.0 (8) (production if live there). Release name `9 (3.0.0)`. Release notes: `fastlane/metadata/android/en-US/changelogs/9.txt`. Re-check declarations: Location permission (foreground only, core feature), Camera, Data safety (precise location stored in photos on device; weather lookup sends coordinates to Open-Meteo when enabled; address lookup via the platform Geocoder), Ads = No. Replace icon, feature graphic and ALL screenshots (§23A).

## Decisions taken
See STATUS.md "Decisions taken" (16 items). No questions were asked.
