# M4 — Build Engine (Java → APK, on-device)

**Depends on:** M0, M1, M2, M3 · **PoC gates:** G1, G2, G5 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** build a signed APK from a `hello-java`-style project entirely on the phone, report structured diagnostics, install/export the result, and keep history.

---

## 1. Pipeline (D6)

Steps run inside proot (`ShellEnvironment.PROOT_UBUNTU`) with `cwd = guestPath(project)`. Env: `ANDROID_JAR` (platform android.jar), `BUILD_TOOLS` (dir with aapt2/d8/zipalign/apksigner), `JAVA_HOME`. All outputs go to `build/`.

| # | Step class | Reference command (flags **[VERIFY in G2]**) |
|---|---|---|
| 1 | `ResourceCompileStep` | `aapt2 compile --dir res -o build/res.zip` |
| 2 | `ResourceLinkStep` | `aapt2 link -I $ANDROID_JAR --manifest AndroidManifest.xml --java build/gen --min-sdk-version <min> --target-sdk-version <target> -o build/app.unsigned.apk build/res.zip` |
| 3 | `JavaCompileStep` | `ecj -source 1.8 -target 1.8 -cp "$ANDROID_JAR:libs/*" -d build/classes $(find src build/gen -name '*.java')` |
| 4 | `DexStep` | `d8 --release --lib $ANDROID_JAR --min-api <min> --output build/dex $(find build/classes -name '*.class') libs/*.jar` |
| 5 | `PackageStep` | add `build/dex/classes.dex` to `build/app.unsigned.apk` |
| 6 | `AlignStep` | `zipalign -f -p 4 build/app.unsigned.apk build/app.aligned.apk` |
| 7 | `SignStep` | `apksigner sign --ks <keystore> --ks-pass … --out build/app.apk build/app.aligned.apk` |

- Each step has its own timeout (default 120 s; dex/compile 300 s), its own log section, and duration.
- A failed step stops the pipeline; later steps are marked `SKIPPED`.
- MVP does a **full rebuild** every time (`build/` cleaned). Incremental builds are *Later*.
- `libs/*.jar` on the classpath and dexed. **AAR, Maven resolution, Kotlin, Gradle: not supported** (Later).
- `NativeBuildStep` is declared as an interface only (hook for JNI/NDK, see 09).

## 2. Public interfaces

```kotlin
interface BuildEngine {
    fun build(projectId: String, variant: BuildVariant = BuildVariant.DEBUG): Flow<BuildEvent>
    suspend fun cancel(buildId: String)
    fun observeHistory(projectId: String): Flow<List<BuildRun>>
    suspend fun getRun(buildId: String): BuildRun?
    suspend fun readLog(buildId: String, offset: Int, limit: Int): AppResult<LogSlice>
}

sealed interface BuildEvent {
    data class Started(val buildId: String) : BuildEvent
    data class StepStarted(val step: BuildStepId) : BuildEvent
    data class StepLog(val step: BuildStepId, val chunk: String) : BuildEvent
    data class StepFinished(val step: BuildStepId, val status: StepStatus, val durationMs: Long) : BuildEvent
    data class Finished(val run: BuildRun) : BuildEvent
}

data class BuildRun(
    val id: String, val projectId: String, val variant: BuildVariant,
    val status: BuildStatus,                    // SUCCESS | FAILED | CANCELLED
    val startedAt: Long, val finishedAt: Long?,
    val apkHostPath: String?, val logPath: String,
    val summary: BuildSummary, val gitCommit: String?,
)
data class BuildSummary(val failedStep: BuildStepId?, val diagnostics: List<BuildDiagnostic>, val steps: List<StepResultSummary>)
data class BuildDiagnostic(val severity: Severity, val file: String?, val line: Int?, val column: Int?, val message: String, val step: BuildStepId)

interface ApkInstaller { suspend fun install(apk: File): AppResult<InstallOutcome> }          // PackageInstaller session API
interface ApkExporter  { suspend fun exportToDownloads(run: BuildRun): AppResult<Uri> }
interface FixWithAiLauncher { fun launch(projectId: String, buildId: String) }                // implemented by M6

object LogDistiller { fun distill(raw: String, maxChars: Int = 6000): DistilledLog }          // shared with M6
```

## 3. Functional requirements

- M4-FR-01 `build()` runs steps in order, streams events, persists a `BuildRun`, never throws.
- M4-FR-02 **`BuildErrorParser`** converts ecj and aapt2 output into `BuildDiagnostic`s (file/line/column/severity/message). Unknown lines are kept as raw log only.
- M4-FR-03 **`LogDistiller`**: strip ANSI, collapse repeated lines, keep first 20 + last 60 lines, always include lines matching `error|ERROR|FAILED|Exception|warning: .*deprecat` (errors first), cap at `maxChars`, add `[N lines omitted]`. Full log stays on disk.
- M4-FR-04 **Signing**: `KeystoreManager` creates a debug keystore on first build with `keytool` in the rootfs (standard debug alias/passwords); supports importing a release keystore via system picker; release passwords are stored only in `SecretStore`.
- M4-FR-05 **Install** via `PackageInstaller`; handles "Install unknown apps" permission (deep link to settings), signature mismatch (offer uninstall-then-install), and result broadcast. Needs `REQUEST_INSTALL_PACKAGES`.
- M4-FR-06 **Export** the APK to `Downloads/{APP_NAME}/<slug>-<versionName>-<yyyyMMdd-HHmm>.apk` (MediaStore where applicable [VERIFY under targetSdk 28]); share sheet.
- M4-FR-07 **History**: `BuildRunEntity` in Room; logs in `buildLogsDir/<buildId>.log`; keep last 20 runs per project and ≤ 200 MB total (oldest pruned).
- M4-FR-08 **One build at a time** globally (queue the second); cancel kills the process tree.
- M4-FR-09 `BuildService` (foreground) with progress notification and Cancel action; wake lock only while building.
- M4-FR-10 **Smoke test**: `HelloWorldSmokeTest : PostInstallCheck` copies the `hello-java` template to a temp project under the rootfs cache, builds it, checks `build/app.apk` exists and `apksigner verify` passes, then deletes it. It does not install the APK.
- M4-FR-11 After a successful build, if auto-checkpoint is on, call `GitService.checkpoint(projectId, "build <id> ok")` and store the commit id on the run.
- M4-FR-12 Reads `package`, `minSdk`, `targetSdk` from the manifest; fails early with a readable diagnostic if missing.

