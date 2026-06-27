package com.ampairs.agent.domain.dto

import jakarta.validation.constraints.NotBlank

/**
 * Request body for the cloud chat proxy (`POST /agent/v1/chat/completions`). The app's
 * `AmpairsProxyTransport` (the `"ampairs"` cloud transport) sends this; the backend forwards it to the
 * hosted model with a server-side provider key, so the key never reaches the client. Field names map to
 * snake_case on the wire via the global Jackson strategy (`max_tokens`, `response_format`, etc.).
 *
 * `model` is the provider-side model id the app requested (e.g. `claude-sonnet-4-6`); the proxy
 * validates it against an allow-list and falls back to the configured default if it isn't allowed.
 */
data class ChatCompletionRequest(
    @field:NotBlank val model: String = "",
    val messages: List<ChatCompletionMessage> = emptyList(),
    /** Max output tokens for the completion. Coerced into a safe range server-side. */
    val maxTokens: Int = 1024,
    /** Sampling temperature from the app. Applied only for models that accept it (ignored otherwise). */
    val temperature: Float = 0.3f,
    /** Optional structured-output constraint for the text-to-SQL / intent path. */
    val responseFormat: ChatResponseFormat? = null,
)

/** One chat message. [role] is `system` | `user` | `assistant`. */
data class ChatCompletionMessage(
    val role: String = "user",
    val content: String = "",
)

/**
 * Structured-output request. [schema] is a JSON-schema document serialized as a string (the app's
 * `OutputSchema.toJsonSchema()`); the proxy instructs the model to emit JSON conforming to it.
 */
data class ChatResponseFormat(
    val type: String = "json_schema",
    val schema: String = "",
)

/** Response body — the assistant's completion text plus the resolved provider model id. */
data class ChatCompletionResponse(
    val content: String,
    val modelId: String? = null,
)
