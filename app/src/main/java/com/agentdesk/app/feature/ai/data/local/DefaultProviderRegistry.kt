package com.agentdesk.app.feature.ai.data.local

import com.agentdesk.app.feature.ai.data.anthropic.AnthropicEventParser
import com.agentdesk.app.feature.ai.data.anthropic.AnthropicMapper
import com.agentdesk.app.feature.ai.data.anthropic.AnthropicProvider
import com.agentdesk.app.feature.ai.data.http.ErrorMapper
import com.agentdesk.app.feature.ai.data.http.HttpClientFactory
import com.agentdesk.app.feature.ai.data.http.SseClient
import com.agentdesk.app.feature.ai.data.openai.OpenAiCompatibleProvider
import com.agentdesk.app.feature.ai.data.openai.OpenAiEventParser
import com.agentdesk.app.feature.ai.data.openai.OpenAiMapper
import com.agentdesk.app.feature.ai.domain.LlmProvider
import com.agentdesk.app.feature.ai.domain.ProviderConfigRepository
import com.agentdesk.app.feature.ai.domain.ProviderRegistry
import com.agentdesk.app.feature.ai.domain.model.ProviderConfig
import com.agentdesk.app.feature.ai.domain.model.ProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProviderRegistry @Inject constructor(
    private val repository: ProviderConfigRepository,
    private val sseClient: SseClient,
    private val anthropicMapper: AnthropicMapper,
    private val anthropicParser: AnthropicEventParser,
    private val openAiMapper: OpenAiMapper,
    private val openAiParser: OpenAiEventParser,
    private val errorMapper: ErrorMapper,
    private val httpClientFactory: HttpClientFactory
) : ProviderRegistry {

    override suspend fun getProvider(configId: String): LlmProvider? {
        val config = repository.getProvider(configId) ?: return null
        val apiKey = repository.getApiKey(configId).orEmpty()
        return createProvider(config, apiKey)
    }

    override suspend fun createProvider(config: ProviderConfig, apiKey: String): LlmProvider {
        return when (config.type) {
            ProviderType.ANTHROPIC -> AnthropicProvider(
                config = config,
                apiKey = apiKey,
                sseClient = sseClient,
                mapper = anthropicMapper,
                parser = anthropicParser,
                errorMapper = errorMapper,
                httpClientFactory = httpClientFactory
            )
            ProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(
                config = config,
                apiKey = apiKey,
                sseClient = sseClient,
                mapper = openAiMapper,
                parser = openAiParser,
                errorMapper = errorMapper,
                httpClientFactory = httpClientFactory
            )
        }
    }
}
