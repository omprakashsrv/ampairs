package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
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
) : ChatProvider {

    override val id: String = ID

    private val allowedModels: Set<String> =
        allowedModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    // Self-constructed, NOT the Spring bean: Spring Boot 4 autoconfigures a Jackson 3 (tools.jackson)
    // ObjectMapper, so injecting the Jackson 2 type fails to wire and breaks every context that scans
    // this bean. We only need request serialization (snake_case via a property naming strategy) and
    // read the response with the tree model, so no jackson-module-kotlin is required.
    private val objectMapper = ObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

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

        // Tree model — avoids needing jackson-module-kotlin to bind into Kotlin data classes.
        val root = objectMapper.readTree(response.body())
        val text = root.path("choices").path(0).path("message").path("content").asText("")
        val returnedModel = root.path("model").asText("").ifBlank { model }
        return ChatCompletionResponse(content = text, modelId = returnedModel)
    }

    companion object {
        const val ID: String = "openrouter"
    }
}

/** OpenAI-compatible request body. Field names serialize to snake_case (e.g. `max_tokens`). */
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
