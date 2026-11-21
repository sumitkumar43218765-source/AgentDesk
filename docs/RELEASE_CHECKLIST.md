# AgentDesk Release Verification Checklist

This checklist documents the required validation steps before publishing a release of AgentDesk.

---

## 1. Pre-Release Build & Static Verification
- [ ] `./gradlew testDebugUnitTest` runs with 100% test pass rate in Codespaces.
- [ ] `./gradlew assembleRelease` builds arm64-v8a APK without errors.
- [ ] Checksum (SHA-256) is computed and stored alongside the APK artifact.
- [ ] Proguard / R8 keep rules preserve Termux native JNI symbols, Room schemas, Hilt components, and Kotlinx serialization metadata.
- [ ] `network_security_config.xml` blocks cleartext HTTP except explicitly whitelisted local/LAN loopback addresses.
- [ ] `allowBackup=false` and data extraction rules exclude private credentials and database files.

---

## 2. Fresh Installation on Clean Device
- [ ] Install `app-release.apk` on a clean Android 8.0+ (API 26+) physical arm64-v8a device.
- [ ] Launch application; verify cold start time to first screen is within performance budget (< 4 seconds).
- [ ] Verify onboarding flow appears on initial launch and successfully resolves environment status.
- [ ] Complete toolchain setup (JDK 17, Android CLI SDK, Termux bootstrap).
- [ ] Verify notification permission prompt is shown only after initial onboarding setup.

---

## 3. End-to-End Agent Smoke Test
- [ ] Create or import an Android project (e.g., standard Empty Activity or sample app).
- [ ] Configure AI Provider (Anthropic, OpenAI, or local endpoint) with valid credentials.
- [ ] Trigger an Agent run: Ask agent to modify a file or implement a feature.
- [ ] Verify approval card prompts for sensitive shell executions and file modifications (or auto-accepts in auto mode).
- [ ] Run `./gradlew assembleDebug` or build APK via Agent Build Tool.
- [ ] Verify build logs are captured and distilled diagnostics highlight errors if any.
- [ ] Verify APK install trigger prompts package installer.

---

## 4. Notifications & Background Execution
- [ ] Start a long-running build or agent task.
- [ ] Put application into background.
- [ ] Verify foreground service notification is shown with active status.
- [ ] When agent requires approval while backgrounded, verify high-priority `approval` notification displays actionable "Allow Once" and "Deny" buttons.
- [ ] Tapping an action correctly resolves the permission gateway without hanging.

---

## 5. Security & Privacy Verification
- [ ] Navigate to **Settings -> Data & Privacy -> Export Diagnostics**.
- [ ] Verify generated zip file contains no unredacted API keys (`sk-...`, `Bearer ...`, or passwords).
- [ ] Navigate to **Danger Zone -> Wipe Workspace**.
- [ ] Verify typed confirmation requires typing exact string `WIPE WORKSPACE`.
- [ ] Confirm wipe: verify all project files, chats, and build logs are removed while installed toolchains remain functional.

---

## 6. Upgrade & Migration Test
- [ ] Install previous release version and create sample projects/chats.
- [ ] Install new release APK over the previous version (`adb install -r app-release.apk`).
- [ ] Verify Room database migrations execute seamlessly without crash or data loss.
- [ ] Verify all preferences and existing toolchains are retained.
