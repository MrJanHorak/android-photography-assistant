# ShutterDeck — Handoff Snapshot

Date: 2026-05-30
Repo: `MrJanHorak/android-photography-assistant` · Working dir: `D:\androidprojects`
Package root: `com.janhorak.shutterdeck`

> **Read this first, then `ROADMAP.md` for the prioritized backlog.** This file is the
> authoritative *current technical snapshot*. The vision is a "Swiss army knife" app for
> photographers (exposure, planning, gear, film, business, on-shoot utilities) with an
> intuitive UI and an eventual iOS port via Kotlin Multiplatform (KMP).

---

## 1. Project Snapshot (what works today)

ShutterDeck is a multi-tool Jetpack Compose app. Bottom navigation now has 5 tabs:
**Tools** (sectioned home grid), **Planner** (hub), **Gear** (inventory + filters + power/media + loans + insurance/export + kits + maintenance), **Film** (hub + stock library + roll logger + development timer + push/pull helper + reciprocity assistant), **More** (settings/theme).

**14 calculator/reference tools** remain reachable from the Tools grid, grouped into **Exposure** / **Lens & Focus** / **Planning & Output**:
1. Light Meter — ambient (lux sensor) + reflective (CameraX) metering, gear-aware coaching.
2. Depth of Field · 3. ND Filter · 4. Field of View · 5. Astro Shutter (500/NPF) ·
6. Print Size · 7. Focus Stacking · 8. Sunny 16 / reciprocity · 9. Guide Number (flash) ·
10. Equivalent Exposure · 11. Macro / Extension · 12. Diffraction Limit ·
13. Golden Hour (sun times) · 14. Sun & Moon position + moon phase.

**Planner tab** is a hub linking: Golden Hour, Sun & Moon, **Scouting Locations** (Room CRUD),
and **Shoots** (Room-backed shoot list → per-shoot shot checklist).

**Film tab** is now a hub for the film suite. **FL1 Film Stocks** is live: a Room-backed
`FilmStockEntity` library seeded from `app/src/main/assets/film_stock_catalog.json`, with built-in
read-only starter stocks and editable custom stocks in the same table. Each stock stores format,
type, ISO, processing type, optional reciprocity exponent/start threshold, optional push/pull
latitude, description and development notes. The screen supports search, source/format/type filters,
and custom add/edit/delete flows. `Sunny16.kt` now accepts a stock-specific reciprocity onset
threshold instead of hard-coding 1 second.

**FL2 Roll & frame logger** is also live. `FilmRollEntity` snapshots stock metadata (display name,
format, type, processing, base ISO, reciprocity fields) onto each roll so history/export survives
later stock edits or deletes, while `FilmFrameEntity` stores frame number + exposure sequence,
aperture, shutter speed, focal length, capture time, optional GPS and notes. The Film hub links to
a roll list and per-roll detail screen where users can start/edit/delete rolls, finish/reopen them,
log/edit/delete frames, and export a single-roll CSV through Android's document picker.

**FL3 Development timer + dilution calculator** is now live as well. The Film hub links to a
dedicated darkroom screen with an optional saved-roll context selector, a 1+N dilution calculator,
20C-based developer time compensation across 16–26C, and a guided fixed-step recipe timer
(pre-soak, developer, stop bath, fixer, wash). The timer shows recurring agitation cues, supports
pause/resume/reset, keeps the live countdown pinned above the scrollable form, and restores an
active session from `SavedStateHandle` if the process is recreated while a recipe is running.

**FL4 Push / pull helper** is now live. It can work from either an active saved roll or a library
stock, compares box ISO against the chosen EI using raw stop deltas, checks the result against each
stock's saved `maxPushStops` / `maxPullStops` when that data exists, surfaces developer notes, and
generates a clipboard-ready note block for roll notes or lab logs. Roll-based guidance still works
after stock deletion, but saved latitude falls back to unavailable unless the live stock entry still
exists.

**FL5 Reciprocity assistant** is also live. It uses either a saved roll or a library stock to apply
stock-specific reciprocity exponent/onset data, show the corrected long-exposure time plus added
compensation stops, surface stock notes when available, and generate a clipboard-ready note block.
Because rolls snapshot reciprocity exponent/onset values, the reciprocity helper still works from a
roll even if the linked stock is later removed from the library.

