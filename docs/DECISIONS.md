# Architecture & Technical Decisions

## D001: App Name and Root Package Selection
- **Decision:** Working title set to `AgentDesk`. Root package set to `com.agentdesk.app`.
- **Reason:** Clean, matches product vision of a mobile mini Claude Desktop for Android with autonomous agent runtime.
- **Date:** 2026-11-20

## D002: Architecture & Stack Compliance
- **Decision:** Feature-based Clean Architecture, MVI (UiState, UiEvent, UiEffect), Hilt DI, Room 2.6.x (KSP only, no KAPT), Jetpack Compose + Material 3, OkHttp + SSE, Keystore-backed SecretStore.
- **Date:** 2026-11-20

## D003: Room 2.x Entity Scaffolding
- **Decision:** Added `AppMetadataEntity` and `AppMetadataDao` to `AppDatabase` for versioning and initial schema generation.
- **Reason:** Room 2.x requires at least one entity in `@Database(entities = [...])` to compile via KSP and generate schema. This metadata table is useful for tracking migrations, schema versions, and installation metadata.
- **Date:** 2026-11-20

## D004: Direct AES-256-GCM Keystore Implementation
- **Decision:** Implemented `CryptoBox` using standard Android Keystore AES-GCM (`KeyGenParameterSpec`, 256-bit AES, GCM NoPadding) storing IV (12 bytes) + ciphertext, rather than the deprecated `androidx.security.crypto.EncryptedSharedPreferences`.
- **Reason:** Avoids known crashes and deprecation issues in Jetpack Security crypto library on various OEM ROMs, while fulfilling `SecretStore` contract and backup exclusion requirements.
- **Date:** 2026-11-20

## D005: Multiplatform kotlinx.serialization + JSON Testing Dependency
- **Decision:** Utilized `kotlinx.serialization.json` across all data models and network DTOs, and added standard `org.json:json:20240303` to `testImplementation` alongside `testOptions.unitTests.isReturnDefaultValues = true`.
- **Reason:** Prevents JVM unit tests from failing due to stubbed Android `org.json` classes while ensuring high-performance, robust JSON streaming parsing.
- **Date:** 2026-11-20

## D007: Manifest-driven Setup and DoneMarker File Truth
- **Decision:** Tool state is cached in Room `ToolInstallStateEntity`, but filesystem markers (`setup/done/<id>@<version>`) combined with actual binary `verify` execution serve as source of truth, reconciled on app launch.
- **Reason:** Complies with PRD M2 architecture; guarantees process deaths or database clearing never cause redundant package re-downloads when binaries are already installed.
- **Date:** 2026-11-20

## D008: Host-side Direct File IO & Guest Proot Storage Parity
- **Decision:** Projects are stored at `PathProvider.projectsRoot/<slug>`, which maps directly to guest proot `/root/projects/<slug>`. The app UI and agent runtime perform direct host filesystem IO while tools and compilers execute commands at the corresponding guest path.
- **Reason:** Enables fast native Linux filesystem semantics (symlinks, executable bits, case sensitivity) required by `git`, `ecj`, `aapt2`, and Android build tools, while avoiding Shared Storage SAF performance overhead.
- **Date:** 2026-11-20

## D009: DocumentBuilderFactory DOM + Regex Fallback for ManifestReader
- **Decision:** Used standard `DocumentBuilderFactory` with XML namespace handling and secondary regex attribute matching rather than Android-only `XmlPullParser`.
- **Reason:** Guarantees reliable XML parsing across both standard JVM unit tests and Android runtime without requiring mocked pull parser dependencies.
- **Date:** 2026-11-21

## D010: Keystore Secrets Persistence with Keystore AES-GCM CryptoBox
- **Decision:** Release keystore passwords and aliases are encrypted using `CryptoBox` (Android Keystore AES-256-GCM) and persisted in private preferences; passwords are scrubbed from all build logs and terminal chunks.
- **Reason:** Prevents credential leaks in history logs, memory dumps, or plain storage while fulfilling security requirements of M4.
- **Date:** 2026-11-21


