# LLM Provider API Reference

This document tracks the official API specifications, versions, and verified behaviors for all supported LLM providers in AgentDesk.

## 1. Anthropic (Messages API)
- **API Version Header:** `anthropic-version: 2023-06-01`
- **Base URL:** `https://api.anthropic.com`
- **Endpoint:** `POST /v1/messages`
- **Authentication:** `x-api-key: {apiKey}`
- **SSE Stream Protocol:**
  - `message_start`: contains initial message structure and `usage.input_tokens`.
  - `content_block_start`: announces a new block (index, block type `text` or `tool_use`, id, name).
  - `content_block_delta`: streamed chunks (`text_delta` or `input_json_delta` with partial JSON).
  - `content_block_stop`: ends the current block index.
  - `message_delta`: contains `stop_reason` (`end_turn`, `tool_use`, `max_tokens`) and `usage.output_tokens`.
  - `message_stop`: marks the end of the SSE stream.
  - `error`: standard Anthropic error structure with `type` and `message`.
- **Verified Date:** November 2026

## 2. OpenAI-Compatible API (Chat Completions)
- **Base URL:** Configurable (e.g., `https://api.openai.com/v1`, `https://openrouter.ai/api/v1`, `https://api.groq.com/openai/v1`)
- **Endpoint:** `POST {baseUrl}/chat/completions`
- **Authentication:** `Authorization: Bearer {apiKey}`
- **SSE Stream Protocol:**
  - Payload lines start with `data: {...}` and terminate with `data: [DONE]`.
  - `choices[0].delta`:
    - `content`: string delta for text.
    - `tool_calls`: array of `{index, id, type: "function", function: {name, arguments}}`.
  - `choices[0].finish_reason`: `stop`, `tool_calls`, `length`.
  - `usage`: optionally streamed when `stream_options.include_usage = true`.
- **Model Discovery:** `GET {baseUrl}/models`
- **Verified Date:** November 2026
