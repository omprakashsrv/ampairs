package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionMessage
import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatResponseFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for the per-provider allow-list / configuration logic and the shared [ChatPrompt] shaping.
 * The actual upstream HTTP/SDK call requires live credentials and is verified on-device, not here.
 */
class ChatProvidersTest {

    // --- AnthropicChatProvider ---------------------------------------------------------------------

    private fun anthropic(apiKey: String = "") = AnthropicChatProvider(
        apiKey = apiKey,
        defaultModel = "claude-sonnet-4-6",
        allowedModelsCsv = "claude-sonnet-4-6, claude-opus-4-8",
    )

    @Test
    fun `anthropic isConfigured tracks the api key`() {
        assertThat(anthropic(apiKey = "").isConfigured()).isFalse()
        assertThat(anthropic(apiKey = "sk-test").isConfigured()).isTrue()
    }

    @Test
    fun `anthropic serves only allow-listed models`() {
        val provider = anthropic()
        assertThat(provider.serves("claude-opus-4-8")).isTrue()
        assertThat(provider.serves("  claude-sonnet-4-6  ")).isTrue()
        assertThat(provider.serves("openrouter/free")).isFalse()
        assertThat(provider.defaultModel).isEqualTo("claude-sonnet-4-6")
    }

    // --- OpenRouterChatProvider --------------------------------------------------------------------

    private fun openRouter(apiKey: String = "") = OpenRouterChatProvider(
        apiKey = apiKey,
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openrouter/free",
        allowedModelsCsv = "openrouter/free, google/gemma-4-31b-it:free",
        referer = "",
        title = "Ampairs",
    )

    @Test
    fun `openrouter isConfigured tracks the api key`() {
        assertThat(openRouter(apiKey = "").isConfigured()).isFalse()
        assertThat(openRouter(apiKey = "sk-or-v1-test").isConfigured()).isTrue()
    }

    @Test
    fun `openrouter serves only allow-listed models`() {
        val provider = openRouter()
        assertThat(provider.serves("openrouter/free")).isTrue()
        assertThat(provider.serves("google/gemma-4-31b-it:free")).isTrue()
        assertThat(provider.serves("claude-sonnet-4-6")).isFalse()
        assertThat(provider.defaultModel).isEqualTo("openrouter/free")
    }

    // --- ChatPrompt --------------------------------------------------------------------------------

    @Test
    fun `systemPrompt joins system turns and appends the response schema`() {
        val request = ChatCompletionRequest(
            model = "m",
            messages = listOf(
                ChatCompletionMessage(role = "system", content = "You are an assistant."),
                ChatCompletionMessage(role = "user", content = "hi"),
            ),
            responseFormat = ChatResponseFormat(type = "json_schema", schema = """{"type":"object"}"""),
        )

        val system = ChatPrompt.systemPrompt(request)

        assertThat(system).contains("You are an assistant.")
        assertThat(system).contains("JSON schema:")
        assertThat(system).contains("""{"type":"object"}""")
    }

    @Test
    fun `conversation drops system and blank turns`() {
        val request = ChatCompletionRequest(
            model = "m",
            messages = listOf(
                ChatCompletionMessage(role = "system", content = "sys"),
                ChatCompletionMessage(role = "user", content = "  "),
                ChatCompletionMessage(role = "user", content = "real question"),
            ),
        )

        val convo = ChatPrompt.conversation(request)

        assertThat(convo).hasSize(1)
        assertThat(convo.first().content).isEqualTo("real question")
    }
}
