package com.agentdesk.app.feature.ai.data.openai

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.ai.data.http.ErrorMapper
import com.agentdesk.app.feature.ai.data.http.HttpClientFactory
import com.agentdesk.app.feature.ai.data.http.SseClient
import com.agentdesk.app.feature.ai.domain.LlmProvider
import com.agentdesk.app.feature.ai.domain.model.LlmEvent
import com.agentdesk.app.feature.ai.domain.model.LlmRequest
import com.agentdesk.app.feature.ai.domain.model.ModelInfo
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class OpenAiCompatibleProvider @Inject constructor(
    private val config: ProviderConfig,
    private val apiKey: String,
    private val sseClient: SseClient,
    private val mapper: OpenAiMapper,
    private val parser: OpenAiEventParser,
    private val errorMapper: ErrorMapper,
    private val httpClientFactory: HttpClientFactory
) : LlmProvider {

    override val type: ProviderType = ProviderType.OPENAI_COMPATIBLE

    override suspend fun listModels(): AppResult<List<ModelInfo>> {
        val client = httpClientFactory.createClient()
        val baseUrl = config.baseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/models" else "$baseUrl/v1/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    val models = mutableListOf<ModelInfo>()

                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val id = item.getString("id")
                            models.add(ModelInfo(id = id, name = id))
                        }
                    }

                    if (models.isEmpty() && config.defaultModel.isNotBlank()) {
                        models.add(ModelInfo(id = config.defaultModel, name = config.defaultModel))
                    }

                    AppResult.Success(models)
                } else {
                    val defaultList = if (config.defaultModel.isNotBlank()) {
                        listOf(ModelInfo(config.defaultModel, config.defaultModel))
                    } else {
                        listOf(ModelInfo("gpt-4o", "GPT-4o"), ModelInfo("gpt-4o-mini", "GPT-4o Mini"))
                    }
                    AppResult.Success(defaultList)
                }
            }
        } catch (e: Exception) {
            val defaultList = if (config.defaultModel.isNotBlank()) {
                listOf(ModelInfo(config.defaultModel, config.defaultModel))
            } else {
                listOf(ModelInfo("gpt-4o", "GPT-4o"), ModelInfo("gpt-4o-mini", "GPT-4o Mini"))
            }
            AppResult.Success(defaultList)
        }
    }

    override fun stream(request: LlmRequest): Flow<LlmEvent> {
        val jsonPayload = mapper.toRequestBodyJson(request)
        val baseUrl = config.baseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/chat/completions" else "$baseUrl/v1/chat/completions"

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))

        for ((key, value) in config.extraHeaders) {
            reqBuilder.addHeader(key, value)
        }

        return sseClient.streamEvents(reqBuilder.build())
            .flatMapConcat { sseEvent ->
                flow {
                    for (event in parser.parse(sseEvent)) {
                        emit(event)
                    }
                }
            }
            .catch { throwable ->
                emit(LlmEvent.Error(errorMapper.map(500, throwable.message)))
            }
    }

    override suspend fun testConnection(): AppResult<Unit> {
        val client = httpClientFactory.createClient()
        val baseUrl = config.baseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/models" else "$baseUrl/v1/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    AppResult.Success(Unit)
                } else {
                    val body = resp.body?.string()
                    AppResult.Failure(errorMapper.map(resp.code, body))
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Network(e.message ?: "Connection test failed", e))
        }
    }
}
