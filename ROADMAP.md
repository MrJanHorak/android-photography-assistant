# Photography Assistant — Roadmap & Task Backlog

> Vision: a "Swiss army knife" app for photographers that tracks and assists with
> everything that matters — exposure, planning, gear, film, business, and on-shoot
> utilities — with an intuitive UI, and an eventual iOS port.

This document is written so any model/contributor can pick up a task without extra
context. Read `HANDOFF.md` first for the current technical snapshot. Each task has an
ID, a self-contained description, acceptance criteria, and key files. Work top-down;
later phases depend on the Phase 0 foundation.

---

## 1. Current State (audit summary)

The app today is effectively **one tool** (a light meter + exposure/gear advisor)
rendered on a single ~1,800-line screen.

- **Stack:** Jetpack Compose, MVVM, Hilt DI, CameraX, KSP. AGP 9.2.1, Kotlin 2.2.10,
  Compose BOM 2026.02.01, minSdk 24, targetSdk 36.
- **Features:** ambient (lux) metering, reflective camera metering, gear catalog
  (bodies/lenses with mount compatibility), workflow coaching (ISO/aperture/shutter
  first), scene presets, custom JSON catalog import + preview, persisted settings.
- **Persistence:** `SharedPreferences` only (`GearSelectionPreferences`).
- **Structure:** single `metering` feature package (data/domain/di/presentation).
  `LightMeterScreen.kt` is 73 KB and holds UI + business logic + formatting helpers.

### Key gaps that block the "Swiss army knife" goal
1. **No navigation / no multi-tool shell.** Everything is one screen.
2. **No general database.** Can't store inventory, shoots, locations, clients, etc.
3. **Business logic is trapped in the UI** (workflow/DoF/exposure math lives in
   composable files), which hurts testability and blocks an iOS port.
4. **Placeholder identity:** package `com.example.photography_helper`, leftover
   `Greeting`/`GreetingPreview` in `MainActivity.kt`, default purple theme.
5. **Thin test coverage** (only catalog merge/preview tested).

---

## 2. Architectural Direction (decide before building many tools)

To scale to many tools AND port to iOS later, adopt this layering now:

- **Tool hub + navigation.** Add Navigation Compose with a home "tool grid" and/or
  bottom navigation. Each tool is its own screen/feature package.
- **Feature-per-package.** `feature/<tool>/{presentation,domain,data}`. Keep the
  existing meter as `feature/lightmeter`.
- **Pure-Kotlin domain layer.** All math/logic (exposure, DoF, sun/moon, timers)
  goes in `domain` with **no Android imports**. This is the code you will share with
  iOS via Kotlin Multiplatform (KMP) later. Unit-test it on the JVM.
- **Room + DataStore.** Room for structured records (gear, shoots, locations,
  clients, film rolls); DataStore (Preferences) to replace `SharedPreferences`.
- **Design system.** A real color scheme, typography, and a small set of reusable
  components (ToolCard, CalculatorField, ResultRow, SectionHeader).
- **iOS note:** Full KMP migration is Phase 8. The cheap win now is *discipline*:
  keep domain logic Android-free so the eventual move to a `shared` KMP module is
  mechanical rather than a rewrite. Compose Multiplatform can later share UI too.

---

## 3. Phased Plan

Legend for each task: **[ID] Title** — description · *Acceptance* · `key files`.
Priority: P0 (foundation), P1 (high value, do early), P2 (valuable), P3 (later).

### Phase 0 — Foundation & cleanup (P0) ✅ COMPLETE

> Done: package renamed to `com.janhorak.shutterdeck` (app "ShutterDeck"); Navigation
> Compose shell with bottom nav (Tools/Planner/Gear/More) + home tool grid; Room +
> DataStore wired via Hilt; OLED design system with cyan accent + night-vision red
> theme (switchable from More); exposure math/formatting extracted to a pure-Kotlin
> `metering/domain` with JVM unit tests; GitHub Actions CI running tests + assembleDebug.

- **[F1] Fix project identity.** Rename package `com.example.photography_helper` to a
  real namespace (e.g. `com.<yourname>.photoassistant`); set `applicationId`; remove
  `Greeting`/`GreetingPreview` from `MainActivity.kt`; rename `Photography_helperTheme`.
  *Acceptance:* builds, no `com.example` references, no placeholder composables.
  `MainActivity.kt`, `app/build.gradle.kts`, `AndroidManifest.xml`, `ui/theme/*`.