**Phase 1 meter polish** is now complete. `LightMeterScreen.kt` delegates its remaining business
logic to pure `metering/domain/` helpers: `ShootingAid.kt` owns subject-motion/stabilization/preset
logic, `WorkflowCoaching.kt` owns workflow-priority coaching plus nearest-option helpers, and
`MeterReadout.kt` owns suggested-shutter and camera-AE summary formatting. Shared metering models
(`ExposureOption`, `ShutterOption`, `ReflectiveMeterReading`) now live in `metering/domain/` too,
so the screen is effectively presentation/layout-focused again while the behavior stays covered by
JVM tests.

**Planner polish** has also started. Shoots can now optionally link to saved scouting locations:
`ShootEntity` gained `locationId`, `ShootsScreen.kt` now supports editing shoots and choosing or
clearing a saved location, the shoot list shows linked-location labels, and
`ShootDetailScreen.kt` shows the linked location context (name, coordinates, best time, notes) or
a graceful missing-location state if the saved location was removed later.

Saved-location and astronomy coordinate entry now also support **Use current location** alongside
manual entry. `LocationsScreen.kt`, `SunTimesScreen.kt`, and `SunMoonPositionScreen.kt` all share
the same permission/settings/status flow via `ui/location/CurrentLocationUi.kt`, while
`core/location/DeviceLocationProvider.kt` owns the one-shot fused-location lookup. The astronomy
tools update UTC offset from the device timezone when current-location autofill is used.

The shoot planner now also supports richer **per-shot gear + notes**. `ShotItemEntity` gained
free-form `gearNotes` and `notes` fields, `ShootDetailViewModel.kt` now preserves existing `done`
and `sortOrder` data when editing a shot, and `ShootDetailScreen.kt` uses a shared add/edit dialog
instead of the old one-line shot adder so each shot can carry optional gear and planning notes
without losing the checklist workflow.

Saved scouting locations now also support one lightweight **reference photo** attachment.
`LocationEntity` gained `referencePhotoUri`, `LocationsScreen.kt` now supports choose/replace/clear
photo actions inside the location editor, and `LocationsViewModel.kt` preserves `createdAt` on edit
while only closing the editor after a successful save. Shared URI-grant handling now lives in
`core/storage/ReferencePhotoGrantManager.kt` so Gear and Planner can safely reuse the same SAF
document without accidentally releasing each other's persisted read access.

That planner follow-through is now rounded out with an in-app **map preview** and lightweight
**shot reordering**. Saved locations with coordinates can open a full-width OpenStreetMap preview
dialog plus an external map-app fallback, with osmdroid initialized from `ShutterDeckApp.kt`
without requiring an API key. `ShootDetailScreen.kt` also now supports move-up / move-down shot
reordering within the open and completed groups, with pure reorder logic in
`planner/domain/ShotOrdering.kt`.

**Gear tab** now has nine useful capabilities:
1. **Inventory foundation + richer metadata** — add/edit/delete bodies, lenses and accessories
   with brand/model, serial, purchase date, purchase/current value, weight, notes, optional saved
   lens thread size, condition, storage location, purchase source, and a lightweight reference-photo
   attachment stored as a persisted document URI.
2. **Catalog seeding** — one-tap import of bodies/lenses from the current metering catalog into
   the inventory, with duplicate avoidance via nullable `GearItemEntity.catalogId` and a
   fallback skip for manual brand/model matches already saved.
3. **Filter/thread tracker** — Room-backed filter inventory plus a Gear-tab compatibility view
   that matches saved filters against saved lenses by normalized thread size.
4. **Battery tracker** — Room-backed batteries/power packs with optional gear linkage, charge
   state, capacity, health, current charge percentage, last charged date, last checked date and notes.
5. **Memory-card tracker** — Room-backed card inventory with optional gear linkage, card type,
   capacity, speed label, workflow status, last formatted date and notes.
