package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * [ChatProvider] for Anthropic Claude via the official Java SDK. The key is server-side only
 * (`agent.anthropic-api-key` → `AGENT_ANTHROPIC_API_KEY`). Anthropic carries the system prompt as a
 * dedicated field (not a message role), so the shared [ChatPrompt.systemPrompt] is mapped to `.system`.
 */
@Component
class AnthropicChatProvider(
    @Value("\${agent.anthropic-api-key:}") private val apiKey: String,
    @Value("\${agent.chat.anthropic.default-model:claude-sonnet-4-6}") override val defaultModel: String,
    @Value("\${agent.chat.anthropic.allowed-models:claude-sonnet-4-6,claude-opus-4-8,claude-haiku-4-5}")
    allowedModelsCsv: String,
) : ChatProvider {

    override val id: String = ID

    private val allowedModels: Set<String> =
        allowedModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()

    // Created lazily so a missing key fails the request (503 at the router) rather than app startup.
    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override fun serves(model: String): Boolean = model.trim() in allowedModels

    override fun complete(request: ChatCompletionRequest, model: String): ChatCompletionResponse {
        val systemPrompt = ChatPrompt.systemPrompt(request)

        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(request.maxTokens.coerceIn(1, MAX_CHAT_OUTPUT_TOKENS).toLong())
        if (systemPrompt.isNotBlank()) {
            builder.system(systemPrompt)
        }
        ChatPrompt.conversation(request).forEach { msg ->
            val role = if (msg.role.equals("assistant", ignoreCase = true)) {
                MessageParam.Role.ASSISTANT
            } else {
                MessageParam.Role.USER
            }
            builder.addMessage(MessageParam.builder().role(role).content(msg.content).build())
        }

        val response = client.messages().create(builder.build())

        // Concatenate the text blocks (thinking/tool blocks, if any, are skipped).
        val text = response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("")

        return ChatCompletionResponse(content = text, modelId = model)
    }

    companion object {
        const val ID: String = "anthropic"
    }
}
