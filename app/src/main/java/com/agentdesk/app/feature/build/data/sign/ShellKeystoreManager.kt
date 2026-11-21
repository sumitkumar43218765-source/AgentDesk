package com.agentdesk.app.feature.build.data.sign

import android.content.Context
import android.net.Uri
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.security.CryptoBox
import com.agentdesk.app.feature.build.domain.KeystoreManager
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.SigningConfig
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellKeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellBackend: ShellBackend,
    private val pathProvider: PathProvider,
    private val cryptoBox: CryptoBox
) : KeystoreManager {

    companion object {
        private const val DEBUG_KEYSTORE_GUEST_PATH = "/root/.android/debug.keystore"
        private const val RELEASE_KEYSTORE_GUEST_PATH = "/root/.android/release.keystore"
        private const val PREF_KEY_RELEASE_ALIAS = "release_keystore_alias"
        private const val PREF_KEY_RELEASE_STOREPASS = "release_keystore_storepass"
        private const val PREF_KEY_RELEASE_KEYPASS = "release_keystore_keypass"
    }

    override suspend fun getOrCreateDebugKeystore(): AppResult<SigningConfig> {
        val hostKeystore = pathProvider.guestToHost(DEBUG_KEYSTORE_GUEST_PATH)
        if (hostKeystore.exists() && hostKeystore.length() > 0) {
            return AppResult.Success(
                SigningConfig(
                    keystoreGuestPath = DEBUG_KEYSTORE_GUEST_PATH,
                    storePass = "android",
                    alias = "androiddebugkey",
                    keyPass = "android",
                    isDebug = true
                )
            )
        }

        // Generate debug keystore using keytool in proot
        val command = "mkdir -p /root/.android && " +
                "keytool -genkeypair -v -keystore $DEBUG_KEYSTORE_GUEST_PATH " +
                "-storepass android -alias androiddebugkey -keypass android " +
                "-keyalg RSA -keysize 2048 -validity 10000 -dname \"CN=Android Debug,O=Android,C=US\""

        val request = ShellRequest(
            command = command,
            cwd = "/root",
            environment = ShellEnvironment.PROOT_UBUNTU,
            timeoutMs = 60_000,
            executionId = UUID.randomUUID().toString()
        )

        val result = shellBackend.runToCompletion(request)
        return if (result.exitCode == 0 || hostKeystore.exists()) {
            AppResult.Success(
                SigningConfig(
                    keystoreGuestPath = DEBUG_KEYSTORE_GUEST_PATH,
                    storePass = "android",
                    alias = "androiddebugkey",
                    keyPass = "android",
                    isDebug = true
                )
            )
        } else {
            AppResult.Failure(AppError.Build("Failed to generate debug keystore: ${result.stderr.ifBlank { result.stdout }}"))
        }
    }

    override suspend fun importReleaseKeystore(
        sourceUri: Uri,
        alias: String,
        storePass: String,
        keyPass: String
    ): AppResult<SigningConfig> {
        return try {
            val hostFile = pathProvider.guestToHost(RELEASE_KEYSTORE_GUEST_PATH)
            hostFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(hostFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return AppResult.Failure(AppError.Storage("Unable to open source keystore stream"))

            val prefs = context.getSharedPreferences("keystore_secrets", Context.MODE_PRIVATE)
            prefs.edit()
                .putString(PREF_KEY_RELEASE_ALIAS, cryptoBox.encrypt(alias))
                .putString(PREF_KEY_RELEASE_STOREPASS, cryptoBox.encrypt(storePass))
                .putString(PREF_KEY_RELEASE_KEYPASS, cryptoBox.encrypt(keyPass))
                .apply()

            AppResult.Success(
                SigningConfig(
                    keystoreGuestPath = RELEASE_KEYSTORE_GUEST_PATH,
                    storePass = storePass,
                    alias = alias,
                    keyPass = keyPass,
                    isDebug = false
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage("Failed to import release keystore: ${e.message}"))
        }
    }

    override suspend fun getSigningConfig(variant: BuildVariant): AppResult<SigningConfig> {
        return when (variant) {
            BuildVariant.DEBUG -> getOrCreateDebugKeystore()
            BuildVariant.RELEASE -> {
                val hostFile = pathProvider.guestToHost(RELEASE_KEYSTORE_GUEST_PATH)
                if (!hostFile.exists()) {
                    return AppResult.Failure(AppError.Build("Release keystore not found. Import a release keystore first."))
                }
                val prefs = context.getSharedPreferences("keystore_secrets", Context.MODE_PRIVATE)
                val encAlias = prefs.getString(PREF_KEY_RELEASE_ALIAS, null)
                val encStorePass = prefs.getString(PREF_KEY_RELEASE_STOREPASS, null)
                val encKeyPass = prefs.getString(PREF_KEY_RELEASE_KEYPASS, null)

                if (encAlias == null || encStorePass == null || encKeyPass == null) {
                    return AppResult.Failure(AppError.Build("Release keystore credentials missing."))
                }

                val alias = cryptoBox.decrypt(encAlias)
                val storePass = cryptoBox.decrypt(encStorePass)
                val keyPass = cryptoBox.decrypt(encKeyPass)

                if (alias.isNullOrBlank() || storePass.isNullOrBlank() || keyPass.isNullOrBlank()) {
                    return AppResult.Failure(AppError.Build("Release keystore credentials missing or corrupted."))
                }

                AppResult.Success(
                    SigningConfig(
                        keystoreGuestPath = RELEASE_KEYSTORE_GUEST_PATH,
                        storePass = storePass,
                        alias = alias,
                        keyPass = keyPass,
                        isDebug = false
                    )
                )
            }
        }
    }
}
