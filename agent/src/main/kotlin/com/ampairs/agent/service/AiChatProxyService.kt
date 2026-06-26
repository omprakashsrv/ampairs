package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * Cloud chat proxy: turns the app's provider-agnostic [ChatCompletionRequest] into an Anthropic
 * Messages API call and returns the assistant text. The provider key is held **server-side only**
 * ([anthropicApiKey], bound from `AGENT_ANTHROPIC_API_KEY`), so the app never sees it — it just calls
 * `POST /agent/v1/chat/completions` with its JWT + `X-Workspace-ID`.
 *
 * Mirrors [AiModelProxyService]'s role as the single outbound-call point for the agent module, but for
 * inference rather than model downloads. The requested model is validated against [allowedModels]; an
 * unknown id falls back to [defaultModel] so a stale app build can't request a model we don't serve.
 */
@Service
class AiChatProxyService(
    // Anthropic API key. Bound from `agent.anthropic-api-key` (→ AGENT_ANTHROPIC_API_KEY) in
    // application.yml. Blank → completions return 503 ("cloud chat not configured"). SECRET.
    @Value("\${agent.anthropic-api-key:}") private val anthropicApiKey: String,
    @Value("\${agent.chat.default-model:claude-sonnet-4-6}") private val defaultModel: String,
    @Value("\${agent.chat.allowed-models:claude-sonnet-4-6,claude-opus-4-8,claude-haiku-4-5}")
    allowedModelsCsv: String,
) {

    private val allowedModels: Set<String> =
        allowedModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    // Created lazily so a missing key fails the request (503) rather than app startup.
    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder().apiKey(anthropicApiKey).build()
    }

    fun complete(request: ChatCompletionRequest): ChatCompletionResponse {
        if (anthropicApiKey.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Cloud chat is not configured on this server",
            )
        }
        if (request.messages.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "messages must not be empty")
        }

        val model = resolveModel(request.model)

        // Anthropic carries the system prompt as a dedicated field, not a message role — pull the
        // system turns out and join them. The structured-output schema (if any) is appended as an
        // instruction so the model returns raw JSON the app's OutputSchema can parse.
        val systemPrompt = buildString {
            request.messages.filter { it.role.equals("system", ignoreCase = true) }
                .forEach { appendLine(it.content) }
            request.responseFormat?.takeIf { it.schema.isNotBlank() }?.let {
                appendLine()
                appendLine(
                    "Respond with ONLY a single JSON object that strictly conforms to the JSON " +
                        "schema below. Output no prose, no markdown code fences, no explanation — " +
                        "only the JSON object.",
                )
                appendLine("JSON schema:")
                append(it.schema)
            }
        }.trim()

        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(request.maxTokens.coerceIn(1, MAX_OUTPUT_TOKENS).toLong())
        if (systemPrompt.isNotBlank()) {
            builder.system(systemPrompt)
        }
        request.messages
            .filter { !it.role.equals("system", ignoreCase = true) && it.content.isNotBlank() }
            .forEach { msg ->
                val role = if (msg.role.equals("assistant", ignoreCase = true)) {
                    MessageParam.Role.ASSISTANT
                } else {
                    MessageParam.Role.USER
                }
                builder.addMessage(
                    MessageParam.builder().role(role).content(msg.content).build(),
                )
            }

        val response = client.messages().create(builder.build())

        // Concatenate the text blocks (thinking/tool blocks, if any, are skipped).
        val text = response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("")

        return ChatCompletionResponse(content = text, modelId = model)
    }

    /** Requested model if it's in the allow-list, otherwise the configured default. */
    internal fun resolveModel(requested: String): String =
        requested.trim().takeIf { it in allowedModels } ?: defaultModel

    private companion object {
        // Safety ceiling on the per-request output the app can request through the proxy.
        const val MAX_OUTPUT_TOKENS = 8192
    }
}
