# STATUS — Wiz GPS Camera 3.0.0 (9) — branch `redesign/wiz-gps-camera-v3.0.0`

## Drift from the handoff (found 2026-11-26)
- HEAD `0334f28` on `main`, only `.DS_Store` and `.gitignore` modified (left untouched, not committed). As planned.
- All sources are in one flat package `com.wizlabs.gpscamera`; there was no `ui/` or `engine/` folder. New code uses `ui/mascot`, `engine` (own packages). The `camera/` files are in the same package as before (directory only).
- CI signs with `KEYSTORE_BASE64` (decoded to a file, then `KEYSTORE_FILE`), not `KEYSTORE_FILE` as a secret. Workflow unchanged; it already makes separate APK/AAB artifacts named `Wiz GPS Camera (<versionName>)`.
- `privacy-policy.html` was only in the repo root; the second copy in `app/src/main/assets/` was added.
- `wiz-ads/sync.sh --no-sdk` already existed (0.3). No SDK was in the app.
- White on `#00897A` (light teal) is 4.32:1, not 4.5:1 as planned. Light theme is not shipped; the token is documented in `GpsTokens.kt` and tested.
- Play status of 2.2.0 (8): not recorded in the repo. Haiku reads it from Play Console.

## Decisions taken (no questions asked)
1. **Dark-only UI** for 3.0.0 (camera and gallery are dark by design; Settings follows). Light palette kept as tokens/contrast test only. NOT DONE: light theme (§19 "Themes").
2. **Icon**: 3 SVG concepts in `store-assets/icon-concepts/`. Chose A (pin with lens head): reads at 48 px, silhouette matches Iris, differs from competitors' generic camera+pin glyphs. B looked like a car logo, C is the generic viewfinder+pin glyph.
3. Launcher label `GPS Camera` (`app_name`); `app_full_name` = "Wiz GPS Camera" for About/update popup.
4. Fonts: Sora + Roboto Mono variable TTF from github.com/google/fonts (OFL), subset to Latin with fonttools (51 KB + 28 KB). Arabic/Hindi fall back to system fonts.
5. Default stamp accent is now Electric Teal. One-time migration moves the old default (yellow) to teal; later choices are kept. Old "Wiz Yellow" swatch replaced by "Gold".
6. GPS gate stays strict: the existing code has no "use anyway" path, so none was added (N6).
7. Camera layout: the pills above the shutter (project, note, stamp) moved to a top glass bar; flash/timer/ratio/GPS chip/grid/exposure became the quick row on the preview; level moved to the top bar. Preview frame geometry unchanged (it sits below the bar, not full-bleed), so capture/stamp geometry is untouched.
8. GPS chip shows `±5 m · 3 s` (mono). The address was dropped from the chip (it is on the stamp).
9. Splash: AVD icon + wordmark branding image (`windowSplashScreenBrandingImage`, Android 12+ only; older versions show the animated icon).
10. Settings keeps its existing section order; About (wordmark, version, privacy link) added at the end. Not regrouped into the 8 groups of §12.8.
11. Export now shows an Iris "proud" card (Share / Done) instead of opening the share sheet at once.
12. Update popup: `mayPromptNow()` is false on the Camera screen, onboarding and the gate (`GpsUi.busy`).
13. Ads: `AdsSetup` slots `gallery` (native), `settings` (banner), `adfree` (rewarded row); screens/noAdScreens per §16A. Gallery slot item is not composed at all while ads are off (no grid gap). `Review` library of wiz-update is synced and its two Play deps added (`app-update 2.1.0`, `review 2.0.2`, both from the offline cache).
14. Gallery map: photos at one spot made `zoomToBoundingBox` zoom to the whole world; now zoom 17 at the centre. (Bug fix found while taking the screenshots.)
15. `tools/StoreArt.java` (old yellow store-art tool) deleted; store art is now HTML (`store-assets/_src/`) rendered with headless Chrome.
16. No hi-IN listing: the repo has no hi-IN folder, so none was created.

