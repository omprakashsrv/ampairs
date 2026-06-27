package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionMessage
import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse

/** Safety ceiling on the per-request output the app can request through any cloud provider. */
const val MAX_CHAT_OUTPUT_TOKENS: Int = 8192

/**
 * One upstream chat provider (Anthropic, OpenRouter, …). [AiChatProxyService] routes a request to the
 * provider that [serves] the requested model; each provider owns its own credentials, allow-list, and
 * wire format. Adding a provider = one more `@Component` implementing this interface — no router edit.
 */
interface ChatProvider {

    /** Stable id, e.g. `"anthropic"` / `"openrouter"`. Matches `agent.chat.default-provider`. */
    val id: String

    /** Model used when the routed request's model isn't in this provider's allow-list. */
    val defaultModel: String

    /** True when this provider has the credentials it needs to serve requests. */
    fun isConfigured(): Boolean

    /** True when [model] is in this provider's allow-list (so the router should route here). */
    fun serves(model: String): Boolean

    /** Run the completion. [model] has already been resolved to one this provider serves. */
    fun complete(request: ChatCompletionRequest, model: String): ChatCompletionResponse
}

/**
 * Shared prompt shaping used by every provider so behavior is identical across upstreams: system turns
 * are collected into a single system prompt, and a structured-output schema (if present) is appended as
 * a JSON-only instruction (the app's `OutputSchema` parses the raw JSON back). This keeps the
 * text-to-SQL / intent path provider-agnostic.
 */
object ChatPrompt {

    fun systemPrompt(request: ChatCompletionRequest): String = buildString {
        request.messages.filter { it.role.equals("system", ignoreCase = true) }
            .forEach { appendLine(it.content) }
        request.responseFormat?.takeIf { it.schema.isNotBlank() }?.let {
            appendLine()
            appendLine(
                "Respond with ONLY a single JSON object that strictly conforms to the JSON schema " +
                    "below. Output no prose, no markdown code fences, no explanation — only the JSON " +
                    "object.",
            )
            appendLine("JSON schema:")
            append(it.schema)
        }
    }.trim()

    /** Non-system, non-blank turns in order — the actual conversation sent to the model. */
    fun conversation(request: ChatCompletionRequest): List<ChatCompletionMessage> =
        request.messages.filter { !it.role.equals("system", ignoreCase = true) && it.content.isNotBlank() }
}
