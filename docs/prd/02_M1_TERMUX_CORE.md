# M1 — Termux Core & Terminal

**Depends on:** M0 · **PoC gates:** G3, G4 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** embed Termux (custom package name) inside the app as the execution environment; expose a clean `ShellBackend` + `PathProvider`; provide the in-app terminal UI.

Class/function names of the Termux code below are from the upstream `termux-app` layout (e.g. `TermuxService`, `TerminalView`, `TerminalSession`, `TermuxInstaller`, `AppShell`, `ExecutionCommand`). Check them against the forked source; adapt names, keep the interfaces in §3.

---

## 1. Fork & custom-package bootstrap

1. Create the repo from `termux-app` at a **pinned release tag**. Record the tag in `docs/UPSTREAM.md`.
2. Rename the package with **termux-generator** (config in `/tools/termux-generator/`), `TERMUX_APP_PACKAGE = {ROOT_PKG}` [VERIFY exact variable/flag names].
3. Official Termux repos/packages are built for the `com.termux` prefix and **cannot be mixed** with a custom package name. Therefore the bootstrap contains only the minimum, built for our prefix:
   `bash, coreutils, apt/dpkg stack, proot, proot-distro (or raw proot), curl, ca-certificates, tar, xz-utils, gzip, unzip`.
4. All other tools are installed **inside the proot Ubuntu rootfs** with `apt` (M2). No custom apt repo is hosted.
5. Build the APK + bootstrap on GitHub Actions (`termux-bootstrap.yml`), aarch64 only; upload the bootstrap zip as an artifact and embed it in the APK build.
6. Remove/rename `sharedUserId` and any `com.termux.*` authority/permission/action strings; the app must install **alongside** official Termux [VERIFY G3].
7. After the rename commit, tag `fork-baseline`. Upstream merges are manual and only on explicit request.

## 2. Execution strategy [VERIFY G4]

- Android blocks executing files from an app's writable data directory for apps with a high `targetSdk`. We follow Termux: `targetSdk = 28`. Consequence: Play Store is not a target.
- Document a fallback (not implemented in MVP): ship executables as native libs in `jniLibs`.
- If G4 fails, stop and report; do not continue to M2.

## 3. Public interfaces

```kotlin
package {ROOT_PKG}.feature.terminal.domain

enum class ShellEnvironment { TERMUX_NATIVE, PROOT_UBUNTU }

data class ShellRequest(
    val command: String,                   // run via `bash -lc`
    val cwd: String? = null,               // guest path when PROOT_UBUNTU
    val env: Map<String, String> = emptyMap(),
    val environment: ShellEnvironment = ShellEnvironment.PROOT_UBUNTU,
    val timeoutMs: Long = 120_000,
    val executionId: String,
    val bindExternalStorage: Boolean = false,
)

sealed interface ShellEvent {
    data class Started(val executionId: String) : ShellEvent
    data class Stdout(val chunk: String) : ShellEvent
    data class Stderr(val chunk: String) : ShellEvent
    data class Exited(val code: Int, val durationMs: Long) : ShellEvent
    data object TimedOut : ShellEvent
    data object Cancelled : ShellEvent
    data class Failure(val error: AppError) : ShellEvent
}

data class ShellResult(
    val exitCode: Int, val stdout: String, val stderr: String,
    val durationMs: Long, val truncated: Boolean, val fullLogPath: String?,
)

interface ShellBackend {
    suspend fun isReady(): Boolean
    fun run(request: ShellRequest): Flow<ShellEvent>              // streaming, cold; cancel = collector cancel
    suspend fun runToCompletion(request: ShellRequest): ShellResult
    suspend fun cancel(executionId: String)
    fun openSession(request: SessionRequest): SessionHandle       // interactive / visible terminal session
}

data class SessionRequest(val title: String, val command: String? = null, val environment: ShellEnvironment = ShellEnvironment.PROOT_UBUNTU, val cwd: String? = null)
interface SessionHandle { val id: String; val isRunning: StateFlow<Boolean>; fun write(text: String); fun close() }

interface PathProvider {
    val termuxPrefix: File; val termuxHome: File; val tmpDir: File
    val rootfsDir: File                 // host path of Ubuntu rootfs
    val rootfsHome: File                // host path of guest /root
    val projectsRoot: File              // host: rootfsHome/projects ; guest: /root/projects
    val setupStateDir: File             // host path used by M2 (status.jsonl, done markers)
    val logsDir: File; val buildLogsDir: File
    fun hostToGuest(host: File): String?
    fun guestToHost(guest: String): File
}

sealed interface EnvironmentStatus {
    data object NotInstalled : EnvironmentStatus
    data class Extracting(val progress: Float) : EnvironmentStatus
    data object BootstrapReady : EnvironmentStatus           // Termux bootstrap OK, rootfs missing
    data object Ready : EnvironmentStatus                    // bootstrap + rootfs present
    data class Failed(val error: AppError) : EnvironmentStatus
}
interface BootstrapGate { val status: StateFlow<EnvironmentStatus>; suspend fun ensureBootstrap(): AppResult<Unit> }
```

## 4. Components

