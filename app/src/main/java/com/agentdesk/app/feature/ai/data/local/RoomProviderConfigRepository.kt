package com.agentdesk.app.feature.ai.data.local

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.core.security.SecretStore
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProviderConfigRepository @Inject constructor(
    private val dao: ProviderConfigDao,
    private val secretStore: SecretStore
) : ProviderConfigRepository {

    override fun observeProviders(): Flow<List<ProviderConfig>> {
        return dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getProvider(id: String): ProviderConfig? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getApiKey(providerId: String): String? {
        return secretStore.get("provider.$providerId.apiKey")
    }

    override suspend fun saveProvider(config: ProviderConfig, apiKey: String?): AppResult<Unit> {
        return try {
            dao.insert(config.toEntity())
            if (!apiKey.isNullOrBlank()) {
                secretStore.put("provider.${config.id}.apiKey", apiKey)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage(e.message ?: "Failed to save provider config", e))
        }
    }

    override suspend fun deleteProvider(id: String): AppResult<Unit> {
        return try {
            dao.deleteById(id)
            secretStore.remove("provider.$id.apiKey")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage(e.message ?: "Failed to delete provider config", e))
        }
    }

    private fun ProviderConfigEntity.toDomain(): ProviderConfig {
        val headers = mutableMapOf<String, String>()
        try {
            val json = JSONObject(extraHeadersJson)
            for (key in json.keys()) {
                headers[key] = json.getString(key)
            }
        } catch (ignored: Exception) {}

        return ProviderConfig(
            id = id,
            type = if (type == "ANTHROPIC") ProviderType.ANTHROPIC else ProviderType.OPENAI_COMPATIBLE,
            displayName = displayName,
            baseUrl = baseUrl,
            defaultModel = defaultModel,
            extraHeaders = headers,
            enabled = enabled,
            createdAt = createdAt
        )
    }

    private fun ProviderConfig.toEntity(): ProviderConfigEntity {
        val headersJson = JSONObject(extraHeaders as Map<*, *>).toString()
        return ProviderConfigEntity(
            id = id,
            type = type.name,
            displayName = displayName,
            baseUrl = baseUrl,
            defaultModel = defaultModel,
            extraHeadersJson = headersJson,
            enabled = enabled,
            createdAt = createdAt
        )
    }
}
