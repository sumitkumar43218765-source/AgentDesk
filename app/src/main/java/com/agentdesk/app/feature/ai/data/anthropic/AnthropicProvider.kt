package com.agentdesk.app.feature.ai.data.anthropic

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
import javax.inject.Inject

class AnthropicProvider @Inject constructor(
    private val config: ProviderConfig,
    private val apiKey: String,
    private val sseClient: SseClient,
    private val mapper: AnthropicMapper,
    private val parser: AnthropicEventParser,
    private val errorMapper: ErrorMapper,
    private val httpClientFactory: HttpClientFactory
) : LlmProvider {

    override val type: ProviderType = ProviderType.ANTHROPIC

    override suspend fun listModels(): AppResult<List<ModelInfo>> {
        return AppResult.Success(
            listOf(
                ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", 200000, true),
                ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", 200000, true),
                ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", 200000, true)
            )
        )
    }

    override fun stream(request: LlmRequest): Flow<LlmEvent> {
        val jsonPayload = mapper.toRequestBodyJson(request)
        val baseUrl = config.baseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/messages" else "$baseUrl/v1/messages"

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
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
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/messages" else "$baseUrl/v1/messages"

        val minimalPayload = """
            {
                "model": "${config.defaultModel.ifBlank { "claude-3-5-haiku-20241022" }}",
                "max_tokens": 1,
                "messages": [{"role": "user", "content": "hi"}]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(minimalPayload.toRequestBody("application/json".toMediaType()))
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
            AppResult.Failure(AppError.Network(e.message ?: "Connection failed", e))
        }
    }
}
