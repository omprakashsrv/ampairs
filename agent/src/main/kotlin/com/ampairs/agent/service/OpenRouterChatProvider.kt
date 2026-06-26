package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

/**
 * [ChatProvider] for OpenRouter via its OpenAI-compatible Chat Completions API
 * (`POST {base-url}/chat/completions`). Lets us serve free/no-cost models (e.g. the `openrouter/free`
 * smart router, or specific `…:free` models) alongside Anthropic. Raw JDK [HttpClient] + Jackson,
 * mirroring [AiModelProxyService]'s outbound-call pattern; the key is server-side only
 * (`agent.chat.openrouter.api-key` → `OPENROUTER_API_KEY`).
 *
 * OpenAI's shape carries the system prompt as a `role: "system"` message, so the shared
 * [ChatPrompt.systemPrompt] is prepended as one. Structured output is requested via the same JSON-only
 * system instruction (not OpenAI `response_format`) so it works across the variable free-model lineup.
 */
@Component
class OpenRouterChatProvider(
    @Value("\${agent.chat.openrouter.api-key:}") private val apiKey: String,
    @Value("\${agent.chat.openrouter.base-url:https://openrouter.ai/api/v1}") private val baseUrl: String,
    @Value("\${agent.chat.openrouter.default-model:openrouter/free}") override val defaultModel: String,
    @Value("\${agent.chat.openrouter.allowed-models:openrouter/free,google/gemma-4-31b-it:free}")
    allowedModelsCsv: String,
    // Optional OpenRouter ranking headers (appear on their leaderboards). Blank → not sent.
    @Value("\${agent.chat.openrouter.referer:}") private val referer: String,
    @Value("\${agent.chat.openrouter.title:Ampairs}") private val title: String,
    private val objectMapper: ObjectMapper,
) : ChatProvider {

    override val id: String = ID

    private val allowedModels: Set<String> =
        allowedModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override fun serves(model: String): Boolean = model.trim() in allowedModels

    override fun complete(request: ChatCompletionRequest, model: String): ChatCompletionResponse {
        val messages = buildList {
            ChatPrompt.systemPrompt(request).takeIf { it.isNotBlank() }
                ?.let { add(OpenAiMessage(role = "system", content = it)) }
            ChatPrompt.conversation(request).forEach {
                val role = if (it.role.equals("assistant", ignoreCase = true)) "assistant" else "user"
                add(OpenAiMessage(role = role, content = it.content))
            }
        }
        val body = OpenAiChatRequest(
            model = model,
            messages = messages,
            maxTokens = request.maxTokens.coerceIn(1, MAX_CHAT_OUTPUT_TOKENS),
            temperature = request.temperature,
        )

        val httpRequest = HttpRequest.newBuilder(URI.create("$baseUrl/chat/completions"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .apply {
                if (referer.isNotBlank()) header("HTTP-Referer", referer)
                if (title.isNotBlank()) header("X-Title", title)
            }
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build()

        val response = runCatching {
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        }.getOrElse {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenRouter request failed: ${it.message}")
        }
        if (response.statusCode() >= 400) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OpenRouter returned ${response.statusCode()} for model $model",
            )
        }

        val parsed = objectMapper.readValue(response.body(), OpenAiChatResponse::class.java)
        val text = parsed.choices.firstOrNull()?.message?.content.orEmpty()
        return ChatCompletionResponse(content = text, modelId = parsed.model ?: model)
    }

    companion object {
        const val ID: String = "openrouter"
    }
}

/** OpenAI-compatible request body. Field names map to snake_case via the shared ObjectMapper. */
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val maxTokens: Int,
    val temperature: Float,
)

private data class OpenAiMessage(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val model: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiChoice(
    val message: OpenAiMessage? = null,
)
