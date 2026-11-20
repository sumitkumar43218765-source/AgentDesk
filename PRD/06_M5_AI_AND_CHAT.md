# M5 — AI Providers & Chat

**Depends on:** M0 · **Read first:** 00_OVERVIEW_AND_ARCHITECTURE.md
**Goal:** BYO-key LLM provider layer with streaming + tool-call support, and the chat UI/persistence. This module can be built **before** Termux exists (chat works without tools).

Provider API details (endpoints, event names, field names) change over time. At implementation time read each provider's **current official API docs** and record the version/date in `docs/PROVIDERS.md`. Do not rely on memory.

---

## 1. Provider layer (`feature/ai`)

```kotlin
interface LlmProvider {
    val type: ProviderType
    suspend fun listModels(): AppResult<List<ModelInfo>>           // may return a static list if unsupported
    fun stream(request: LlmRequest): Flow<LlmEvent>                // cold; cancel = collector cancel
    suspend fun testConnection(): AppResult<Unit>
}

data class LlmRequest(
    val model: String, val system: String?, val messages: List<LlmMessage>,
    val tools: List<ToolSpec> = emptyList(), val maxTokens: Int = 4096, val temperature: Double? = null,
)
data class LlmMessage(val role: Role, val blocks: List<ContentBlock>)       // Role: USER | ASSISTANT
sealed interface ContentBlock {
    data class Text(val text: String) : ContentBlock
    data class ToolUse(val id: String, val name: String, val argsJson: String) : ContentBlock
    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : ContentBlock
    // Image(...) = Later
}
data class ToolSpec(val name: String, val description: String, val inputSchemaJson: String)

sealed interface LlmEvent {
    data class TextDelta(val text: String) : LlmEvent
    data class ToolCallStarted(val id: String, val name: String) : LlmEvent
    data class ToolCallArgsDelta(val id: String, val partialJson: String) : LlmEvent
    data class ToolCallCompleted(val id: String, val name: String, val argsJson: String) : LlmEvent
    data class Usage(val inputTokens: Int, val outputTokens: Int) : LlmEvent
    data class Done(val stopReason: StopReason) : LlmEvent          // END_TURN | TOOL_USE | MAX_TOKENS | OTHER
    data class Error(val error: AppError) : LlmEvent
}
```

**Adapters (MVP):**
- `AnthropicProvider` — Messages API with SSE streaming and tool use.
- `OpenAiCompatibleProvider` — Chat Completions style streaming with tool calls; configurable `baseUrl` + headers, so it also covers OpenRouter, Groq, local/self-hosted gateways, etc.
- Gemini and others: *Later* (interface is ready).

**Shared infrastructure**
- `SseClient`: OkHttp SSE → `Flow`; timeouts (connect 15 s, read 120 s idle); cancellation closes the call.
- `RetryPolicy`: 429 and 5xx with exponential backoff + jitter, honor `Retry-After`; max 3 tries; never retry after partial output was already emitted to the UI for the same turn unless the stream failed before any event.
- `ErrorMapper`: HTTP/body → `AppError` (`Auth`, `RateLimit`, `Network`, context-too-long → `AppError.Unknown` with code `CONTEXT_TOO_LONG`, server errors).
- `ToolCallAssembler`: joins partial JSON args into a complete call; validates JSON; invalid JSON → `ToolCallCompleted` with a flagged error the agent can feed back to the model.
- `UsageTracker`: tokens per conversation (no currency in MVP).
- `ProviderRegistry`: builds a provider from a `ProviderConfig`.

**Config & secrets**
`ProviderConfigEntity(id, type, displayName, baseUrl, defaultModel, extraHeadersJson, enabled, createdAt)` in Room. **API key lives only in `SecretStore`** under key `provider.<id>.apiKey`. Keys are never shown in full after saving (masked, "Replace" only).

## 2. Chat (`feature/chat`)

**Entities**
- `ConversationEntity(id, projectId?, title, providerId, model, createdAt, updatedAt, pinned)`
- `MessageEntity(id, conversationId, role, blocksJson, status, usageJson?, createdAt)`; `status`: `COMPLETE | STREAMING | INTERRUPTED | ERROR`.

**Behavior**
- Streaming partial assistant message is shown live and **persisted every ~2 s** as `STREAMING`; after process death it is marked `INTERRUPTED` with a "Continue" action.
- Conversation may be bound to a project (M3) or be general chat (`projectId = null`).
- Title = first user message truncated to 40 chars (LLM-generated titles are *Later*).
- Stop button cancels the stream; partial text is kept.
- Retry last response; edit-and-resend last user message (MVP: resend only).
- `ToolCallCardRenderer` registry: M5 ships a generic JSON card for unknown blocks; M6 registers rich cards.

## 3. Functional requirements