- **`EmbeddedShellBackend`** — implements `ShellBackend` on the forked `TermuxService` (bound service via `TermuxServiceConnection` exposing `StateFlow<TermuxService?>`). Non-interactive commands use Termux's background execution path (`AppShell`/`ExecutionCommand`) so stdout and stderr are separate and streamed; `openSession` creates a real terminal session.
- **`ProotCommandBuilder`** — wraps a command for `PROOT_UBUNTU`: raw `proot` with `-r <rootfs>`, `-w <cwd>`, explicit binds (`/dev`, `/proc`, `/sys`, a tmp dir), scrubbed env (`HOME=/root`, `PATH`, `LANG`, `JAVA_HOME`, `ANDROID_HOME`, `TMPDIR`), then `bash -lc '<command>'`. **Never bind shared storage** unless `bindExternalStorage = true`. The rootfs is installed once with `proot-distro install ubuntu` [VERIFY; document distro/version in `docs/UPSTREAM.md`].
- **`ExecutionRegistry`** — tracks running executions, global concurrency limit (default 2), timeouts, cancel = SIGTERM, SIGKILL after 5 s.
- **`OutputBuffer`** — in-memory cap 1 MB per execution; full output spills to `logsDir/exec/<id>.log`; `ShellResult.truncated` + `fullLogPath` set accordingly.
- **`FakeShellBackend`** — scripted responses for unit tests and for developing M5/M6 without a device toolchain.
- **`ExternalTermuxBackend`** — **debug flavor only**, uses the `RUN_COMMAND` intent of official Termux (needs `allow-external-apps=true`). Not compiled into release. Exists only to let agent work start before the fork is stable.
- **`EnvironmentProbe`** — runs `java -version`, `which aapt2`, etc. in the rootfs and returns structured results for M2 health checks.
- **`BootstrapGate`** — wraps Termux's installer: extraction progress, low-storage check, cleanup + retry on interruption. The Splash screen (M2) awaits it.
- **Terminal UI** (`feature/terminal`): `TerminalScreen` hosts `TerminalView` in `AndroidView`, attaches to a `TerminalSession` from the service; `SessionTabs` (create, close, rename); `ExtraKeysBar` reusing Termux's extra-keys view; pinch to zoom font (persisted in `AppPreferences`); copy/paste; optional keep-screen-on; session list survives screen rotation and navigation.
- **Foreground notification/wake lock** — owned by the service; wake lock only while at least one execution is running.

## 5. Functional requirements

- M1-FR-01 App installs next to official Termux and launches.
- M1-FR-02 First launch extracts the bootstrap with progress; interrupted extraction recovers.
- M1-FR-03 `proot` can create/enter the Ubuntu rootfs; `PathProvider` paths resolve; `hostToGuest/guestToHost` round-trip.
- M1-FR-04 `run()` streams stdout/stderr separately and in order; `Exited` always arrives last; cancel and timeout terminate the process tree.
- M1-FR-05 Output over 1 MB truncates in memory and keeps the full log on disk.
- M1-FR-06 `TerminalScreen` supports ≥ 3 concurrent sessions and survives rotation.
- M1-FR-07 Commands never see app secrets in their environment.
- M1-FR-08 `EnvironmentProbe` returns a result per check; failures never throw.

## 6. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/
├─ termux/                      forked Java: TermuxService.java · TermuxInstaller.java · … (only package/constants edited)
└─ feature/terminal/
   ├─ domain/   ShellBackend.kt · ShellRequest.kt · ShellEvent.kt · ShellResult.kt · SessionRequest.kt · SessionHandle.kt
   │            PathProvider.kt · EnvironmentStatus.kt · BootstrapGate.kt · EnvironmentProbe.kt · ProbeResult.kt
   ├─ data/     EmbeddedShellBackend.kt · TermuxServiceConnection.kt · ProotCommandBuilder.kt · ExecutionRegistry.kt
   │            OutputBuffer.kt · DefaultPathProvider.kt · TermuxBootstrapGate.kt · DefaultEnvironmentProbe.kt · FakeShellBackend.kt
   ├─ di/       TerminalModule.kt
   └─ presentation/
      ├─ TerminalRoute.kt · TerminalScreen.kt · TerminalViewModel.kt · TerminalUiState.kt · TerminalEvent.kt
      ├─ components/ TerminalViewHost.kt · SessionTabs.kt · ExtraKeysBar.kt · TerminalToolbar.kt
app/src/debug/java/{ROOT_PKG}/feature/terminal/data/ExternalTermuxBackend.kt
/tools/termux-generator/  config files · /.github/workflows/termux-bootstrap.yml
```

## 7. Risks / edge cases

- **Phantom process killer** (Android 12+) can kill child processes during long builds [VERIFY G5]. Surface guidance in Environment Health (M2/M7); do not try to hide it.
- Low storage during extraction: abort cleanly with a clear message.
- Termux notification/permission differences on Android 13+ (notification runtime permission): request at first run (M7 permissions screen).
- Rootfs corruption: `EnvironmentStatus.Failed` + "Repair" action (re-extract) in M2.

## 8. Tests

- Unit: `ProotCommandBuilder` (quoting, binds, env scrub), `OutputBuffer` spill, `ExecutionRegistry` timeout/cancel, `PathProvider` mapping.
- Instrumented (device): echo, long-running + cancel, large output, stderr ordering, 3 terminal sessions.

## 9. Acceptance criteria

- [ ] App installs alongside official Termux; first launch reaches `BootstrapReady`.
- [ ] `proot` Ubuntu login works; `java` can be installed manually in the terminal.
- [ ] `run("echo hi && echo err 1>&2")` yields Stdout then Stderr events then `Exited(0)`.
- [ ] A 3-minute `sleep` is cancelled and leaves no child process.
- [ ] 5 MB output: memory cap respected, full log on disk.
- [ ] `FakeShellBackend` is used by at least one passing unit test.
- [ ] Release build contains no `ExternalTermuxBackend`.