- **[F2] Add Navigation Compose + app shell.** Introduce a `NavHost`, a `HomeScreen`
  tool grid, and bottom navigation (Tools / Planner / Gear / More). Move the meter
  behind a "Light Meter" tool entry.
  *Acceptance:* app opens to a hub; can navigate to the light meter and back.
  new `navigation/`, `home/HomeScreen.kt`, edit `MainActivity.kt`.
- **[F3] Add Room + DataStore.** Add Room (with KSP) and DataStore deps to
  `libs.versions.toml` + `app/build.gradle.kts`. Create an `AppDatabase`, a Hilt
  `DatabaseModule`, and migrate `GearSelectionPreferences` to DataStore.
  *Acceptance:* DB builds and is injectable; existing settings still persist.
  `gradle/libs.versions.toml`, `app/build.gradle.kts`, new `core/data/db/*`.
- **[F4] Establish design system.** Replace default purple with a photography-oriented
  palette (neutral dark-first, accent color). Add a **red/night-vision theme toggle**
  (critical for astro). Build reusable `ToolCard`, `ResultRow`, `LabeledField`,
  `SectionHeader` components.
  *Acceptance:* light/dark/night themes switchable; meter screen uses shared components.
  `ui/theme/*`, new `ui/components/*`.
- **[F5] Extract domain logic from the meter UI.** Move exposure/workflow/DoF-style
  math out of `LightMeterScreen.kt` into a pure-Kotlin `feature/lightmeter/domain`
  (Android-free). Split the 73 KB screen into smaller composable files.
  *Acceptance:* `LightMeterScreen.kt` is layout-only; logic has JVM unit tests; build green.
  `metering/presentation/LightMeterScreen.kt` → new `domain/` files + tests.
- **[F6] Testing & CI baseline.** Add unit tests for extracted domain; wire a GitHub
  Actions workflow running `:app:testDebugUnitTest` + `assembleDebug`.
  *Acceptance:* CI runs on push; tests pass. `.github/workflows/*`, `app/src/test/*`.

### Phase 1 — Finish & polish the existing meter (P1)
(Carried over from HANDOFF "Best Next Steps".)

- **[BUG1] Verify ISO factor in `requiredShutterSeconds` (HIGH).** In
  `metering/domain/ExposureMath.kt`, required shutter is `N² · (ISO/100) / 2^EV`, so a
  higher ISO yields a *longer* required shutter. Correct exposure should need a
  *shorter* time at higher ISO (factor `100/ISO`). Confirm intended EV convention,
  then fix and add a regression test. Results match only at ISO 100 today.
- **[BUG2] Verify stabilization direction in `calculateHandheldMinimumShutterSeconds`
  (HIGH).** It *divides* by `2^stabilizationStops`, making the handheld limit *faster*
  with IS/IBIS. Stabilization lets you shoot *slower*, so it should *multiply*.
  Confirm the intended meaning of "minimum shutter seconds", then fix + test.
  *Both bugs were found during Phase 0 and left behavior-preserved; they affect the
  app's core exposure guidance and deserve a deliberate, tested fix.*

- **[M1] Exposure recipe summary card.** Compact card combining shutter target,
  aperture, ISO, and the priority-based adjustment path. *Acceptance:* card shows a
  one-glance recommendation that updates with inputs.
- **[M2] Custom user scene presets.** Persist named motion/stabilization combos (Room
  or DataStore), not just built-ins. *Acceptance:* user can create/save/delete presets.
- **[M3] Workflow-helper unit tests.** Cover the suggestion helpers extracted in F5.
- **[M4] Richer import diagnostics.** Field-level schema report on catalog import
  failure. *Acceptance:* invalid import shows per-field errors.

### Phase 2 — Core calculators (P1, offline, high value, low risk)
All are pure-domain + simple UI. Great first tools to prove the F1–F5 foundation.

- **[C1] Depth-of-field calculator.** Inputs: focal length, aperture, focus distance,
  sensor/crop (reuse gear catalog). Outputs: near/far limits, total DoF, hyperfocal
  distance, circle of confusion by format. *Acceptance:* matches a known reference table.
