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

ShutterDeck is a multi-tool Jetpack Compose app. Bottom navigation has 4 tabs:
**Tools** (sectioned home grid), **Planner** (hub), **Gear** (inventory + filters + power/media + loans + kits + maintenance), **More** (settings/theme).

**14 working tools** (all reachable from the Tools grid, grouped into **Exposure** / **Lens & Focus** / **Planning & Output**):
1. Light Meter — ambient (lux sensor) + reflective (CameraX) metering, gear-aware coaching.
2. Depth of Field · 3. ND Filter · 4. Field of View · 5. Astro Shutter (500/NPF) ·
6. Print Size · 7. Focus Stacking · 8. Sunny 16 / reciprocity · 9. Guide Number (flash) ·
10. Equivalent Exposure · 11. Macro / Extension · 12. Diffraction Limit ·
13. Golden Hour (sun times) · 14. Sun & Moon position + moon phase.

**Planner tab** is a hub linking: Golden Hour, Sun & Moon, **Scouting Locations** (Room CRUD),
and **Shoots** (Room-backed shoot list → per-shoot shot checklist).

**Gear tab** now has eight useful capabilities:
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
7. **Packing kits** — build named kits from saved gear, see total weight, and tick items off
   as packed before leaving.
8. **Maintenance log** — record cleanings, firmware updates, repairs and shutter-count checkpoints
   against saved gear items.
Reference photos currently surface as attachment labels with replace/clear actions rather than
full in-app previews.

**Persistence:** Room DB `shutterdeck.db` (v10) + DataStore (Preferences) for theme/settings.
This build still uses `fallbackToDestructiveMigration(dropAllTables = true)`, so upgrading from
v9 to v10 wipes local Room data; reseed the gear catalog and recreate local planner/gear records after install.

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
- `gear/presentation/` — `GearInventoryScreen`, `GearInventoryViewModel`, `GearItemEditState`
  (inventory + filter + battery/card + loans + kits + maintenance summaries),
  `GearFilterTrackerSection.kt`, `GearSupportTrackerSection.kt`, `GearLoanTrackerSection.kt`,
  `GearPresentationFormatting.kt`.
- `gear/domain/` — `GearLoanReminders.kt` (pure reminder helper for due-soon/overdue status).
- `calculators/domain/` — 13 pure calculators incl. `SolarTimes.kt`, `CelestialPosition.kt`.
- `calculators/presentation/` — one screen per calculator + shared infra:
  `CalculatorScaffold.kt` (CalculatorScaffold/ResultCard/CalculatorHint),
  `CalculatorFormatting.kt` (formatMeters/Degrees/OneDecimal/Clock/…),
  `CalculatorInputState.kt` (`rememberInput`).
- `planner/presentation/` — `PlannerScreen` (hub), `Locations*`, `Shoots*`,
  `ShootDetail*` (screens + `@HiltViewModel`s).
- `metering/` — the original light meter feature (data/domain/di/presentation).
- `core/data/` — `SettingsRepository`, `CoreDataModule` (Hilt: DataStore + Room + DAOs),
  `db/` (`AppDatabase`, entities, DAOs incl. `GearItemEntity` / `GearItemDao`,
  `GearFilterEntity` / `GearFilterDao`,
  `GearBatteryEntity` / `GearBatteryDao`, `GearMemoryCardEntity` / `GearMemoryCardDao`,
  `GearLoanEntity` / `GearLoanDao`,
  `GearKitEntity` / `GearKitItemEntity` / `GearKitDao`, and
  `GearMaintenanceEntryEntity` / `GearMaintenanceDao`).
- `ui/components/` — `ToolCard`, `ResultRow`, `SectionHeader`, `LabeledField`,
  `PlaceholderScreen`. `ui/theme/` — color/type/theme incl. NIGHT (red) mode.

---

## 3. Toolchain (known-good — do not change casually)
- AGP `9.2.1` · Kotlin `2.2.10` · Compose BOM `2026.02.01` · Hilt `2.59.2`
  (via **KSP** `2.2.10-2.0.2`, not kapt) · Room `2.7.1` · Navigation `2.9.0` ·
  DataStore `1.1.1` · Gradle wrapper `9.4.1` · minSdk 24 / targetSdk 36 / JDK 21.
- Versions are centralized in `gradle/libs.versions.toml`.
- Do **not** apply `org.jetbrains.kotlin.android` (AGP built-in Kotlin is used).
- `app/build.gradle.kts` now enables **core library desugaring** so existing `java.time`
  usage works correctly on minSdk 24–25 devices.
- `android.disallowKotlinSourceSets=false` is required (Gradle warns it's experimental — expected).
- Harmless warnings: Hilt annotation target (KT-73255). Ignore.

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
**Current status: `assembleDebug` succeeds; 87 unit tests, 0 failures.** CI at
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
1. **Finish Phase 4 Gear** — inventory, catalog seeding, richer metadata/reference-photo support,
   filter/thread tracking, battery/card tracking, loan/rental tracking, packing kits and
   maintenance logs are working, so the next highest-value step is the insurance/export workflow.
2. **Phase 5 Film suite (FL1/FL2)** — film stock DB + roll/frame logger (Room). Strong
   differentiator; reuses reciprocity from Sunny 16 (C6).
3. **Polish P3/P4:** link a shoot to a saved location; per-shot gear/notes; map view for
   locations; reference-photo attachments.
4. **Phase 1 meter polish** — continue moving `LightMeterScreen.kt` logic into pure domain.
5. **Phase 7 quick wins (U1/U3/U5):** spirit level (accelerometer), gray-card screen,
   composition overlays on the CameraX preview.

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
- Filter compatibility is **derived**, not hard-linked: `GearItemEntity.filterThreadSizeText`
  and `GearFilterEntity.threadSizeText` are normalized via a blank-safe helper so unfilled
  values never match each other by accident.
- Gear reference photos are stored as persisted document URI strings; the screen currently shows
  attachment labels only, not thumbnails, and `GearInventoryViewModel` now owns URI-permission
  acquisition/release so replace, clear, and delete do not leak grants.
- Light meter logic still partly lives in `metering/presentation/LightMeterScreen.kt`
  (large file) rather than fully in `domain`; refactor opportunistically.
- KMP `shared` module not yet extracted (Phase 8). Keep domain Android-free to make it cheap.
- No instrumented/Robolectric tests for DAOs (project is JVM-unit-test only) — DAO logic is
  thin and matches the untested `ScenePresetDao` precedent.
