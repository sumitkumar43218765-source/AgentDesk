# {APP_NAME} — PRD v1.0 — 00 Overview & Architecture

Working title: `{APP_NAME}` · Package: `{ROOT_PKG}` (placeholders, replace globally before coding)
Platform: Android (arm64-v8a only) · Language: Kotlin (app) + Java (Termux fork)

---

## 0. How the coding agent must use this PRD

1. Build **one module at a time**, in the order of §9. Read this file + the one module file only.
2. **One class per file.** File trees in module files are the *minimum*; split further, never merge.
3. Never change another module's public interface without updating this PRD first.
4. A module is done only when **all its Acceptance Criteria pass** and the Global DoD (§10) is met.
5. Items tagged **[VERIFY]** are assumptions that must be validated on a real device (PoC-0, §8) before dependent code is written. If a [VERIFY] item fails, stop and report; do not improvise a workaround.
6. Do not add features that are not in this PRD. Features marked *Later* get interfaces/hooks only.
7. Do not hardcode filesystem paths. All paths come from `PathProvider` (M1).

---

## 1. Product summary

A mobile, small version of **Claude Desktop** for Android phones: a chat UI plus an autonomous AI agent that works *on the phone itself*. The agent's "computer" is an **embedded Termux** (own package name) with a **proot Ubuntu** rootfs. An on-device **Java → APK build pipeline** is one of the agent's tools. The user brings their own LLM API key. No backend, no accounts.

**It is not a code editor.** Files are viewable (read-only); the agent writes code.

## 2. Goals / Non-goals

**Goals (v1)**
- G1. One-tap onboarding: splash → tool setup → auto install → chat.
- G2. Agent can write a Java Android app, build it fully on-device, install it, read errors, fix and retry.
- G3. Embedded terminal with Termux UI inside the app.
- G4. BYO API key, multiple providers, streaming chat.
- G5. Safe by default: permission prompts, project sandbox, git checkpoints.
- G6. Zero-cost infrastructure; offline-capable for terminal and build.
- G7. Material 3, minimal, fast.

**Non-goals (v1)**: code editor, cloud builds, own backend/accounts/telemetry, Play Store distribution, Kotlin/Compose/Gradle builds (Later), iOS, multi-user.

## 3. Primary scenarios

- S1 First run: install → pick tools → wait → chat ready (smoke test green).
- S2 "Make a flashlight app" → agent creates project, writes files, builds, installs APK.
- S3 Build fails → agent reads distilled errors, edits, rebuilds (≤5 retries).
- S4 User opens terminal and runs commands manually in the same environment.
- S5 User exports APK / project zip, pushes to GitHub.
- S6 App killed mid-install or mid-agent-run → resumes/offers to continue.

## 4. Scope map (feature → module)

| Feature | Phase | Module |
|---|---|---|
| Foundation, theme, DI, DB, secrets | MVP | M0 |
| Embedded Termux, ShellBackend, terminal UI | MVP | M1 |
| Onboarding, tool setup, Tool Manager, env health | MVP | M2 |
| Projects, templates, git, file tree/viewer, import/export | MVP | M3 |
| Build pipeline, signing, install, export, history | MVP | M4 |
| LLM providers, streaming chat, history | MVP | M5 |
| Agent loop, tools, permissions, timeline | MVP | M6 |
| Settings, notifications, hardening, release | MVP | M7 |
| MCP, JNI/NDK, Kotlin/Gradle, attachments, plugins, voice | Later | 09 |

## 5. Tech stack & conventions (pinned by preference)