## 4. UI

- **`BuildScreen`** (per project): Build / Cancel button; live step list (name, status icon, duration); collapsible raw log (monospace, auto-scroll, copy); diagnostics list — tap opens the file viewer at the line (M3); after success: **Install**, **Export**, **Share**; after failure: **Fix with AI** (calls `FixWithAiLauncher`).
- **`BuildHistoryScreen`**: runs with status, time, size; open details; delete log.
- Empty/error states for: missing tools (link to Tool Manager), no project selected.

## 5. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/build/
├─ domain/
│  ├─ model/ BuildRun.kt · BuildEvent.kt · BuildSummary.kt · BuildDiagnostic.kt · BuildVariant.kt · BuildStatus.kt · BuildStepId.kt · StepStatus.kt · LogSlice.kt · DistilledLog.kt · InstallOutcome.kt
│  ├─ BuildEngine.kt · ApkInstaller.kt · ApkExporter.kt · FixWithAiLauncher.kt · KeystoreManager.kt
│  └─ usecase/ StartBuildUseCase.kt · CancelBuildUseCase.kt · ObserveBuildHistoryUseCase.kt · InstallApkUseCase.kt · ExportApkUseCase.kt
├─ data/
│  ├─ engine/ DefaultBuildEngine.kt · BuildContext.kt · BuildStep.kt · BuildEnvironment.kt · BuildScheduler.kt
│  │  steps/  ResourceCompileStep.kt · ResourceLinkStep.kt · JavaCompileStep.kt · DexStep.kt · PackageStep.kt · AlignStep.kt · SignStep.kt · NativeBuildStep.kt (interface only)
│  ├─ parse/  BuildErrorParser.kt · EcjOutputParser.kt · Aapt2OutputParser.kt · LogDistiller.kt · ManifestReader.kt
│  ├─ sign/   ShellKeystoreManager.kt · SigningConfig.kt
│  ├─ install/ PackageInstallerApkInstaller.kt · InstallResultReceiver.kt · MediaStoreApkExporter.kt
│  ├─ local/  BuildRunEntity.kt · BuildRunDao.kt · RoomBuildHistoryRepository.kt · BuildLogStore.kt · BuildRetentionPolicy.kt
│  ├─ check/  HelloWorldSmokeTest.kt
│  ├─ service/ BuildService.kt · BuildNotificationFactory.kt
│  └─ di/     BuildModule.kt
└─ presentation/
   ├─ build/   BuildRoute.kt · BuildScreen.kt · BuildViewModel.kt · BuildUiState.kt · components/StepList.kt · RawLogView.kt · DiagnosticsList.kt · BuildActionsBar.kt
   └─ history/ BuildHistoryRoute.kt · BuildHistoryScreen.kt · BuildHistoryViewModel.kt
app/src/main/AndroidManifest.xml additions: REQUEST_INSTALL_PACKAGES, FOREGROUND_SERVICE, BuildService
```

## 6. Edge cases

- Tools missing/unverified → `build()` returns a clear failure pointing to Tool Manager (no cryptic shell errors).
- Process killed by the OS mid-build → run marked `FAILED(interrupted)`; offer rebuild.
- Storage full → fail fast with `AppError.Storage`.
- Duplicate class / missing symbol / missing resource → surfaced as diagnostics with file+line when parseable.
- Package name changes between builds → installer warns (new app vs update).
- Very large logs → UI shows tail; full log paged via `readLog`.

## 7. Tests

- Unit: `EcjOutputParser`, `Aapt2OutputParser` (recorded real outputs as fixtures), `LogDistiller` (size cap, error priority), `BuildScheduler` (queueing/cancel), `BuildRetentionPolicy`.
- Instrumented: full build of `hello-java`; broken-code build gives diagnostics with line numbers; install on device; cancel mid-build; 5-minute stress build (G5).

## 8. Acceptance criteria

- [ ] `hello-java` builds on a real phone, APK installs and launches.
- [ ] A deliberate syntax error yields a diagnostic with the right file and line and a short distilled log (< 6000 chars).
- [ ] Cancel stops all child processes within 5 s.
- [ ] Smoke test passes in Environment Health and gates "Ready" in M2.
- [ ] APK export lands in `Downloads/{APP_NAME}/` with the expected name; share works.
- [ ] History keeps 20 runs per project and prunes older logs.
- [ ] Release build excludes debug-only classes; no keystore password appears in logs.
