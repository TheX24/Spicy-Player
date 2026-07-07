# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Spicy Player is an offline Android music player whose headline feature is a from-scratch port of the [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) Spicetify extension, targeting **visual parity** with its karaoke-style rendering. Lyrics are drawn on a Jetpack Compose `Canvas` and animated by a custom analytic spring physics engine; audio is handled by Media3/ExoPlayer. AGPL-3.0.

## Build & run

Windows shell here is PowerShell — use `.\gradlew.bat`. On POSIX use `./gradlew`.

- `.\gradlew.bat assembleDebug` — build debug APK (output renamed to `SpicyPlayer-<versionName>.apk`)
- `.\gradlew.bat installDebug` — build + install to a connected device/emulator
- `.\gradlew.bat assembleRelease` — release build (minified/R8; signing below)
- `.\gradlew.bat lint` — Android Lint
- `.\gradlew.bat test` — JVM unit tests; `.\gradlew.bat connectedAndroidTest` for instrumented

Note: there are currently **no test sources** (`app/src/main` only — no `test`/`androidTest` dirs), so the test tasks are effectively no-ops until tests are added.

`local.properties` must point `sdk.dir` at a local Android SDK (the committed value is a Linux path — override for your machine). Release signing reads `app/keystore.properties` (gitignored: `keyAlias`, `keyPassword`, `storeFile`, `storePassword`); if absent, the build silently falls back to the debug keystore, so a release build succeeds without real signing.

## Toolchain & module layout

- Single app module `:app`. There is **no Gradle module per feature** — features are packages inside `:app`, not separate modules.
- `build-logic/` is a composite build of convention plugins applied by id: `com.tx24.android.application`, `com.tx24.android.application.compose`, `com.tx24.android.hilt`, plus `.android.library`, `.android.feature`, `.android.compose`, `.kotlin.library`. Shared config (compileSdk 35, minSdk 21, JVM 11 + core library desugaring, Compose setup) lives in `build-logic/convention/src/main/java/...` — change global build settings there, not in `app/build.gradle.kts`. Note `build-logic/convention/bin/` holds stale compiled copies of these `.kt` files; edit the ones under `src/`.
- Dependencies are centralized in `gradle/libs.versions.toml` (version catalog `libs.*`). Kotlin 2.0, Hilt DI, KSP (Room), Compose BOM.

## Architecture

Hilt-based DI throughout. Two entry points: `SpicyApplication` (`@HiltAndroidApp`) and `MainActivity` (`@AndroidEntryPoint`, all-Compose, single-Activity). UI is assembled in `ui/SpicyApp.kt` → `SpicyApp2`, which hosts the Compose `NavHost` and adapts layout by `WindowSizeClass`.

**Playback** — client/service split over Media3:
- `playback/PlaybackManager.kt` (`@Singleton`) is the app-side facade. It binds a `MediaController` to the service and exposes playback state as `StateFlow`s for the UI.
- `playback/PlaybackService.kt` is the live `MediaSessionService` (referenced by `SessionToken` from `PlaybackManager` and `widgets/`). **`service/MediaPlaybackService.kt` is legacy/unwired — don't extend it; work in `playback/`.**

**Data / library** — `library/` is the canonical data layer:
- `library/store/` — repositories: `MediaRepository` (wraps `MediaStore`, exposes the device library as an auto-updating `StateFlow`), `PlaylistsRepository`, `AlbumsRepository`, `library/store/lyrics/LyricsRepository`, and `library/store/preferences/UserPreferencesRepository` (DataStore).
- `library/database/` — Room (`SpicyDatabase`, `dao/`, `entities/`, `migrations/`). Wired in `library/database/di/DatabaseModule.kt`. Uses explicit migrations plus `fallbackToDestructiveMigration()`; bump `versionCode`/migrations together when changing schema.
- `network/` — Retrofit + Gson for remote lyrics/metadata (e.g. LRCLIB), wired in `network/di/RetrofitModule.kt`.

**Lyrics engine** (`lyrics/spicy/`) — the core differentiator, keep changes faithful to Spicy Lyrics behavior:
- `parser/TtmlLyricsParser.kt` — stateful XML pull-parser: reads `<ttm:agent>` to tag primary (`v1`) vs guest (`v2`) voices, tokenizes `<p>` into per-syllable `Word`s, injects virtual interlude lines.
- `canvas/` — `SpicyLyricsView` + `LyricsRenderer` draw every frame with `TextMeasurer`; `LyricsLayoutCalculator`, `ScrollManager`, and `DynamicBackgroundRenderer`/`StackBlur` handle layout, auto-scroll, and the blurred cover-art backdrop.
- `animation/SpringSimulation.kt` — closed-form (analytic) damped-harmonic-oscillator solver, not Euler/Verlet, so motion is frame-rate independent. Critical damping (ζ=1) for scroll centering; under-damped for word bounce and interlude dots. See README "Project Architecture & Core Logic" for the math.

**Other**: `widgets/` uses Glance for home-screen widgets; `tageditor/` is a self-contained feature (jaudiotagger) with its own nav graph, viewmodel, and UI.

**Feature UI convention**: library features live under `uiLibrary/<feature>/` split into `navigation/` (exposes a `NavGraphBuilder.xxxGraph()` extension), `ui/`, and `viewmodel/`. `ui/SpicyApp.kt` composes these graphs. Follow this shape when adding a feature screen.

## Gotchas

- **Duplicate/parallel packages from an in-progress refactor**: `model/` vs `models/`, top-level `parser/` vs `lyrics/spicy/parser/`, and `library/store/model/` all coexist. Prefer the `library/` tree and the package the surrounding/importing code actually uses; grep imports before adding to one of these rather than guessing.
- `plan.md` is the living feature/bug roadmap — consult it for intended direction and check items off there when completing roadmap work.
- `spicy-lyrics/` (gitignored) is a reference checkout of the original extension for parity work, not part of the build.
