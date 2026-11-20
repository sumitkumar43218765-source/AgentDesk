# M2 — Onboarding, Tool Setup & Tool Manager

**Depends on:** M0, M1 · **PoC gates:** G1, G2, G7 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** first-run flow `Splash → Tool Setup → Install (terminal) → Chat`, a manifest-driven installer, and a Tool Manager for later installs/updates/health.

---

## 1. User flow

1. **Splash** — waits for `BootstrapGate` (M1). Then routes:
   - required tools all installed + verified → **Chat/Home**
   - otherwise → **Tool Setup**
2. **Tool Setup screen** — grouped list: *Required* (pre-selected, locked), *Optional* (unselected). Each row: name, one-line purpose, size estimate, checkbox. Footer: total download/disk size, free-space check, Wi-Fi/mobile-data warning. Button: **Done**.
3. **Install screen** — top **progress header** (step list with ✅ ⏳ ⬜ ⚠️ and overall bar) above the **embedded terminal** showing the live installer session. Install runs in `SetupInstallService` (foreground).
4. When **required** tools finish and checks pass → auto-navigate to Chat. Optional tools keep installing in the background (notification + progress in Tool Manager).
5. Failure: failed step shows **Retry**, **Skip** (optional only), **Open terminal**, **Copy log**.

## 2. Tool manifest

Bundled `assets/tool_manifest.json`, `schemaVersion` checked on load. Remote manifests are *Later*.

```json
{
  "schemaVersion": 1,
  "tools": [
    {
      "id": "jdk-17",
      "name": "Java JDK 17",
      "description": "Needed by ecj, d8 and apksigner",
      "category": "CORE",
      "required": true,
      "dependsOn": ["rootfs-ubuntu"],
      "sizeEstimateMb": 300,
      "environment": "PROOT_UBUNTU",
      "source": { "kind": "APT", "packages": ["openjdk-17-jdk-headless"] },
      "install": ["apt-get update", "apt-get install -y openjdk-17-jdk-headless"],
      "verify": "java -version",
      "version": "17"
    }
  ]
}
```

`source.kind`: `APT | URL_TARBALL | GITHUB_RELEASE | SCRIPT`. Android build-tools on ARM64 use whichever kind G1 proves workable.

**MVP catalog** (sizes are estimates, replace with G7 measurements):

| id | Required | Est. size | Depends on | Verify |
|---|---|---|---|---|
| `rootfs-ubuntu` | yes | 0.5–1 GB | bootstrap | `cat /etc/os-release` |
| `jdk-17` | yes | ~0.3 GB | rootfs | `java -version` |
| `android-sdk-min` (android.jar platform + aapt2, d8, zipalign, apksigner) | yes | 0.4–0.6 GB | jdk-17 | `aapt2 version`, `d8 --version` |
| `ecj` | yes | ~20 MB | jdk-17 | `ecj -version` |
| `git` | yes | ~50 MB | rootfs | `git --version` |
| `ripgrep` | no | small | rootfs | `rg --version` |
| `python3` | no | 0.1–0.2 GB | rootfs | `python3 --version` |
| `nodejs` | no | ~0.2 GB | rootfs | `node --version` |
| `gradle` (experimental) | no | ~0.2 GB | jdk-17 | `gradle --version` |
| `ndk-clang` (experimental, gated by G6) | no | 0.4–0.7 GB | rootfs | `clang --version` |
| `cmake-ninja` | no | 0.05–0.1 GB | ndk-clang | `cmake --version` |

## 3. Installer engine

- **`InstallPlanner`** — resolves selection + dependencies (topological order), totals sizes, checks free space ≥ total × 1.5 + 500 MB, blocks with a clear message otherwise.
- **`InstallScriptGenerator`** — emits one idempotent bash script per run into `setupStateDir`. Each tool block: skip if `done/<id>@<version>` exists **and** `verify` passes; otherwise run `install`, then `verify`, then write the done marker.
- **Status protocol** — the script appends JSON lines to `setupStateDir/status.jsonl`:
  `{"ts":1731900000,"tool":"jdk-17","event":"start|progress|ok|fail|skip","pct":42,"msg":"…"}`. `StatusTailer` exposes them as a `Flow`. The terminal shows the same run live via `openSession`.
- **Resume** — on process death, `SetupInstallService` restarts, reads `status.jsonl` + done markers, continues. Re-running the script is always safe.
- **Retry** — apt/download steps retry 3× with backoff and mirror fallback (mirror configurable in Settings). Network loss pauses, then resumes on connectivity.
- **`SetupInstallService`** — foreground, progress notification, cancel action, partial wake lock only while a step runs.
- **Post-install checks** — `PostInstallCheck` interface; `ToolVerifyCheck` (all `verify` commands) is built in. M4 contributes `HelloWorldSmokeTest` through Hilt multibinding. M2 runs every registered check; "Ready" requires all to pass. (This avoids an M2↔M4 circular dependency.)

