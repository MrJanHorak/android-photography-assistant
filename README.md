# ShutterDeck

ShutterDeck is an Android photography-assistant app built with Jetpack Compose. It aims to be a
"Swiss army knife" for photographers: exposure references, on-shoot utilities, planning tools,
gear tracking, and film workflows in one local-first app.

This repository is still moving quickly, so this README stays intentionally high-level.
For the authoritative current feature snapshot, read `HANDOFF.md`. For the backlog and phased plan,
read `ROADMAP.md`.

## What works today

### Tools tab

The **Tools** tab is the biggest surface in the app. It currently includes 26 calculator/reference
tools grouped into:

- **Exposure** tools such as Light Meter, ND Filter, Sunny 16, EV/Lux, Color Temperature, and Unit Converter.
- **Lens & Focus** tools such as Depth of Field, Focus Stacking, Field of View, Macro, and Diffraction.
- **Planning & Output** tools such as Golden Hour, Sun & Moon, and Print Size.
- **On-Shoot Utilities** such as Spirit Level, Gray Card, Intervalometer, Dew Point, Digital Slate,
  Shot Notes, Lighting Setup, Composition Overlays, and Live Histogram & Zebra.

The Tools screen also includes a lightweight search field so the grid can be filtered by tool name
or description.

### Planner tab

The **Planner** tab covers scouting locations and saved shoots, including shot checklists, linked
locations, current-location autofill, map preview, and astronomy helpers for planning. Shoot
create/edit drafts now survive rotation, and linked locations can reopen the same in-app map
preview directly from shoot detail. The shoot and location editors now use roomier bottom sheets
instead of cramped dialog forms.

### Gear tab

The **Gear** tab covers inventory plus field logistics: filters, batteries, memory cards, loans,
packing kits, maintenance records, and insurance/export summaries.

### Film tab

The **Film** tab covers analog workflows: film stock library, roll/frame logging, development timer,
push-pull helper, and reciprocity assistant.

### More tab

The **More** tab currently includes theme settings plus lightweight in-app help covering the tab
layout, permissions, immersive tools, and local-first behavior.

## Permissions and local data

- **Camera** is requested only for metering and live-preview tools.
- **Location** is requested only for planning, astronomy, and note helpers that can use it.
- **Speech recognition / microphone access** is used only for shot-note dictation flows.
- Most saved data stays **local on the device** unless the user explicitly exports, shares, or
  attaches something through the Android system picker.

## Tech stack

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose
- Hilt for dependency injection
- Room + DataStore for persistence/settings
- CameraX for preview/metering utilities
- Play Services Location
- osmdroid for map preview
- JVM unit tests for Android-free domain logic

The long-term architecture direction is to keep business/domain logic Android-free so it can move
cleanly into a future Kotlin Multiplatform shared module.

## Build and test

Use a recent Android Studio setup with Android SDK 36 installed.

### Build the debug app

```powershell
.\gradlew.bat :app:assembleDebug
```

### Run JVM unit tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

### Run the standard validation pass

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

## Project guide

- `HANDOFF.md` - current technical snapshot, major features, important files, and working patterns
- `ROADMAP.md` - phased backlog and future directions
- `app/src/main/java/com/janhorak/shutterdeck/` - main app source

## What is left

The named Phase 7 utility backlog is complete. The remaining work is broader expansion and polish:

- more small tools from the grab-bag backlog (for example battery/card estimator and
  color-temperature gel math)
- future business/pro workflow slices from the roadmap
- Phase 8 discipline follow-through toward a shared Kotlin Multiplatform module

See `ROADMAP.md` for the exact backlog and acceptance criteria.