6. **Loan / rental tracker** — Room-backed loan records with optional linkage to saved inventory
   items (or a manual external-gear label), direction/status fields, counterpart/date/notes,
   Gear-tab CRUD, summary counts, and in-app due-soon / overdue reminders when the due date is
   saved as ISO `YYYY-MM-DD`.
7. **Insurance & export** — an **Insurance & export** section summarizes owned inventory
   coverage (item count, serial coverage, saved current values, saved reference photos, total
   purchase/current value) and can save **CSV** or **PDF** reports via Android's document picker.
   The export currently covers owned inventory items (`GearItemEntity`), which are the records
   that store insurer-relevant serial/value metadata.
8. **Packing kits** — build named kits from saved gear, see total weight, and tick items off
   as packed before leaving.
9. **Maintenance log** — record cleanings, firmware updates, repairs and shutter-count checkpoints
   against saved gear items.
Reference photos currently surface as attachment labels with replace/clear actions rather than
full in-app previews.

**Persistence:** Room DB `shutterdeck.db` (v15) + DataStore (Preferences) for theme/settings.
`AppDatabase.MIGRATION_14_15` now preserves the new location `referencePhotoUri` column on upgrade,
but this build still keeps `fallbackToDestructiveMigration(dropAllTables = true)` for older schema
jumps, so much older local installs can still lose Room data on upgrade.

---

## 2. Architecture & layering (FOLLOW THIS)

MVVM + Hilt DI + Navigation Compose + Room + DataStore + CameraX.

- **Pure-Kotlin domain layer** — all math/logic lives in `*/domain/` with **NO `android.*`
  imports** (so it can move to a KMP `shared` module later). JVM-only stdlib like
  `kotlin.math`, `Math.toRadians`, `String.format`, `java.util.Locale` is acceptable.
- **Thin Compose screens** in `*/presentation/`. Parse input strings → call domain →
  render results. Reuse shared components; don't put math in composables.
- **Room** for structured records; **DataStore** for settings. DI in Hilt modules.
- **One JVM unit test per domain file**, validated against published reference values.

### Package map (`app/src/main/java/com/janhorak/shutterdeck/`)
- `MainActivity.kt`, `ShutterDeckApp.kt` (`@HiltAndroidApp`), `AppViewModel.kt` (theme).
- `navigation/` — `Routes`, `TopLevelDestination`, `titleForRoute()` (Destinations.kt);
  `ShutterDeckRoot.kt` (Scaffold + bottom nav + `NavHost`).
- `home/HomeScreen.kt` — the sectioned Tools grid (`toolSections`).
- `film/data/` — `FilmStockCatalogLoader` (bundled JSON parser/fallback loader),
  `FilmStockRepository` (seed bundled starter stocks into Room, enforce read-only built-ins).
- `film/domain/` — `FilmRollExport` (pure CSV export + filename helpers), `FilmRollLog`
  (roll statuses, default timestamps), `FilmDevelopment` (dilution math, temperature compensation,
  agitation cues, fixed recipe-step builder), `FilmPushPull` (EI delta + latitude guidance + note
  builder), `FilmReciprocity` (stock-aware reciprocity correction + note builder).
- `metering/domain/` — exposure math/formatting helpers plus shared metering models
  (`ExposureOption`, `ShutterOption`, `ReflectiveMeterReading`), `ShootingAid`
  (subject-motion + stabilization models, scene presets, available-mode resolution, preset
  matching, qualitative shooting-aid assessment), `WorkflowCoaching`
  (workflow-priority guidance + nearest-option helpers), and `MeterReadout`
  (suggested-shutter + camera-AE summary formatting).
- `film/presentation/` — `FilmScreen` (hub), `FilmStocksScreen`, `FilmStocksViewModel`,
  `FilmRollsScreen`, `FilmRollsViewModel`, `FilmRollDetailScreen`, `FilmRollDetailViewModel`,
  `FilmDevelopmentScreen`, `FilmDevelopmentViewModel`, `FilmReferenceViewModel`,
  `FilmPushPullScreen`, `FilmReciprocityScreen`.
