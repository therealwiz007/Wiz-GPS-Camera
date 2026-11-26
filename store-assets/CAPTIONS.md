# Screenshot captions (en-US) — frames 1080x1920, real emulator captures in raw/

| # | Headline (≤32 chars) | Screen (real app capture) | Mascot |
|---|---|---|---|
| 1 | Location & time on every photo | Camera with Glass stamp | none |
| 2 | Exact GPS, fresh fix | GPS gate, fix locked | Iris happy |
| 3 | Proof photos can't fake | Gallery with verified badges | none |
| 4 | Stamp styles for every job | Stamp style sheet | none |
| 5 | Projects for field work | Gallery filtered by project | none (see note) |
| 6 | All your photos on a map | Gallery map view | none |
| 7 | PDF reports in one tap | Gallery export done card | Iris proud |
| 8 | Pro camera controls | Camera with EV slider and 2x zoom | none |

Capture setup: debug build on the `wiz35` emulator (API 35), demo-mode status bar (10:00, full battery),
mock GPS fix 12.97565, 77.60712 via `adb emu geo fix`, projects "Depot 7" and "Site audit" seeded via
SharedPreferences. The emulator's virtual camera scene is a low-resolution wall; the stamp, UI, verification
badges, map and PDF export are real app output. Frame HTML: `_src/frame.html?n=<1-8>`; feature graphic: `_src/feature.html`.
