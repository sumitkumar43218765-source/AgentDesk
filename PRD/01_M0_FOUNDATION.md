# M0 — Foundation

**Depends on:** nothing · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** project skeleton every other module builds on: build config, DI, theme, navigation shell, MVI base, error model, logging, Room/DataStore setup, secret storage.

---

## 1. Deliverables

1. Gradle setup: version catalog, KSP, Hilt, Room 2.x (schema export on), Compose BOM, kotlinx.serialization, OkHttp, DataStore.
2. `AgentApplication` (`@HiltAndroidApp`), single-activity `MainActivity`.
3. Material 3 theme (light/dark/system, optional dynamic color), typography with a monospace role, spacing tokens.
4. Navigation shell: `AppNavHost` + `Routes` (type-safe) + `NavGraphRegistrar` so each feature registers its own routes.
5. MVI base: `UiState`, `UiEvent`, `UiEffect`, `MviViewModel`.
6. `AppResult<T>`, `AppError`, `DispatcherProvider`, `Clock`, `IdGenerator`, `StringProvider`.
7. `AppLogger` + file logger with rotation + `LogRedactor`.
8. `AppDatabase` (empty entity list now, migration scaffolding), `AppPreferences` (DataStore).
9. `SecretStore` interface + `KeystoreSecretStore` (AES-GCM, key in Android Keystore). Avoid deprecated security-crypto wrappers [VERIFY].
10. Shared UI components.

## 2. Public interfaces

```kotlin
package {ROOT_PKG}.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String
    data class Network(override val message: String, val cause: Throwable? = null) : AppError
    data class Auth(override val message: String) : AppError
    data class RateLimit(override val message: String, val retryAfterMs: Long?) : AppError
    data class Storage(override val message: String, val cause: Throwable? = null) : AppError
    data class Shell(override val message: String, val exitCode: Int? = null) : AppError
    data class Build(override val message: String) : AppError
    data class Permission(override val message: String) : AppError
    data class Cancelled(override val message: String = "Cancelled") : AppError
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError
}

interface DispatcherProvider { val main: CoroutineDispatcher; val io: CoroutineDispatcher; val default: CoroutineDispatcher }
interface Clock { fun nowMillis(): Long }
interface IdGenerator { fun newId(): String }   // UUID v4

package {ROOT_PKG}.core.security
interface SecretStore {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun contains(key: String): Boolean
}

package {ROOT_PKG}.core.mvi
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initial: S) : ViewModel() {
    val state: StateFlow<S>
    val effects: Flow<F>              // one-shot, Channel-backed
    abstract fun onEvent(event: E)
    protected fun setState(reducer: S.() -> S)
    protected fun emitEffect(effect: F)
}
```

`AppPreferences` exposes typed `Flow`s: `themeMode`, `dynamicColor`, `terminalFontSize`, `wifiOnlyDownloads`, `lastOnboardingVersion`, `defaultProviderId`, `defaultAgentMode` (more keys are added by later modules through `PreferenceKeys`).

## 3. Functional requirements

- M0-FR-01 App launches to a placeholder `HomeRoute` showing the theme and nav working in light/dark.
- M0-FR-02 Features contribute routes by implementing `NavGraphRegistrar` (Hilt multibinding `Set<NavGraphRegistrar>`); `AppNavHost` collects them.
- M0-FR-03 `MviViewModel` guarantees effects are delivered once and not lost during configuration changes.
- M0-FR-04 `KeystoreSecretStore` survives process death; values are not readable from backups (`allowBackup=false`, data extraction rules exclude secrets).
- M0-FR-05 `AppLogger` writes to `logsDir/app.log`, rotates at 2 MB × 5 files, redacts patterns that look like API keys/tokens (`sk-…`, `Bearer …`, long base64).
- M0-FR-06 Room schema export is enabled; `exportSchema = true`, schema dir committed.
- M0-FR-07 Shared components handle `UiState` variants: `Loading`, `Empty`, `Error(retry)`.
- M0-FR-08 Strings are externalized; no hardcoded user-visible text.

## 4. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/
├─ app/
│  ├─ AgentApplication.kt
│  ├─ MainActivity.kt
│  └─ navigation/ AppNavHost.kt · Routes.kt · NavGraphRegistrar.kt · HomeRoute.kt
├─ core/
│  ├─ common/   AppResult.kt · AppError.kt · DispatcherProvider.kt · Clock.kt · IdGenerator.kt · StringProvider.kt
│  ├─ logging/  AppLogger.kt · FileAppLogger.kt · LogRedactor.kt · LogRotation.kt
│  ├─ mvi/      UiState.kt · UiEvent.kt · UiEffect.kt · MviViewModel.kt
│  ├─ database/ AppDatabase.kt · Converters.kt
│  ├─ datastore/ AppPreferences.kt · PreferenceKeys.kt
│  ├─ security/ SecretStore.kt · KeystoreSecretStore.kt · CryptoBox.kt
│  ├─ di/       CoreModule.kt · DispatcherModule.kt · DatabaseModule.kt · DataStoreModule.kt · SecurityModule.kt · LoggingModule.kt
│  └─ ui/
│     ├─ theme/       Color.kt · Theme.kt · Type.kt · Shape.kt · Spacing.kt
│     └─ components/  AppScaffold.kt · AppTopBar.kt · LoadingState.kt · ErrorState.kt · EmptyState.kt
│                     ConfirmDialog.kt · SectionHeader.kt · MonoText.kt · StatusChip.kt · ProgressRow.kt · SettingsRow.kt
app/src/main/res/ values/strings.xml · xml/data_extraction_rules.xml · xml/backup_rules.xml
gradle/libs.versions.toml
```

## 5. Edge cases

- Keystore key invalidated (device lock change): `SecretStore.get` returns `null`, logs a redacted warning; UI later prompts re-entry of keys.
- Dynamic color unavailable (< API 31): fall back to static palette.
- Log file not writable: fall back to in-memory ring buffer, never crash.

## 6. Tests

- Unit: `LogRedactor` (keys, bearer tokens), `LogRotation`, `AppResult` helpers, `MviViewModel` effect delivery.
- Instrumented: `KeystoreSecretStore` put/get/remove round-trip and persistence across process restart.

## 7. Acceptance criteria

- [ ] `./gradlew assembleDebug` and unit tests pass.
- [ ] App shows themed placeholder in light and dark mode; route registered through `NavGraphRegistrar`.
- [ ] Secret stored, app process killed, secret still readable; not present in app backups.
- [ ] Logger rotates and redacts a fake `sk-` key in tests.
- [ ] No KAPT in the build; Room version is 2.x.