- `gear/presentation/` — `GearInventoryScreen`, `GearInventoryViewModel`, `GearItemEditState`
  (inventory + filter + battery/card + loans + kits + maintenance summaries),
  `GearFilterTrackerSection.kt`, `GearSupportTrackerSection.kt`, `GearLoanTrackerSection.kt`,
  `GearInsuranceExportSection.kt`, `GearPresentationFormatting.kt`.
- `gear/domain/` — `GearLoanReminders.kt` (pure reminder helper for due-soon/overdue status),
  `GearInsuranceExport.kt` (owned-inventory insurance summary + CSV/PDF export content).
- `calculators/domain/` — 13 pure calculators incl. `SolarTimes.kt`, `CelestialPosition.kt`.
- `calculators/presentation/` — one screen per calculator + shared infra:
  `CalculatorScaffold.kt` (CalculatorScaffold/ResultCard/CalculatorHint),
  `CalculatorFormatting.kt` (formatMeters/Degrees/OneDecimal/Clock/…),
  `CalculatorInputState.kt` (`rememberInput`).
- `planner/domain/` — `ShotOrdering.kt` (pure shot-reordering helper for the planner checklist).
- `planner/presentation/` — `PlannerScreen` (hub), `Locations*`, `Shoots*`,
  `ShootDetail*` (screens + `@HiltViewModel`s).
- `core/location/` — `DeviceLocationProvider` (one-shot fused current-location lookup for
  planner/calculator autofill).
- `core/storage/` — `PersistedDocumentAccess` + `ReferencePhotoGrantManager` (shared persisted
  document labels + URI-permission handling for Gear and Planner reference photos).
- `metering/` — the original light meter feature (data/domain/di/presentation).
- `core/data/` — `SettingsRepository`, `CoreDataModule` (Hilt: DataStore + Room + DAOs),
  `db/` (`AppDatabase`, entities, DAOs incl. `GearItemEntity` / `GearItemDao`,
  `GearFilterEntity` / `GearFilterDao`,
  `GearBatteryEntity` / `GearBatteryDao`, `GearMemoryCardEntity` / `GearMemoryCardDao`,
  `GearLoanEntity` / `GearLoanDao`,
  `FilmStockEntity` / `FilmStockDao`,
  `FilmRollEntity` / `FilmFrameEntity` / `FilmRollDao`,
  `GearKitEntity` / `GearKitItemEntity` / `GearKitDao`, and
  `GearMaintenanceEntryEntity` / `GearMaintenanceDao`).
- `ui/components/` — `ToolCard`, `ResultRow`, `SectionHeader`, `LabeledField`,
  `PlaceholderScreen`. `ui/location/` — reusable current-location permission/action UI.
  `ui/theme/` — color/type/theme incl. NIGHT (red) mode.

---

## 3. Toolchain (known-good — do not change casually)
- AGP `9.2.1` · Kotlin `2.2.10` · Compose BOM `2026.02.01` · Hilt `2.59.2`
  (via **KSP** `2.2.10-2.0.2`, not kapt) · Room `2.7.1` · Navigation `2.9.0` ·
  DataStore `1.1.1` · Play Services location `21.3.0` · Gradle wrapper `9.4.1` ·
  minSdk 24 / targetSdk 36 / JDK 21.
- Versions are centralized in `gradle/libs.versions.toml`.
- Do **not** apply `org.jetbrains.kotlin.android` (AGP built-in Kotlin is used).
- `app/build.gradle.kts` now enables **core library desugaring** so existing `java.time`
  usage works correctly on minSdk 24–25 devices.
