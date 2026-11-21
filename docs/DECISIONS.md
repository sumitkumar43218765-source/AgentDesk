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