- M5-FR-01 Add/edit/delete providers; **Test connection** shows success or a mapped error.
- M5-FR-02 Model picker: fetched list if supported, plus manual model id entry; selection stored per conversation.
- M5-FR-03 Streaming renders markdown incrementally without jank (debounce rendering to ~30 fps).
- M5-FR-04 Code blocks: language label, horizontal scroll, copy button; inline code; links open in browser; tables and lists render.
- M5-FR-05 Conversation list: search by title, group by project, swipe-to-delete with undo, pin.
- M5-FR-06 Empty states: no provider (CTA to add one), no conversations (suggested prompts).
- M5-FR-07 Error banner with Retry for network/auth/rate-limit; auth error links to the provider edit screen.
- M5-FR-08 Token usage shown per assistant message (small, collapsible).
- M5-FR-09 Auto-scroll follows the stream unless the user scrolls up; "scroll to bottom" FAB.
- M5-FR-10 No message content or keys are written to logs.
- M5-FR-11 Chat works fully with **no** tools registered; the tool list passed to the provider comes from an injected `ToolRegistry` interface (empty until M6).

## 4. File tree (minimum)

```
app/src/main/java/{ROOT_PKG}/feature/ai/
├─ domain/
│  ├─ model/ LlmRequest.kt · LlmMessage.kt · ContentBlock.kt · LlmEvent.kt · ToolSpec.kt · StopReason.kt · ModelInfo.kt · ProviderConfig.kt · ProviderType.kt · TokenUsage.kt
│  ├─ LlmProvider.kt · ProviderRegistry.kt · ProviderConfigRepository.kt · ToolRegistry.kt
│  └─ usecase/ SaveProviderUseCase.kt · DeleteProviderUseCase.kt · TestProviderUseCase.kt · ListModelsUseCase.kt
├─ data/
│  ├─ http/  SseClient.kt · RetryPolicy.kt · ErrorMapper.kt · HttpClientFactory.kt
│  ├─ anthropic/ AnthropicProvider.kt · AnthropicDtos.kt · AnthropicEventParser.kt · AnthropicMapper.kt
│  ├─ openai/    OpenAiCompatibleProvider.kt · OpenAiDtos.kt · OpenAiEventParser.kt · OpenAiMapper.kt
│  ├─ ToolCallAssembler.kt · UsageTracker.kt
│  ├─ local/ ProviderConfigEntity.kt · ProviderConfigDao.kt · RoomProviderConfigRepository.kt
│  └─ di/    AiModule.kt
└─ presentation/ ProviderListRoute.kt · ProviderListScreen.kt · ProviderEditScreen.kt · ProviderEditViewModel.kt · components/ModelPickerSheet.kt · MaskedKeyField.kt

app/src/main/java/{ROOT_PKG}/feature/chat/
├─ domain/
│  ├─ model/ Conversation.kt · ChatMessage.kt · MessageStatus.kt · StreamingState.kt
│  ├─ repository/ ChatRepository.kt
│  ├─ BasePrompts.kt · ToolCallCardRenderer.kt · ToolCallCardRegistry.kt
│  └─ usecase/ SendMessageUseCase.kt · StopStreamingUseCase.kt · RetryLastUseCase.kt · CreateConversationUseCase.kt
│              RenameConversationUseCase.kt · DeleteConversationUseCase.kt · ObserveConversationsUseCase.kt · RecoverInterruptedUseCase.kt
├─ data/ local/ ConversationEntity.kt · MessageEntity.kt · ConversationDao.kt · MessageDao.kt · BlocksJsonConverter.kt · RoomChatRepository.kt · StreamingPersister.kt · di/ChatModule.kt
└─ presentation/
   ├─ list/ ConversationListRoute.kt · ConversationListScreen.kt · ConversationListViewModel.kt
   ├─ chat/ ChatRoute.kt · ChatScreen.kt · ChatViewModel.kt · ChatUiState.kt · ChatEvent.kt · ChatEffect.kt
   └─ components/ MessageList.kt · UserBubble.kt · AssistantMessage.kt · MarkdownText.kt · CodeBlock.kt · Composer.kt
                  ModelChip.kt · ErrorBanner.kt · UsageFootnote.kt · GenericToolCard.kt · ScrollToBottomFab.kt
app/src/main/res/raw/prompt_chat_base.md
```

Wrap the markdown renderer behind `MarkdownText` so the library can be swapped.

## 5. Edge cases

- Provider returns tool calls but no tools were offered → treat as text, log a redacted warning.
- Context too long → show a clear error and suggest starting a new chat (summarization is *Later*).
- Switching provider/model mid-conversation is allowed; blocks stay provider-neutral.
- Device offline when sending → message saved as `ERROR` with Retry.
- Very long single message (> 100 KB) is rejected with a message.
- Rotation / process death during streaming → no duplicated or lost messages.

## 6. Tests

- Unit: SSE event parsers per provider using **recorded fixtures**, `ToolCallAssembler` (split JSON across deltas, invalid JSON), `RetryPolicy`, `ErrorMapper`, mappers to/from provider DTOs, `StreamingPersister`.
- UI: composer enable/disable states, stop button, code-block copy.
- Manual: real calls with a user key for both adapters.

## 7. Acceptance criteria

- [ ] Add an Anthropic provider and an OpenAI-compatible provider with real keys; Test connection passes.
- [ ] Streaming reply renders smoothly; Stop keeps partial text; Retry works.
- [ ] A fixture-driven test shows a tool call split across 5 deltas is assembled correctly.
- [ ] Kill the app mid-stream: message is `INTERRUPTED` after restart with a Continue action.
- [ ] No API key appears in Room, DataStore, logs, or backups.
- [ ] Rate-limit (429) test shows backoff and then a friendly error.
- [ ] Chat works end-to-end with the tool registry empty.