```kotlin
interface PostInstallCheck {
    val id: String; val title: String
    suspend fun run(): CheckResult        // Pass | Fail(message, details)
}
data class InstallProgress(val tools: List<ToolProgress>, val overallPct: Int, val phase: Phase)
enum class Phase { PLANNING, INSTALLING, VERIFYING, SMOKE_TEST, DONE, FAILED, PAUSED }
```

## 4. Tool Manager (Settings → Environment)

- List all catalog tools: installed version, size on disk, status, actions **Install / Update / Reinstall / Remove**. Required tools cannot be removed (only **Repair**). Selecting a tool auto-selects its dependencies; removing warns about dependents.
- **Environment Health** card: runs all `verify` commands + checks, shows pass/fail per row, **Run smoke test**, **Copy diagnostics** (redacted).
- **Storage usage**: rootfs, projects, build logs, caches (`StorageUsageCalculator`); "Clean caches" action.
- Guidance text for the phantom-process killer when a long step dies unexpectedly [VERIFY G5].

## 5. Data

`ToolInstallStateEntity(toolId PK, installedVersion, state, lastVerifiedAt, sizeBytes, lastError)` — a **cache**. Truth is the filesystem (done markers + verify). `ToolStateReconciler` runs at app start and after every install.

## 6. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/setup/
├─ domain/
│  ├─ model/  ToolDefinition.kt · ToolSource.kt · ToolCategory.kt · ToolState.kt · InstallProgress.kt · Phase.kt · SetupStatusEvent.kt · CheckResult.kt
│  ├─ repository/ ToolCatalogRepository.kt · ToolStateRepository.kt
│  ├─ PostInstallCheck.kt · InstallPlanner.kt · InstallPlan.kt
│  └─ usecase/ LoadToolCatalogUseCase.kt · PlanInstallUseCase.kt · StartInstallUseCase.kt · ObserveInstallProgressUseCase.kt
│               RetryToolUseCase.kt · RemoveToolUseCase.kt · RunHealthChecksUseCase.kt · ResolveStartRouteUseCase.kt
├─ data/
│  ├─ manifest/ ToolManifestDto.kt · ToolManifestParser.kt · AssetToolCatalogRepository.kt
│  ├─ install/  InstallScriptGenerator.kt · StatusTailer.kt · StatusLineParser.kt · DoneMarkers.kt · SetupInstallService.kt · SetupNotificationFactory.kt
│  ├─ check/    ToolVerifyCheck.kt · StorageUsageCalculator.kt
│  ├─ local/    ToolInstallStateEntity.kt · ToolInstallStateDao.kt · RoomToolStateRepository.kt · ToolStateReconciler.kt
│  └─ di/       SetupModule.kt
└─ presentation/
   ├─ splash/   SplashRoute.kt · SplashScreen.kt · SplashViewModel.kt
   ├─ setup/    ToolSetupRoute.kt · ToolSetupScreen.kt · ToolSetupViewModel.kt · ToolSetupUiState.kt · components/ToolRow.kt · ToolGroupHeader.kt · SizeSummaryBar.kt
   ├─ install/  InstallRoute.kt · InstallScreen.kt · InstallViewModel.kt · components/ProgressHeader.kt · StepRow.kt · InstallActionsBar.kt
   └─ manager/  ToolManagerRoute.kt · ToolManagerScreen.kt · ToolManagerViewModel.kt · components/ToolStatusCard.kt · HealthCheckCard.kt · StorageUsageCard.kt
app/src/main/assets/tool_manifest.json
```

## 7. Edge cases

- Less free space than required → Done button disabled with the exact shortfall.
- App backgrounded or screen locked during install → service continues; notification shows progress.
- Airplane mode mid-download → status `PAUSED`, auto-resume on connectivity.
- Partial/corrupt download → verify fails, step retried once from scratch.
- User deselects nothing and taps Done quickly twice → second tap ignored (idempotent start).
- Required tool failed → Chat stays gated; user can still open terminal.

## 8. Tests

- Unit: manifest parser (valid/invalid/unknown schema), `InstallPlanner` (dependency order, cycles rejected, space check), `StatusLineParser`, route resolution.
- Instrumented: kill app mid-install and confirm resume; verify idempotency by running the script twice.

## 9. Acceptance criteria

- [ ] Fresh install shows Required locked/selected, Optional unselected; total size and free-space check are correct.
- [ ] Selecting `cmake-ninja` auto-selects `ndk-clang`.
- [ ] Install completes on stable Wi-Fi; progress header matches `status.jsonl`; terminal shows live output.
- [ ] Killing the app mid-install resumes without re-downloading finished tools.
- [ ] Chat opens after required tools + all `PostInstallCheck`s pass; optional installs continue in background.
- [ ] Failed step shows Retry/Skip(optional)/Open terminal/Copy log.
- [ ] Tool Manager can install an optional tool later and run Environment Health.
