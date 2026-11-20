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