- **[C2] Hyperfocal / focus-stacking helper.** Hyperfocal distance + suggested focus
  points and overlap for stacking.
- **[C3] ND filter / long-exposure calculator.** Base shutter + ND stops → resulting
  shutter; reverse mode; bulb-timer handoff. *Acceptance:* 10-stop on 1/60 → ~17s.
- **[C4] Field-of-view & equivalent focal length.** Crop factor, angle of view (H/V/D),
  35mm-equivalent. Reuse `cropFactor` already in `CameraBodyProfile`.
- **[C5] Astro shutter calculator (500 / NPF rule).** Max shutter before star trailing,
  from focal length, crop, aperture, pixel pitch (NPF). Pairs with night theme (F4).
- **[C6] Sunny 16 & reciprocity reference.** Quick exposure reference; film reciprocity
  failure correction by stock.
- **[C7] Flash / guide-number calculator.** GN, distance, aperture, ISO interrelation.
- **[C8] Print size & resolution calculator.** Pixels ↔ print size at chosen DPI;
  aspect-ratio and crop helper; viewing-distance "good enough DPI".
- **[C9] Exposure / equivalent-exposure calculator.** Given an exposure, list
  equivalent ISO/aperture/shutter triplets (stops in/out).
- **[C10] Macro / extension calculator.** Magnification, working distance, extension-tube
  exposure compensation, effective aperture.
- **[C11] Diffraction-limit estimator.** Aperture where diffraction softens a given
  sensor; "sharpest aperture" hint.

### Phase 3 — Planning tools (P2; some need location/astronomy, optional APIs)

- **[P1] Golden/blue hour + sunrise/sunset.** From GPS + date. Use an offline solar
  algorithm (no network). *Acceptance:* times match a known almanac within minutes.
- **[P2] Sun & moon position + moon phase.** Azimuth/elevation track, moonrise/set,
  illumination %. Foundation for a future AR/compass overlay.
- **[P3] Saved locations / scouting.** Room-backed: name, GPS, notes, best time/season,
  attached reference photos, map pin. *Acceptance:* CRUD + map view.
- **[P4] Shot list / shoot planner.** Per-shoot checklist of planned shots with
  status, gear, and notes. *Acceptance:* create shoot, add/check shots, persist.
- **[P5] Weather snapshot (optional API).** Cloud cover, sun, precipitation for a
  location/time. Requires an API key + graceful offline degradation.
- **[P6] Milky Way / astro season planner.** Galactic-core visibility windows by date
  and location (builds on P2).
- **[P7] Tide times (optional API).** For seascape/long-exposure planning.

### Phase 4 — Gear management (P2; Room-backed inventory)

- **[G1] Gear inventory.** Bodies, lenses, accessories: model, serial, purchase date/
  price, current value, photos, notes. Seed from existing gear catalog.
- **[G2] Maintenance & firmware log.** Sensor cleanings, repairs, firmware versions,
  shutter count tracking.
- **[G3] Battery & memory-card tracker.** Count, capacity, health, last-charged.
- **[G4] Filter & thread-size tracker.** Which filters fit which lenses (thread mm).
- **[G5] Packing / kit lists with weight.** Build named kits; total weight for travel;
  checklists you tick before leaving. *Acceptance:* create kit, see total weight.
- **[G6] Loaned/rented gear tracker.** Who/when/return-due reminders.
- **[G7] Insurance/value export.** Export inventory (CSV/PDF) for insurance.

### Phase 5 — Film photography suite (P2; strong differentiator)

- **[FL1] Film stock database.** ISO, format, reciprocity curve, dev notes. Bundled
  JSON like the gear catalog, user-extendable.
- **[FL2] Roll & frame logger.** Per roll: stock, ISO, camera, lens; per frame:
  aperture/shutter/focal/notes/GPS/time (digital "EXIF" for film).
  *Acceptance:* start roll, log frames, finish roll, review log; export.
- **[FL3] Development timer + dilution calculator.** Step timers with agitation cues,
  dilution math, temperature compensation (e.g. Ilford/massive-dev style).
- **[FL4] Push/pull helper.** Adjust dev time/notes for pushed/pulled stocks.
- **[FL5] Reciprocity correction.** Auto-correct long film exposures by stock (links C6).

### Phase 6 — Business / professional (P3; CRM-lite)

