# 09 — Later Modules & Roadmap

**Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md · These are **not** MVP. Build only the hooks listed under "MVP hook"; implement the rest after M7 ships.

---

## 1. Later modules

| ID | Module | Scope | MVP hook already present | Gate |
|---|---|---|---|---|
| L1 | **MCP client** | Remote MCP servers over HTTP/SSE first; local stdio servers launched inside the rootfs later. Tool discovery, per-server enable, per-tool permission levels reusing M6 `PermissionGateway`. Config screen + `SecretStore` for tokens. | `ToolRegistry` (M5) accepts tools from multiple sources; `AgentTool` interface | — |
| L2 | **JNI / NDK plugin** | Optional tools `ndk-clang`, `cmake-ninja`; `NativeBuildStep` compiles C/C++ to `lib<name>.so` for `arm64-v8a`; template `hello-jni`; packages `lib/arm64-v8a/*.so` into the APK before align/sign; agent rules updated. | `NativeBuildStep` interface (M4), disabled `hello-jni` template (M3), optional tools in manifest (M2) | **G6** |
| L3 | **Kotlin / Gradle builds** | Gradle in the rootfs; ARM64 `aapt2` via the AGP override property (`android.aapt2FromMavenOverride`); offline dependency cache; Kotlin and Compose templates; Gradle daemon memory tuning. | `gradle` optional tool (M2), `BuildStep` abstraction (M4) | PoC for Gradle on-device |
| L4 | **Attachments & artifacts** | Image/file attachments in the composer; HTML/markdown artifact preview in a sandboxed WebView; chart rendering. | `ContentBlock` is extensible (`Image`), `ToolCallCardRegistry` (M5/M6) | — |
| L5 | **Plugins / skills** | Skill folders `~/.agent/skills/<name>/SKILL.md` discovered and injected into the system prompt on demand; install from git URL. | `SystemPromptBuilder` sections (M6) | — |
| L6 | **Voice input** | Speech-to-text into the composer. | `Composer` slot (M5) | — |
| L7 | **Context summarization** | LLM-based summarization of old turns when the context nears the limit. | `ContextManager` (M6) | — |
| L8 | **More providers** | Gemini and others via new `LlmProvider` adapters; provider-native token counting. | `LlmProvider` (M5) | — |
| L9 | **Remote tool manifest** | Signed remote `tool_manifest.json` for tool updates without an app update. | `ToolManifestParser` + `schemaVersion` (M2) | — |
| L10 | **App lock** | Biometric/device-credential lock for the whole app. | `BiometricGate` (M7) | — |
| L11 | **Incremental builds** | Hash-based step skipping. | `BuildStep` inputs/outputs declared per step (M4) | — |
| L12 | **Dependency resolver** | AAR/Maven resolution without Gradle. | `libs/` classpath handling (M4) | — |
| L13 | **Task plan tool** | Visible todo list for long runs. | `AgentEvent` (M6) | — |
| L14 | **Chat search** | Full-text search across conversations (Room FTS). | `MessageEntity` (M5) | — |

## 2. Milestones

| Milestone | Modules | Exit criteria |
|---|---|---|
| **A — Foundation & Chat** | PoC-0 (parallel), M0, M5 | Chat streams with two providers; keys secured |
| **B — Environment** | M1, M2 | Fresh install → required tools installed → smoke test hook ready; embedded terminal works |
| **C — Projects & Build** | M3, M4 | `hello-java` builds, installs and launches from the app; export works |
| **D — Agent** | M6 | End-to-end agent run builds an app; permissions, stop, resume work |
| **E — Hardening & Release** | M7 | Release checklist passes on a clean device |
| **F — Expansion** | L2, L1, L3, then others | Per-module acceptance written when scheduled |

## 3. Risks register

| Risk | Impact | Mitigation |
|---|---|---|
| ARM64 `aapt2`/build-tools unavailable or unreliable (G1) | Blocks M4 | Evaluate sources in PoC-0; manifest `source.kind` abstracts the origin |
| Executing binaries from the private dir under chosen targetSdk (G4) | Blocks M1 | PoC-0; documented `jniLibs` fallback |
| Custom-package Termux bootstrap build fragile (G3) | Delays M1 | Pin upstream tag; CI-built, cached bootstrap; `fork-baseline` tag |
| Phantom process killer kills long builds (G5) | Flaky builds | Foreground services, wake locks, guidance in Environment Health |
| Large storage footprint | Install failures | Pre-install space check, size estimates, Tool Manager cleanup |
| GPL obligations from Termux code | Licensing | GPL-compatible release with source link (M7 licenses screen) |
| LLM tool misuse (destructive shell commands) | Data loss | Permission prompts, risk classifier, deny list, git checkpoints, project sandbox |
| Provider API drift | Broken streaming | Fixture-based parser tests; `docs/PROVIDERS.md` with doc version/date |
| Play Store not an option (targetSdk 28) | Distribution | GitHub Releases / F-Droid from day one |

## 4. Open decisions (owner: product)

1. Final app name and `{ROOT_PKG}`.
2. Ubuntu release for the rootfs (document in `docs/UPSTREAM.md` after G3).
3. License choice (GPL-compatible).
4. Whether to host a remote tool manifest (L9) at all.