## D011: Permission Gateway Granular Scoping and Risk Gating
- **Decision:** Implemented `DefaultPermissionGateway` supporting `AUTO`, `ASK_ONCE` (project scope), `ASK_ALWAYS`, and `DENIED`. Added `Auto-accept edits` mode for code modification (`write_file`, `edit_file`) while preserving approval prompts for high-risk commands and shell executions. Commands classified as `HIGH` risk by `CommandRiskClassifier` always prompt even when project-level grants exist.
- **Reason:** Balances agent autonomy during multi-step coding iterations with robust security against destructive commands, network escapes, or accidental deletions.
- **Date:** 2026-11-21

## D012: Loop Guard Runaway Circuit Breaker
- **Decision:** Implemented `LoopGuard` tracking tool name and argument tuples: 3 identical invocations yield an LLM warning; 5 identical invocations or 5 consecutive execution errors abort the run immediately.
- **Reason:** Prevents infinite LLM hallucination loops and runaway token drain.
- **Date:** 2026-11-21

## D013: AgentToolRegistry Interface Separation
- **Decision:** Extracted `AgentToolRegistry` interface extending `ToolRegistry`, allowing `DefaultAgentLoop` to depend solely on interfaces rather than concrete classes, facilitating pure-Kotlin fake testing doubles without Mockito or Robolectric.
- **Reason:** Enhances modularity and speeds up JVM unit test suite.
- **Date:** 2026-11-21

## D014: Settings, Notification Channels, and Defense-in-Depth Diagnostics Redaction
- **Decision:** Implemented unified Settings screen with 10 PRD-defined sections, 6 dedicated Android Notification Channels (`service`, `setup`, `build`, `agent`, `approval`, `backup`), typed confirmation for `WipeWorkspaceUseCase` ("WIPE WORKSPACE"), and defense-in-depth redaction in both `DiagnosticsCollector` and `DiagnosticsZipWriter` to ensure no API keys or sensitive credentials leak into export bundles.
- **Reason:** Guarantees user privacy, strict security against accidental project destruction, seamless background action responses, and compliance with Android security guidelines.
- **Date:** 2026-11-21

## D015: Real Termux Native Terminal Integration (PTY & TerminalView)
- **Decision:** Integrated official Termux core AARs (`terminal-view`, `terminal-emulator`, `termux-shared` v0.118.0) via JitPack with arm64 native `libtermux.so` packaged into the application. Replaced the initial text-based mock with a real interactive `TerminalView` hosted in Compose via `AndroidView`, multi-session tab lifecycle managed by `TermuxService`, PTY subprocess creation via `TerminalSession`, and dynamic `ExtraKeysBar` with visual modifier toggling (CTRL, ALT, arrows, ESC, TAB).
- **Reason:** Fulfills PRD M1 requirement for genuine terminal emulation, ANSI escape color rendering, interactive CLI tools (nano, vim, bash, top), and direct keyboard input instead of simulated text entry.
- **Date:** 2026-11-21

## D016: Authentic Termux UI & Full Shell Integration
- **Decision:** Implemented the full authentic Termux user interface matching upstream Termux specifications:
  1. Left-sliding `TermuxDrawer` with running session list, visual indicators, rename dialog, close buttons, `+ NEW SESSION`, `FAILSAFE` recovery session (executing `/system/bin/sh` with safe recovery environment), and a toggle button for soft keyboard visibility.
  2. Authentic 2-row `ExtraKeysBar` (ESC, /, -, HOME, UP, END, PGUP / TAB, CTRL, ALT, LEFT, DOWN, RIGHT, PGDN) with ANSI escape sequences and sticky modifier highlights.
  3. Termux Context Menu dialog triggered on long-press with Select URL, Share Transcript, Reset Terminal, Kill Process, and Keep Screen On toggle.
  4. Real `TermuxInstaller` handling ZIP extraction, `SYMLINKS.txt` parsing (`oldpath←newpath`), and 0700 binary permissions.
- **Reason:** Fully satisfies user requirement for genuine Termux terminal and UI experience in AgentDesk.
- **Date:** 2026-11-22