- `android.disallowKotlinSourceSets=false` is required (Gradle warns it's experimental — expected).

---

## 4. Build / test commands (Windows PowerShell)
```powershell
# Build + all unit tests (run after every change):
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain

# Filter noisy output:
... 2>&1 | Select-String -Pattern "BUILD|FAILED|error:|e: "

# Run one test class:
.\gradlew.bat :app:testDebugUnitTest --tests "*CelestialPositionTest" --console=plain

# Count tests (PASS = 0 failures/errors):
$x=(Select-Xml -Path "app\build\test-results\testDebugUnitTest\*.xml" -XPath "//testsuite").Node
"Total: $(($x|Measure-Object tests -Sum).Sum), fail $(($x|Measure-Object failures -Sum).Sum), err $(($x|Measure-Object errors -Sum).Sum)"
```
**Current status: `assembleDebug` succeeds; 136 unit tests, 0 failures.** CI at
`.github/workflows/android-ci.yml` (JDK 21, runs tests + assembleDebug + uploads APK).
The user commits the code themselves — **do not git commit** unless asked.

---

## 5. Recipe — add a new calculator tool (the common task)
1. **Domain:** create `calculators/domain/XYZ.kt` — pure top-level function(s), nullable
   return on invalid input. Guard inputs: reject non-finite / non-positive
   (`!value.isFinite() || value <= 0 -> return null`). No `android.*`.
2. **Test:** `app/src/test/java/.../calculators/domain/XYZTest.kt` — assert against a
   known reference value with a sensible tolerance.
3. **Screen:** `calculators/presentation/XYZScreen.kt`:
   ```kotlin
   @Composable fun XYZScreen(modifier: Modifier = Modifier) {
     var a by rememberInput("50")                 // strings survive rotation
     val result = a.toDoubleOrNull()?.let { compute(it) }
     CalculatorScaffold(modifier) {
       SectionHeader("Title", subtitle = "…")
       LabeledField("Label", a, { a = it }, suffix = "mm")  // KeyboardType.Text for text
       if (result != null) ResultCard { ResultRow("Out", formatMeters(result)) }
       else CalculatorHint("Enter …")
     }
   }
   ```
4. **Wire nav:** add a `const val XYZ` to `Routes` + a `titleForRoute` entry
   (Destinations.kt); add `composable(Routes.XYZ) { XYZScreen() }` to the `NavHost`
   (ShutterDeckRoot.kt); add a `ToolEntry(..., Routes.XYZ)` to `tools` (HomeScreen.kt).
5. **Build + test.**

### Recipe — add a Room-backed feature (like Locations/Shoots)
- Add `@Entity` + `@Dao` in `core/data/db/`; register the entity & abstract DAO in
  `AppDatabase` and **bump `version`**; add a `@Provides` for the DAO in `CoreDataModule`.
  The DB uses `fallbackToDestructiveMigration(dropAllTables = true)` (early dev — a version
  bump wipes local data; fine for now, revisit before real users).
- Add a `@HiltViewModel` exposing `dao.observeX().stateIn(viewModelScope, WhileSubscribed(5000), …)`
  and `viewModelScope.launch { dao.upsert/delete(...) }` mutators.
- Screen uses `viewModel: XViewModel = hiltViewModel()` + `collectAsStateWithLifecycle()`.
- For a detail screen with an id arg: route `"x/{xId}"`, `navArgument(...) { type = NavType.LongType }`,
  read it in the VM via `SavedStateHandle.get<Long>("xId")`. See `ShootDetailViewModel`.

---

## 6. Reference values baked into tests (don't "fix" these)
- ND 10-stop on 1/60 ≈ 17.07 s · DoF 50mm f/8 @5m FF → near 3.39m/far 9.53m/H 10.47m ·
  FoV 50mm FF → 39.6° H · Sunny16 f/16 ISO100 → 1/100 · GN 56 @5m ISO100 → f/11.2 ·
  Macro 50mm+25mm → 0.5x, +1.17 stops · Diffraction f/8 → Airy 10.736µm.
- Sun NYC 2024-06-20 ~13:00 EDT → altitude ≈ 72.7°, azimuth ≈ south (181°).
- Moon: 2024-06-22 full (frac > 0.98), 2024-06-06 new (frac < 0.02), 2024-06-14 first quarter.
- Gotcha: ND optical-density 0.9 → 2.99 stops (not exactly 3); test tolerance 0.05.

---

## 7. Behavioral notes
- Ambient EV = `log2(lux / 2.5)` at ISO 100 with a persisted calibration offset.
- Sun/moon: latitude +N, longitude +E, `utcOffsetHours` is the local offset (e.g. -4 EDT).
  Sun-times return minutes-after-local-midnight (`formatClock` normalizes day overflow,
  shows "—" for polar day/night). Azimuth is a compass bearing from true north.
- Theme NIGHT mode is red-on-black to preserve night vision for astro.

---

## 8. Best next steps (prioritized — see ROADMAP §3 for full detail)
1. **Phase 7 quick wins (U1/U3/U5):** spirit level (accelerometer), gray-card screen,
   composition overlays on the CameraX preview.
2. **Phase 8 KMP discipline follow-through** — keep new domain helpers Android-free and continue
   shrinking presentation-only feature files when you touch them.
3. **Phase 6/7 backlog** — business and on-shoot utilities remain the next broad feature area
   now that planner polish is complete.

### Easy grab-bag (no tool too small — ROADMAP §4)
EV↔lux↔foot-candle converter; Kelvin/mired WB converter; ft/m & °C/°F unit converter;
cheat-sheet library (fireworks, milky way, waterfalls); battery/card-capacity estimator;
dew-point lens-fog warning; gel/CTO-CTB calculator. Each is a ~1 domain file + 1 test +
1 screen following §5.

---

## 9. Known constraints / risks
- `fallbackToDestructiveMigration(dropAllTables = true)`: bumping the Room `version` deletes
  local data. Acceptable now; add real `Migration`s before shipping to users.
- Gear inventory, kits and maintenance logs are still mostly **free-form**. Catalog seeding now
  works, but the source models still live under `metering/presentation`; moving them into a
  more neutral shared package is still a worthwhile cleanup, not a blocker.
- Loan/rental reminders are currently **in-app status cues**, not scheduled Android notifications,
  and they only light up when `GearLoanEntity.dueDateText` is saved as ISO `YYYY-MM-DD`.
- Insurance export currently covers the owned inventory list (`GearItemEntity`) only, because
  that is where serial numbers, purchase/current values, purchase source, storage, and reference
  photo attachments are stored today. CSV/PDF files are saved through SAF `CreateDocument`,
  and the app still does **not** store a currency code alongside entered values.
- Filter compatibility is **derived**, not hard-linked: `GearItemEntity.filterThreadSizeText`
  and `GearFilterEntity.threadSizeText` are normalized via a blank-safe helper so unfilled
  values never match each other by accident.
- Film stocks are stored in Room so future rolls can reference one stable ID path, but the bundled
  starter catalog is intentionally **read-only** inside the app. Users extend the library by adding
  custom rows; there is no JSON import/export path for film stocks yet.
- Film rolls snapshot stock metadata instead of live-reading every field from `FilmStockEntity`, so
  historical logs and CSV exports stay readable even if a linked stock changes later.
- The FL3 development timer is intentionally **session-only** for now: recipe inputs are not saved
  to Room, audio/haptic cues are not implemented yet, and temperature compensation currently follows
  a built-in 16–26C chart with interpolation rather than a fully user-editable chemistry profile.
- FL4 push/pull and FL5 reciprocity notes are currently **clipboard-generated session aids**, not
  Room-backed records. Push/pull latitude depends on a live stock entry for `maxPushStops` /
  `maxPullStops`, while reciprocity remains available from roll snapshots because rolls persist the
  reciprocity exponent/onset fields.
- Built-in film reciprocity curves are **starter fits**, not authoritative datasheet replacements.
  Users should keep their own tested custom stock entries when their long-exposure notes differ.
- Gear reference photos are stored as persisted document URI strings; the screen currently shows
  attachment labels only, not thumbnails, and `GearInventoryViewModel` now owns URI-permission
  acquisition/release so replace, clear, and delete do not leak grants.
- `LightMeterScreen.kt` is much more presentation-focused after the completed meter-polish pass,
  but it is still a large Compose file; split it only when another feature change creates a clear,
  low-risk opportunity.
- Room is now at **DB v13** after adding optional `ShootEntity.locationId`; the app still uses
  `fallbackToDestructiveMigration(dropAllTables = true)` rather than hand-written migrations.
- KMP `shared` module not yet extracted (Phase 8). Keep domain Android-free to make it cheap.
- No instrumented/Robolectric tests for DAOs (project is JVM-unit-test only) — DAO logic is
  thin and matches the untested `ScenePresetDao` precedent.
