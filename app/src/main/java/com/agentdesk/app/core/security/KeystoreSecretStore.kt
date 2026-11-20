package com.agentdesk.app.core.security

import android.content.Context
import android.content.SharedPreferences
import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreSecretStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoBox: CryptoBox,
    private val dispatchers: DispatcherProvider,
    private val logger: AppLogger
) : SecretStore {

    companion object {
        private const val PREFS_NAME = "agentdesk_secrets"
        private const val TAG = "KeystoreSecretStore"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun put(key: String, value: String): Unit = withContext(dispatchers.io) {
        try {
            val encrypted = cryptoBox.encrypt(value)
            prefs.edit().putString(key, encrypted).apply()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to encrypt secret for key: $key", e)
            throw e
        }
    }

    override suspend fun get(key: String): String? = withContext(dispatchers.io) {
        val encrypted = prefs.getString(key, null) ?: return@withContext null
        val decrypted = cryptoBox.decrypt(encrypted)
        if (decrypted == null) {
            logger.w(TAG, "Failed to decrypt secret for key: $key. Key might be invalidated.")
        }
        decrypted
    }

    override suspend fun remove(key: String): Unit = withContext(dispatchers.io) {
        prefs.edit().remove(key).apply()
    }

    override suspend fun contains(key: String): Boolean = withContext(dispatchers.io) {
        prefs.contains(key)
    }
}
