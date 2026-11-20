# M7 — Settings, Notifications, Permissions & Hardening

**Depends on:** all modules · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** one coherent Settings area, notification channels, Android permission management, crash/diagnostics, CI, and release readiness.

---

## 1. Settings structure

| Section | Contents |
|---|---|
| **Providers** | Link to M5 provider list; default provider/model |
| **Agent** | Default run mode (Ask / Auto-accept edits); max steps; shell timeout; allow network in shell (default on); manage project permission grants (list/revoke); run token budget |
| **Environment** | Tool Manager and Environment Health (M2); storage usage; apt mirror; "Download tools on Wi-Fi only" (default on); Repair environment |
| **Projects** | Git author name/email; auto-checkpoint after successful build (default on); backup reminders on/off |
| **Terminal** | Font size, keep-screen-on, extra keys layout |
| **Appearance** | Theme (system/light/dark), dynamic color |
| **Notifications** | Per-channel toggles linking to system channel settings |
| **Permissions** | Android permission status + fix-it links (§3) |
| **Data & privacy** | Export diagnostics (redacted), clear chats, clear build logs, **Wipe workspace** (typed confirmation), reveal-API-key gate |
| **About** | Version/build, GPL notice + source link, third-party licenses, upstream Termux tag |

All values are stored in `AppPreferences` (M0) or the relevant Room table. Settings changes apply immediately (no restart).

## 2. Notification channels

| Channel id | Importance | Used for |
|---|---|---|
| `service` | Low | Foreground service ongoing notifications (setup, build, agent) |
| `setup` | Default | Install finished/failed |
| `build` | Default | Build finished/failed (actions: Install, Open) |
| `agent` | Default | Run finished, run interrupted (action: Continue) |
| `approval` | **High** | Agent waiting for permission (actions: Allow once, Deny) |
| `backup` | Low | Backup reminders |

`NotificationChannels` registers all channels at app start; `NotificationFactory` helpers are shared by M2/M4/M6 (they call into this module's interface, defined in M0 `core` as `AppNotifier`, implemented here).

## 3. Permissions manager

Screen listing each item with state (granted / not granted / not needed) and a deep link:
- Notifications (Android 13+ runtime permission).
- Install unknown apps (for `PackageInstaller`).
- Battery optimization exemption (recommended for long builds).
- All-files access (optional, only if the user enables external folder access in a future feature; hidden in MVP unless needed).

First-run: request notification permission after onboarding completes, not before.

## 4. Security & privacy hardening

- `KeystoreSecretStore` keys invalidation handling surfaces a "re-enter API keys" prompt.
- Optional biometric/device-credential gate before revealing or replacing an API key (`BiometricGate`); app-wide lock is *Later*.
- `allowBackup=false`; data extraction rules exclude databases, secrets, rootfs.
- Release build: R8 enabled with keep rules for Termux JNI/reflection [VERIFY by running a release build on device]; no debug-only classes (`ExternalTermuxBackend`).
- Network security config: cleartext disabled except for user-configured local/LAN provider URLs (explicit opt-in per provider).
- Crash handler writes a redacted crash report to `logsDir`; "Share crash report" on next launch.
- Debug builds enable `StrictMode`.
- Dependency licenses collected at build time for the licenses screen.

## 5. CI / release

- Workflows: `build-debug.yml` (PR: assembleDebug + unit tests + lint), `build-release.yml` (tag: signed release APK, checksums, GitHub Release), `termux-bootstrap.yml` (M1).
- Release signing key stored as CI secrets; never in the repo.
- Versioning: `versionName` semver, `versionCode` from CI run number.
- Release checklist (`docs/RELEASE_CHECKLIST.md`): fresh install on a clean device, full onboarding, smoke test, one agent end-to-end run, upgrade-over-previous-version test, uninstall/clear-data warning verified.

## 6. Accessibility & quality

- Content descriptions for icons; 48 dp touch targets; supports system font scaling up to 200% without clipped primary actions.
- Terminal remains usable with font scaling via its own size setting.
- Strings externalized (ready for translation); RTL not required in MVP.
- Performance budget: chat list stays at 60 fps while streaming; cold start to first screen < 2 s warm / < 4 s cold on a mid-range arm64 device.

## 7. Functional requirements

- M7-FR-01 Every setting in §1 exists, persists, and takes effect immediately.
- M7-FR-02 All notification channels are created; `approval` shows actionable buttons while the app is backgrounded.
- M7-FR-03 Permissions screen reflects live state and deep-links work.
- M7-FR-04 "Wipe workspace" deletes projects, build logs, and chats only after typed confirmation; tools stay installed.
- M7-FR-05 "Export diagnostics" produces a zip with logs and environment info, with secrets redacted (tested with fake keys).
- M7-FR-06 Release APK installs and runs the full flow; no debug-only code is present.
- M7-FR-07 Licenses screen lists third-party libraries and the Termux GPL notice with a source link.

## 8. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/settings/
├─ domain/ model/ SettingsSection.kt · PermissionItem.kt · PermissionState.kt · DiagnosticsBundle.kt
│          AppNotifierImpl.kt (interface AppNotifier lives in core) · NotificationChannels.kt · PermissionChecker.kt
│          usecase/ ExportDiagnosticsUseCase.kt · WipeWorkspaceUseCase.kt · RevokeGrantUseCase.kt · ObservePermissionsUseCase.kt
├─ data/ DiagnosticsCollector.kt · DiagnosticsZipWriter.kt · AndroidPermissionChecker.kt · CrashReporter.kt · LicenseRepository.kt · BiometricGate.kt · di/SettingsModule.kt
└─ presentation/
   ├─ SettingsRoute.kt · SettingsScreen.kt · SettingsViewModel.kt · SettingsUiState.kt
   ├─ agent/ AgentSettingsScreen.kt · GrantsManagerScreen.kt
   ├─ environment/ EnvironmentSettingsScreen.kt
   ├─ projects/ ProjectSettingsScreen.kt
   ├─ terminal/ TerminalSettingsScreen.kt
   ├─ appearance/ AppearanceScreen.kt
   ├─ permissions/ PermissionsScreen.kt · PermissionRow.kt
   ├─ data/ DataPrivacyScreen.kt · WipeWorkspaceDialog.kt
   └─ about/ AboutScreen.kt · LicensesScreen.kt
app/src/main/res/xml/network_security_config.xml · proguard-rules.pro
.github/workflows/build-debug.yml · build-release.yml
docs/RELEASE_CHECKLIST.md
```

## 9. Tests

- Unit: diagnostics redaction, permission state mapping, grant revoke, wipe use case (fake FS).
- UI: settings navigation, wipe confirmation requires exact text.
- Device: notification actions from background, permission deep links, release build smoke run, upgrade test.

## 10. Acceptance criteria

- [ ] All settings persist and apply live.
- [ ] Approval notification with Allow/Deny works with the app in the background.
- [ ] Diagnostics zip contains no API key (verified with a planted fake `sk-…` string).
- [ ] Wipe workspace requires typed confirmation and leaves tools installed.
- [ ] Release APK passes the full release checklist on a clean device.
- [ ] Licenses screen shows third-party libs and the Termux GPL notice.
