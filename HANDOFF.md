# Photography Helper Handoff

Date: 2026-05-17

## Project Snapshot

This repository is an Android Jetpack Compose app for a photography assistant focused on practical exposure guidance.

The current app supports:

- Ambient incident-style metering using `Sensor.TYPE_LIGHT`
- Reflective camera metering using CameraX and camera AE metadata
- MVVM state flow with Hilt dependency injection
- Camera body and lens selection with compatibility filtering
- JSON-backed gear catalog loading with Kotlin fallback data
- Searchable gear pickers and persisted shooting setup
- Scene-aware shooting aid with shutter practicality guidance
- Custom catalog import and reset support
- Import validation and preview before applying custom catalog JSON
- Workflow-priority coaching for ISO-first, aperture-first, and shutter-first adjustments

## Current Architecture

Core implementation areas:

- `app/src/main/java/com/example/photography_helper/MainActivity.kt`
  - Activity host for the Compose UI
- `app/src/main/java/com/example/photography_helper/PhotographyHelperApp.kt`
  - `@HiltAndroidApp` application class
- `app/src/main/java/com/example/photography_helper/metering/data/LightMeterRepositoryImpl.kt`
  - Ambient light sensor repository and EV calculation
- `app/src/main/java/com/example/photography_helper/metering/presentation/LightMeterViewModel.kt`
  - Combines ambient and reflective metering into UI state
- `app/src/main/java/com/example/photography_helper/metering/presentation/MeteringUiState.kt`
  - Public metering UI models
- `app/src/main/java/com/example/photography_helper/metering/presentation/CameraMeterPreview.kt`
  - CameraX preview and reflective AE sampling
- `app/src/main/java/com/example/photography_helper/metering/presentation/ExposureCatalog.kt`
  - Fallback camera body and lens definitions plus stop tables
- `app/src/main/java/com/example/photography_helper/metering/presentation/GearCatalogLoader.kt`
  - Bundled asset loading, imported override loading, and merge-by-id behavior
- `app/src/main/java/com/example/photography_helper/metering/presentation/GearSelectionPreferences.kt`
  - Persisted metering, gear, focal length, calibration, and workflow settings
- `app/src/main/java/com/example/photography_helper/metering/presentation/LightMeterScreen.kt`
  - Main screen, gear selection UI, catalog import UI, and shooting aid logic
- `app/src/main/assets/gear_catalog.json`
  - Active bundled gear catalog edited separately from Kotlin fallback data

## Toolchain Notes

Known-good setup in this repo:

- AGP `9.2.1`
- Kotlin `2.2.10`
- Compose BOM `2026.02.01`
- Hilt `2.59.2`
- KSP `2.2.10-2.0.2`
- Gradle wrapper `9.4.1`

Important constraints:

- Do not apply `org.jetbrains.kotlin.android` in this template. AGP built-in Kotlin support is being used.
- `android.disallowKotlinSourceSets=false` is still required here even though Gradle warns it is experimental.
- Hilt is wired through KSP, not `kapt`.

## Latest Completed Work

The most recent completed feature slice added custom catalog validation and preview:

1. Import preview before apply

- Selecting a JSON catalog now validates and previews it instead of immediately writing the override file.
- The catalog card shows which bodies and lenses will be added or overridden, plus the merged result counts.
- Users can apply or dismiss the pending import preview.

2. Import schema validation

- Imported JSON must include `cameraBodyProfiles` and `lensProfiles` arrays.
- The loader rejects empty imports, duplicate ids, blank ids/labels, invalid numeric ranges, and invalid entry shapes with item-level error context.

3. Focused merge tests

- Added JVM unit coverage for merge-by-id replacement/appending behavior.
- Added JVM unit coverage for import preview change summaries.

Files touched for that slice:

- `app/src/main/java/com/example/photography_helper/metering/presentation/GearCatalogLoader.kt`
- `app/src/main/java/com/example/photography_helper/metering/presentation/LightMeterScreen.kt`
- `app/src/test/java/com/example/photography_helper/metering/presentation/GearCatalogLoaderTest.kt`

## Validation Status

Latest successful checks:

- `./gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin`
- `./gradlew.bat assembleDebug`

Latest known device install target:

- `SM-A127M - 13`

Observed status when this handoff was written:

- Unit tests, Kotlin compilation, and debug assemble were succeeding

## Behavioral Notes

- Ambient meter EV conversion is based on `EV = log2(lux / 2.5)` at ISO 100, with a persisted calibration offset.
- Ambient sensor registration is lifecycle-aware and is stopped when the UI leaves the active state.
- Reflective camera metering depends on camera permission and CameraX preview availability.
- Some Android devices will not expose an ambient light sensor. The app already has a degraded UX path for that case.
- The bundled catalog remains in `app/src/main/assets/gear_catalog.json`; imported catalogs are stored internally and applied as overrides.

## Import Format Notes

The importer expects JSON with these top-level arrays:

- `cameraBodyProfiles`
- `lensProfiles`

Imported entries are previewed first, then merged over bundled entries by matching `id` only after the user applies the preview. This means:

- matching `id` replaces the bundled entry
- new `id` adds a new entry
- missing bundled entries are preserved

## Known Constraints And Risks

- The Gradle warning for `android.disallowKotlinSourceSets=false` is expected right now.
- The importer validates key schema and range issues, but it does not yet show a full field-by-field schema report.
- The workflow coaching is practical and usable, but it is still rule-based rather than scene-model or camera-model aware.
- There are dedicated tests for imported catalog merge and preview summaries, but not yet for workflow suggestion helpers.

## Best Next Steps

If work resumes tomorrow, the highest-value next steps are:

1. Add focused workflow tests

- Cover workflow suggestion helpers in `LightMeterScreen.kt` or move them into a more testable domain helper.

2. Add an exposure recipe summary
   - Present a compact recommendation combining shutter target, aperture, ISO, and the priority-based adjustment path.

3. Consider custom user scene presets
   - Persist named motion/stabilization combinations rather than only shipping built-in presets.

4. Consider richer import diagnostics

- Show a more detailed schema report or entry-level diff when a custom catalog fails validation.

## Resume Commands

Useful commands when picking this back up:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

If you need to resume from the UI/feature surface rather than the build system, start in:

- `app/src/main/java/com/example/photography_helper/metering/presentation/LightMeterScreen.kt`
- `app/src/main/java/com/example/photography_helper/metering/presentation/GearCatalogLoader.kt`