- **Kotlin** + **Jetpack Compose** + **Material 3**. Termux fork code stays **Java**.
- **Architecture:** feature-based **Clean Architecture** (`presentation / domain / data` per feature), unidirectional data flow (`UiState` / `UiEvent` / `UiEffect`, `StateFlow`).
- **DI:** Hilt. **Async:** Coroutines + Flow.
- **Persistence:** **Room 2.x** (do NOT adopt Room 3.0), DataStore for preferences.
- **Annotation processing:** **KSP only. No KAPT.**
- **Network:** OkHttp (+ SSE), kotlinx.serialization.
- **Navigation:** Navigation Compose (type-safe routes).
- **Versions:** latest stable at implementation time, pinned in `gradle/libs.versions.toml`; AGP / Kotlin / Compose compatibility must be checked [VERIFY].
- **SDK:** minSdk 26, compileSdk latest stable, **targetSdk 28** [VERIFY — required for executing binaries from the app's private dir, see M1; suppress `ExpiredTargetSdkVersion` lint].
- **ABI:** `abiFilters = arm64-v8a`.
- **UI language:** English, all strings in `strings.xml`.
- **Logging:** `AppLogger` interface; never log API keys, prompts, or file contents beyond a short preview.

## 6. Architecture

```
Compose UI ── ViewModels ── UseCases ── Repositories ── DataSources
                               │
   ┌───────────────┬───────────┴───────┬─────────────────┐
 M5 LlmProvider   M6 Agent runtime   M4 BuildEngine    M2 ToolManager
                               │              │               │
                               └──────  ShellBackend (M1) ────┘
                                          │
                     EmbeddedShellBackend → TermuxService → proot Ubuntu
```

**Repo layout** (repo = fork of `termux-app`, package renamed):

```
/terminal-emulator   /terminal-view   /termux-shared     (Termux libs; only package/constants changed)
/app/src/main/java/{ROOT_PKG}/
    termux/      forked Termux Java code (TermuxService etc.)  ← only M1 edits here
    core/        M0
    feature/     setup/ projects/ build/ ai/ chat/ agent/ terminal/ settings/
    app/         Application, MainActivity, nav host
/tools/termux-generator/   generator config for custom-package bootstrap
/.github/workflows/        build-debug, build-release, termux-bootstrap
/docs/poc0-results.md
```

**Dependency rule:** presentation → domain ← data. Features talk to each other only through `domain` interfaces. `core` depends on nothing in `feature`.

**Long-running work never lives in a ViewModel.** Setup install, builds, and agent runs run in foreground services with their own scopes (`SetupInstallService`, `BuildService`, `AgentRunService`).

## 7. Cross-cutting rules

- **Errors:** `AppResult<T>` + `sealed interface AppError` (Network, Auth, RateLimit, Storage, Shell, Build, Permission, Cancelled, Unknown). No raw exceptions across layer boundaries.
- **Paths:** only via `PathProvider`.
- **Secrets:** API keys and passwords only in `SecretStore` (Android Keystore-backed). Never in Room, DataStore plain, logs, or prompts.
- **Sandbox:** agent file tools are confined to the project root; agent shell runs inside proot with minimal binds. proot is defense-in-depth, **not** a hard security boundary.
- **Network:** app contacts only user-configured LLM endpoints and tool download sources. No analytics.
- **Licensing:** Termux code is GPL-licensed [VERIFY per-library license]; the app ships under a GPL-compatible license with source link and a third-party licenses screen.
- **Distribution:** GitHub Releases / F-Droid. Play Store not targeted (targetSdk 28) [VERIFY].
- **Diagnostics:** rotating local log file, "Export diagnostics" with redaction.
- **NFRs:** Splash→first screen < 2 s on warm start; 60 fps lists; wake lock only during active runs; terminal/build usable offline; chat requires network.

## 8. Decisions log and PoC-0 gates

**Decisions already made**

| # | Decision | Reason |
|---|---|---|
| D1 | App = mini Claude Desktop (chat + agent), not an editor | Product vision |
| D2 | Builds are on-device only | Product vision |
| D3 | Termux fully embedded (fork, custom package name), no RUN_COMMAND in user-facing flow | UX: one app, one tap onboarding |
| D4 | Bootstrap = base Termux + proot; tools live in a proot Ubuntu rootfs installed via `apt` | Official Termux repos don't work with a custom package name; avoids hosting a custom repo |
| D5 | Projects live in the rootfs home (`/root/projects`), exported to shared storage | Compilers/git need a Linux filesystem; app can read it directly via host path |
| D6 | Java-only pipeline first (`aapt2 → ecj → d8 → zipalign → apksigner`); JNI/NDK later | Smallest verifiable pipeline |
| D7 | `ShellBackend` interface with `EmbeddedShellBackend` (prod), `FakeShellBackend` (tests), `ExternalTermuxBackend` (debug-only dev bridge, RUN_COMMAND) | Lets agent/chat work proceed before the fork is ready |
| D8 | `git` is a **required** tool (projects depend on it) | Reconciles M2 and M3 |
| D9 | Onboarding: required tools locked and pre-selected, optional tools unselected, installable later | User flow |
| D10 | Chat opens when **required** tools finish; optional tools continue in background | Avoid blocking on optional installs |

**PoC-0 (manual, in the current Termux + proot Ubuntu; record in `docs/poc0-results.md`)**

| Gate | Pass criteria | Blocks |
|---|---|---|
| G1 | ARM64 `aapt2` (and `zipalign`, `d8`, `apksigner`) run on device; source identified | M2, M4 |
| G2 | Hello World APK built manually with the full pipeline, installs and launches | M4 |
| G3 | Custom-package Termux built with termux-generator installs **alongside** official Termux; bootstrap extracts; proot + Ubuntu login works | M1 |
| G4 | Binaries execute from the private dir with chosen targetSdk on Android 14/15 | M1 |
| G5 | A 5-minute build survives the Android phantom-process killer | M4, M7 |
| G6 | aarch64 clang/NDK usable in rootfs (gates *Later* JNI/NDK only) | Later |
| G7 | Baseline numbers: RAM, free storage, rootfs + toolchain size, full build time | M2 estimates |

## 9. Module index and build order

| ID | Module | File | Depends on | Needs PoC |
|---|---|---|---|---|
| M0 | Foundation | 01_M0_FOUNDATION.md | — | — |
| M5 | AI providers & Chat | 06_M5_AI_AND_CHAT.md | M0 | — |
| M1 | Termux core & terminal | 02_M1_TERMUX_CORE.md | M0 | G3, G4 |
| M2 | Setup & Tool Manager | 03_M2_SETUP_AND_TOOLS.md | M0, M1 | G1, G2 |
| M3 | Projects & Git | 04_M3_PROJECTS.md | M0, M1 | — |
| M4 | Build engine | 05_M4_BUILD.md | M1, M2, M3 | G1, G2, G5 |
| M6 | Agent runtime | 07_M6_AGENT.md | M3, M4, M5 | — |
| M7 | Settings & hardening | 08_M7_SETTINGS_AND_HARDENING.md | all | — |
| — | Later modules & roadmap | 09_LATER_AND_ROADMAP.md | — | G6 |

**Order:** run PoC-0 in parallel with **M0 → M5 → M1 → M2 → M3 → M4 → M6 → M7**.

## 10. Global Definition of Done

- `assembleDebug` succeeds; no lint errors; unit tests pass.
- All module Acceptance Criteria pass on a real arm64 device.
- No hardcoded paths, strings, or secrets; no `TODO` without a tracked note.
- Public interfaces match this PRD; changes are reflected in the PRD.
- Error, empty and loading states exist for every screen.

## 11. Glossary

- **Rootfs**: the Ubuntu filesystem under proot. **Guest path**: path inside proot (`/root/projects/x`). **Host path**: same file seen by the Android app.
- **Bootstrap**: minimal Termux userland extracted on first launch.
- **Tool**: an installable toolchain component (M2). **Agent tool**: a function the LLM can call (M6).
- **Run**: one agent execution from user message to final answer.
