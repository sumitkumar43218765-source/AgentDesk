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
