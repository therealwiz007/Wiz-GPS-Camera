# HAIKU release report: Wiz GPS Camera 3.0.0 (9)

Date: 2026-11-26. Branch: `redesign/wiz-gps-camera-v3.0.0`. Release commit `7ab2267` is an ancestor of the branch tip.

## Status summary
- Code merged into `main` (merge commit `94680f5`, pushed). CI production build succeeded.
- Play: 9 (3.0.0) uploaded to **Closed testing – Alpha** (the track holding 2.2.0 (8)).
- **SUBMITTED for review** (2026-11-26, user approved): release 9 (3.0.0) and Data safety questionnaire. Publishing overview shows "Changes in review". Google review typically within 7 days. Not verified as approved.
- Production: not touched.

## Final version
- applicationId `com.wizlabs.gpscamera`, versionCode **9**, versionName **3.0.0**, minSdk 29, targetSdk 36, compileSdk 36 (`app/build.gradle.kts`).

## Build and tests (re-run locally, offline)
- `gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease`: BUILD SUCCESSFUL.
- Unit tests: 56 run, 0 failures, 0 errors, 0 skipped (from JUnit XML reports).
- Release APK (unsigned, local): 4,382,059 bytes.

## CI build and signing
- Workflow `Wiz GPS Camera - Production APK and AAB`, run `45770335400` on `main`: all steps success.
- Artifacts downloaded to `dist/ci/` (gitignored):
  - `Wiz GPS Camera (3.0.0).aab`: 5,326,659 bytes (under 10 MB, no split needed).
  - `Wiz GPS Camera (3.0.0).apk`: 4,390,251 bytes.
- Signing: `jarsigner -verify -certs` on the AAB reports `CN=Wiz Labs, OU=Wiz Labs, O=Wiz Labs, L=Ambala, ST=Haryana, C=IN`, jar verified. APK `apksigner` reports the same signer.
- APK metadata: versionCode 9, versionName 3.0.0, minSdk 29, targetSdk 36.
- AAB versionCode: not parsed directly (aapt2 could not read the AAB). Play Console reads `9 (3.0.0)` and target SDK 36 from the uploaded bundle.
- Secrets: GitHub secret names only (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) confirmed present. No values were read or printed.

## Acceptance spot-check (code read, not device tested)
Confirmed in source: `AdsSetup.ENABLED = false`; `engine/IrisBrain.kt`; `ui/mascot/Iris*.kt`; `GpsTokens.kt` (Midnight tokens); `camera/{TopBar,QuickRow,ShutterBar,CameraSheets,Viewfinder}.kt`; `WizHooks.mayPromptNow() = !GpsUi.busy`; `update/WizUpdate.java`.

## Store assets
- Icon `icon-512.png` (512x512, text-free): replaced. Feature graphic 1024x500: replaced. Both match repo files byte-for-byte.
- Phone screenshots: 7 old removed, 8 new uploaded in order 1.png–8.png (8/8 shown in Play). Repo copies match.
- Old assets removed from the repo (not tracked in git any more). `tools/StoreArt.java` deleted.
- Tablet and other-locale screenshots: none in repo, none added.

## Listing text (en-US)
- Title: `Wiz GPS Camera: Geotag Photos`, 29 chars (limit 30).
- Short: 70 chars (limit 80). Full: about 1,272 chars (limit 4,000). Changelog: under 500.
- Promo-word fix committed (`63b3722`): "best-quality" → "high-quality"; "glass top bar" → "glass header bar" in changelog. Both are in `main` now.
- Listing text and graphics saved as draft in Play (default listing).

## Manifest and ads checks
- Permissions (merged manifest): CAMERA, VIBRATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET, ACCESS_NETWORK_STATE. AD_ID and ADSERVICES permissions removed with `tools:node="remove"`.
- No ad SDK: 0 matches for `com.google.android.libraries.ads|ump` in `app/src`; no ads or UMP dependency in `app/build.gradle.kts`. Ads slots zero height.
- Play "Contains ads": **No** (keep until AdMob is enabled in a later release).

## Privacy policy
- Live URL returned HTTP 200 before the merge. A Pages rebuild was started by the merge; the new text is not verified live yet.
- Contact email set to `blazewizofficial@gmail.com` (user-provided); "Release note" paragraph removed. Live Pages copy verified (2026-11-26). The in-app copy in `app/src/main/assets` is updated in the repo but ships only in the next AAB.

## Data safety (saved and submitted)
- Was "No" (carried from 2.2.0). Changed to **Yes**, because weather lookup sends coordinates to Open-Meteo and the address lookup uses the platform Geocoder.
- Saved answers: Location, Approximate and Precise: collected, not shared, not ephemeral, users can choose (optional), purpose App functionality. Encrypted in transit: Yes. Account creation: app does not allow account creation; login with outside accounts: No.
- Not answered: data-deletion question (optional). Play services Device or other IDs: not declared (not verified from code).

## App content declarations (Play)
- 10 declarations actioned (Nov 1, 2026): Health apps, Advertising ID, Financial features, Government apps, Data safety, Target audience, Content ratings, Ads, Sign-in details, plus one more. Not re-checked one by one; Data safety is the open item above. "Need attention" tab empty.

## Testing track
- Closed testing – Alpha: 2.2.0 (8) full rollout (178 countries, 96% install base). 9 (3.0.0) uploaded and saved as draft release, name `9 (3.0.0)`, en-US notes from `changelogs/9.txt`.
- Publishing overview shows 1 change ("Start full rollout") plus listing changes, not submitted.

## Production approval
- NOT requested. Dashboard shows production access not yet eligible: needs 12 testers opted in for 14 days (12 tester opt-ins currently, active for 7 days). Production needs an explicit user yes later.

## Outstanding issues
1. Data safety "No" conflicts with code. Decide and update before submit.
2. Privacy contact email updated (see above); in-app asset copy ships next build.
3. N7 CPU target (≤8%) not met on the emulator; needs a real phone check.
4. Not tested: upgrade over 2.2.0 (ledger), CSV/KML re-test, accessibility, font scale, RTL, and the update popup on screens other than camera (per STATUS.md).
5. Dark-only UI; Settings not regrouped; screenshots use the emulator's low-res virtual camera scene.
6. Tracked `.DS_Store` shows as modified in the working tree; left unstaged, not committed.

## Next steps for the user
1. Decide the Data safety answers (see proposal above).
2. Confirm the privacy-policy contact email.
3. Then submit: Publishing overview → Submit 1 change for review.