- **[B1] Client manager.** Contacts, shoot history, notes.
- **[B2] Booking calendar.** Sessions with date/time/location/status + reminders.
- **[B3] Pricing & quote calculator.** Costs, expenses, mileage, margin → quote.
- **[B4] Invoices & receipts.** Generate/export PDF invoices.
- **[B5] Expense & mileage tracker.** Tax-friendly logging with categories.
- **[B6] Model/property releases & contracts.** Templated forms + on-device signature
  capture + PDF export.
- **[B7] Delivery checklist.** Per-shoot deliverables and backup (3-2-1) tracking.

### Phase 7 — On-shoot utilities (P3; sensors/camera)

- **[U1] Spirit level / horizon.** Accelerometer-based level + pitch/roll readout.
- **[U2] Intervalometer / time-lapse planner.** Interval, count, clip-length, card/
  battery estimate. (Triggering the camera needs hardware support; start as planner.)
- **[U3] Gray-card / white-balance screen.** Full-screen 18% gray and white/black
  reference; calibration target.
- **[U4] Live histogram & zebra.** From the CameraX preview already in the app.
- **[U5] Composition overlays.** Rule-of-thirds, golden ratio, diagonals over preview.
- **[U6] Digital slate / clapperboard.** For video/hybrid shooters.
- **[U7] Voice/quick notes per shot.** Fast field notes tied to time/location.
- **[U8] Lighting-setup diagrammer.** Place lights/subject/camera; save/share diagrams.

### Phase 8 — iOS port (P3; after tool set stabilizes)

- **[I1] Extract `shared` KMP module.** Move all Android-free domain code into a Kotlin
  Multiplatform module. *Acceptance:* JVM + iOS targets compile; tests run on both.
- **[I2] Choose UI strategy.** Either Compose Multiplatform (share UI) or native
  SwiftUI consuming the shared module. Document the decision and trade-offs.
- **[I3] Platform abstractions.** `expect/actual` for sensors, camera, storage,
  location so features work on both platforms.

---

## 4. Extra tool ideas (no tool too small)

Quick reference grab-bag to pull future tasks from:

- EV ↔ lux ↔ foot-candle converter; Kelvin/mired white-balance converter.
- Color-temperature & gel calculator (CTO/CTB correction stops).
- Bellows-extension exposure factor; teleconverter aperture/AF impact.
- Crop/aspect-ratio overlay & print-bleed calculator.
- "What lens did I use most?" stats from the frame logger.
- Cheat-sheet library (settings for fireworks, milky way, waterfalls, portraits).
- Posing-guide gallery; lighting-pattern reference (Rembrandt, butterfly, loop).
- Histogram/zone-system reference; Ansel Adams zone mapper.
- Battery-life & card-capacity estimator (RAW size × count).
- Time-zone-aware golden-hour for travel; jet-lag-proof shoot scheduler.
- QR/serial scanner to add gear quickly.
- Rain/condensation warning (dew point vs gear temp) for lens fogging.
- Sensor-dust test pattern viewer; dead/hot-pixel test screen.
- Focus-test/resolution chart generator (print or display).
- Metronome/agitation timer for film dev; safelight timer for darkroom.
- Backup reminder & "did you format the card?" pre-shoot checklist.
- Unit converter (ft/m, °C/°F) used across calculators.
- Watermark/EXIF stamp tool for exported reference shots.
- Shareable shoot report (gear + frames + map) export to PDF.

---

## 5. Suggested execution order (TL;DR)

1. **Phase 0 (F1–F6)** — non-negotiable foundation; everything else rides on it.
2. **Phase 2 calculators (C1, C3, C4, C5, C8)** — fast wins that validate the shell
   and give immediate user value.
3. **Phase 1 meter polish (M1, M3)** — finish what's started.
4. **Phase 3 planner (P1, P3, P4)** and **Phase 4 inventory (G1, G5)** — the
   "tracking everything" core of the vision.
5. **Phase 5 film suite** — strong differentiator if you shoot film.
6. **Phase 6/7** — business + on-shoot utilities as the app matures.
7. **Phase 8** — iOS once the Kotlin domain layer is stable and well-tested.

> Rule of thumb for contributors: **put math in `domain` (no Android imports), keep
> composables thin, persist with Room/DataStore, and write a JVM unit test for every
> calculator.** This keeps the app testable today and portable to iOS tomorrow.
