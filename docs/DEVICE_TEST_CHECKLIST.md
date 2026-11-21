# Physical Device Test Checklist

Certain tests require a physical ARM64 Android device (with targetSdk 28 binary execution behavior, Keystore hardware-backed biometric/lock change behavior, terminal PTY, etc.) that cannot be executed in headless Linux Codespaces.

| ID | Module | Test Scenario | Exact Verification Steps | Status |
|---|---|---|---|---|
| DEV-001 | M0 | Keystore Persistence across Process Death | 1. Put key in SecretStore.<br>2. Kill app process with `am force-stop`.<br>3. Relaunch app, verify `SecretStore.get` retrieves secret. | Pending Device |
| DEV-002 | M0 | Backup Exclusion Verification | 1. Run `bmgr backupnow com.agentdesk.app`.<br>2. Inspect backup archive, verify `secrets.pref` / keystore data excluded. | Pending Device |
| DEV-003 | M0 | Dynamic Color Palette Fallback | 1. Launch on Android < 12 and >= 12.<br>2. Verify static theme fallback on < 12 and dynamic colors on >= 12. | Pending Device |
| DEV-004 | M5 | Real Anthropic API Connection | 1. Enter valid Anthropic API key in Provider Edit screen.<br>2. Tap 'Test Connection', verify success toast.<br>3. Send 'Hello', verify streamed response. | Pending Device |
| DEV-005 | M5 | Real OpenAI/OpenRouter Connection | 1. Enter valid OpenAI/OpenRouter API key and Base URL.<br>2. Tap 'Test Connection', verify success.<br>3. Send 'Hello', verify response. | Pending Device |
| DEV-006 | M5 | Mid-stream Process Death Recovery | 1. Send long prompt in chat.<br>2. While streaming, kill app (`am force-stop`).<br>3. Relaunch, verify message shows INTERRUPTED status and 'Continue' button. | Pending Device |
| DEV-007 | M1 | PTY Terminal Rendering & Key Injection | 1. Open Terminal screen.<br>2. Tap Extra Keys (`Ctrl`, `ESC`, arrow keys).<br>3. Verify terminal scrolls, responds to text input and bell. | Pending Device |
| DEV-008 | M2 | Setup Wizard First Run & Proot Ubuntu Install | 1. Clear app data.<br>2. Launch app, verify Splash navigates to Tool Setup.<br>3. Tap Done, verify SetupInstallService runs, terminal displays live package installations, auto-routes to Chat upon completion. | Pending Device |
| DEV-009 | M2 | Mid-install Process Death & Resume | 1. Start tool install.<br>2. Force-stop app during JDK installation.<br>3. Relaunch app, verify already finished tools are skipped via DoneMarkers and remaining tools resume. | Pending Device |
| DEV-010 | M3 | Project Template Creation & File Tree Navigation | 1. Create `hello-java` project.<br>2. Open project detail and browse file tree.<br>3. Open `MainActivity.java` in file viewer, verify syntax highlighting and line numbers. | Pending Device |
| DEV-011 | M3 | SAF Project Export & Import | 1. Export project as zip.<br>2. Verify zip in Downloads folder.<br>3. Import zip, verify project restored with git history intact. | Pending Device |
| DEV-012 | M4 | Hello World On-Device Compilation & APK Sign | 1. Open `hello-java` project.<br>2. Tap 'Build APK' (Debug variant).<br>3. Verify all 7 steps (Resource Compile -> Sign) complete successfully.<br>4. Verify signed `build/app.apk` is generated and verified with `apksigner`. | Pending Device |
| DEV-013 | M4 | PackageInstaller On-Device APK Install | 1. After successful build, tap 'Install APK'.<br>2. If prompted, grant 'Install unknown apps' permission in Settings.<br>3. Verify system install dialog appears and package installs cleanly. | Pending Device |
| DEV-014 | M4 | MediaStore APK Export to Downloads | 1. Tap 'Export' on a successful build.<br>2. Open Files app, navigate to `Downloads/AgentDesk/`.<br>3. Verify exported APK named `<slug>-1.0-<timestamp>.apk` is present and valid. | Pending Device |
| DEV-015 | M4 | Cancel Build Stops Child Process Tree within 5s | 1. Trigger build on a project with multiple source files.<br>2. Tap 'Cancel Build' during Java Compile or Dex step.<br>3. Verify notification disappears, build status changes to CANCELLED, and child proot processes terminate within 5 seconds. | Pending Device |
| DEV-016 | M4 | 5-Minute Build Survives Phantom Process Killer (G5) | 1. Run a heavy multi-step compilation build.<br>2. Switch app to background for > 5 minutes.<br>3. Verify BuildService foreground notification remains active with wake lock and build completes without phantom killer termination. | Pending Device |
