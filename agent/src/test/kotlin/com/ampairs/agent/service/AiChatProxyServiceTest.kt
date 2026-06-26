package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionMessage
import com.ampairs.agent.domain.dto.ChatCompletionRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Unit tests for the guard branches of [AiChatProxyService] that run before any Anthropic SDK call —
 * the not-configured (503) and empty-messages (400) guards, plus the model allow-list resolution.
 * The actual completion path requires a live provider key and is exercised on-device, not here.
 */
class AiChatProxyServiceTest {

    private fun service(
        apiKey: String = "test-key",
        defaultModel: String = "claude-sonnet-4-6",
        allowed: String = "claude-sonnet-4-6,claude-opus-4-8",
    ) = AiChatProxyService(apiKey, defaultModel, allowed)

    @Test
    fun `complete returns 503 when the provider key is not configured`() {
        val request = ChatCompletionRequest(
            model = "claude-sonnet-4-6",
            messages = listOf(ChatCompletionMessage(role = "user", content = "hi")),
        )

        val ex = assertThrows<ResponseStatusException> { service(apiKey = "").complete(request) }

        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }

    @Test
    fun `complete returns 400 when messages are empty`() {
        val request = ChatCompletionRequest(model = "claude-sonnet-4-6", messages = emptyList())

        val ex = assertThrows<ResponseStatusException> { service().complete(request) }

        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `resolveModel keeps an allow-listed model`() {
        assertThat(service().resolveModel("claude-opus-4-8")).isEqualTo("claude-opus-4-8")
    }

    @Test
    fun `resolveModel falls back to the default for an unknown model`() {
        assertThat(service().resolveModel("gpt-4o")).isEqualTo("claude-sonnet-4-6")
    }

    @Test
    fun `resolveModel falls back to the default for a blank model`() {
        assertThat(service().resolveModel("  ")).isEqualTo("claude-sonnet-4-6")
    }
}