## Results
- Baseline (main, before changes): `:app:testDebugUnitTest :app:assembleDebug` PASS, 11 tests, 0 failures.
- Final: `gradle --offline :app:testDebugUnitTest :app:assembleRelease` PASS, **56 tests, 0 failures** (incl. IrisBrainTest 10, ContrastTest 2, AdsSetupTest 6, synced wiz-ads/wiz-update tests).
- Release APK (unsigned): **4,382,059 bytes (4.18 MiB)** vs limit 9.0 MB.
- No ad SDK: `grep -rE "com\.google\.android\.(libraries\.ads|ump)" app/src` = 0 lines; release runtime classpath has no ads-mobile-sdk / user-messaging-platform. Merged manifest permissions: CAMERA, VIBRATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET, ACCESS_NETWORK_STATE (same as 2.2.0) + the AndroidX dynamic-receiver permission. AD_ID / ADSERVICES removed.
- `grep FFC400` in app/src = none. `git diff 0334f28 HEAD` on `Integrity.kt` and `PhotoPipeline.kt` is empty. `StampRenderer.kt` / `Export.kt`: only the colour literal `0B0D10` → `060B18`.

### Emulator (wiz35, API 35, one emulator) — evidence in `store-assets/qa/2026-11-26/` and `store-assets/raw/`
| Check | Result |
|---|---|
| Splash, onboarding (3 pages, Iris), GPS gate (searching → locked), camera, stamp sheet, gallery, map, export card, settings | PASS (captured) |
| F1 gate with mock fix; F2 capture 4:3; F4 live stamp = saved stamp; F6 EXIF GPS present in saved JPEG (`Exif`/`GPS` bytes; exiftool not installed) | PASS |
| F10 integrity: new photos show teal verified badge; appending a byte to a JPEG → amber warning "Edited" | PASS |
| F11 projects: create/select, folder per project, name on stamp | PASS |
| F12/F13 gallery filter by project, map view | PASS |
| F14 PDF export (saved to Downloads, card shown) | PASS; CSV/KML export NOT TESTED this run (code unchanged) |
| Update popup never over the camera (fake update intent on the camera screen) | PASS; popup showing on Gallery/Settings NOT TESTED (needs a Play update or fake intent at cold start) |
| Upgrade 2.2.0 → 3.0.0 keeps ledger verification | NOT TESTED: the emulator data was wiped; ledger/key code is byte-identical to 2.2.0 |
| Mock location flag/block, 5/8/12/16 MP, burst, timer, flash, volume key, front camera | NOT TESTED: unchanged code, emulator camera only |
| Real lens quality, compass, real GPS, OEM devices | NOT TESTED: no phone attached |
| **N7 CPU ≤ 8% with preview running** | **NOT MET on the emulator**: 50–80% of one core with the emulated camera preview running (software camera pipeline, system load ~25); Settings screen 3–14%; decorative loops are already off once GPS is locked. Not compared with 2.2.0 on the same emulator. Needs a real-phone measurement. |
| Jank on camera screen | 83% janky frames on the emulator (software GPU, camera preview); not meaningful, NOT TESTED on a phone |
| Cold start, PSS | PSS 149 MB total (camera, debug build). Cold start TotalTime NOT MEASURED |
| §19: 200% font scale, reduce-motion, RTL `ar`, small/tablet profiles, TalkBack, API 29 emulator | NOT TESTED |
| Contrast | PASS by unit test: silver/bg 17.8, muted/bg ≥4.5, onTeal/teal ≥4.5, teal/bg ≥3; light pair: white on #00897A = 4.32 (below 4.5, documented) |

## NOT DONE
- Light theme (dark-only), Settings regroup into 8 groups, Stamp sheet "live mini previews" on style chips, teal map pins (default osmdroid marker kept), tablet screenshots (none existed), hi-IN listing, `material-icons-extended` removal (still used for ~60 icons; R8 strips unused ones, APK 4.2 MB), baseline-prof.txt update (wildcard rules already cover the new classes), CSV/KML re-test, the §19 device/accessibility matrix above, N7 target on a phone.
- Screenshots: the emulator's virtual camera scene is a low-res wall poster, so the photo content is blocky. The stamp, UI, badges, map and PDF export are real app output. Real-phone shots would look better.
