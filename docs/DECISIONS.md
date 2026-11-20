# Architecture & Technical Decisions

## D001: App Name and Root Package Selection
- **Decision:** Working title set to `AgentDesk`. Root package set to `com.agentdesk.app`.
- **Reason:** Clean, matches product vision of a mobile mini Claude Desktop for Android with autonomous agent runtime.
- **Date:** 2026-11-20

## D002: Architecture & Stack Compliance
- **Decision:** Feature-based Clean Architecture, MVI (UiState, UiEvent, UiEffect), Hilt DI, Room 2.6.x (KSP only, no KAPT), Jetpack Compose + Material 3, OkHttp + SSE, Keystore-backed SecretStore.
- **Date:** 2026-11-20
