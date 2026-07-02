package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionMessage
import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Router tests for [AiChatProxyService] — the provider selection and guard logic, exercised with fake
 * [ChatProvider]s so no upstream call is made. Each provider's own wire call is exercised on-device.
 */
class AiChatProxyServiceTest {

    /** Test double — records the model it was asked to complete; never makes a network call. */
    private class FakeProvider(
        override val id: String,
        override val defaultModel: String,
        private val configured: Boolean,
        private val served: Set<String>,
    ) : ChatProvider {
        var completedModel: String? = null
        override fun isConfigured(): Boolean = configured
        override fun serves(model: String): Boolean = model.trim() in served
        override fun complete(request: ChatCompletionRequest, model: String): ChatCompletionResponse {
            completedModel = model
            return ChatCompletionResponse(content = "ok", modelId = model)
        }
    }

    private fun userRequest(model: String) = ChatCompletionRequest(
        model = model,
        messages = listOf(ChatCompletionMessage(role = "user", content = "hi")),
    )

    @Test
    fun `returns 400 when messages are empty`() {
        val router = AiChatProxyService(
            providers = listOf(FakeProvider("anthropic", "claude-sonnet-4-6", configured = true, served = emptySet())),
            defaultProviderId = "anthropic",
        )

        val ex = assertThrows<ResponseStatusException> {
            router.complete(ChatCompletionRequest(model = "claude-sonnet-4-6", messages = emptyList()))
        }

        assertThat(ex.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `routes to the provider whose allow-list serves the requested model`() {
        val anthropic = FakeProvider("anthropic", "claude-sonnet-4-6", configured = true, served = setOf("claude-sonnet-4-6"))
        val openrouter = FakeProvider("openrouter", "openrouter/free", configured = true, served = setOf("openrouter/free"))
        val router = AiChatProxyService(listOf(anthropic, openrouter), defaultProviderId = "anthropic")

        val result = router.complete(userRequest("openrouter/free"))

        assertThat(result.modelId).isEqualTo("openrouter/free")
        assertThat(openrouter.completedModel).isEqualTo("openrouter/free")
        assertThat(anthropic.completedModel).isNull()
    }

    @Test
    fun `falls back to the default provider's default model for an unknown model`() {
        val anthropic = FakeProvider("anthropic", "claude-sonnet-4-6", configured = true, served = setOf("claude-sonnet-4-6"))
        val openrouter = FakeProvider("openrouter", "openrouter/free", configured = true, served = setOf("openrouter/free"))
        val router = AiChatProxyService(listOf(anthropic, openrouter), defaultProviderId = "anthropic")

        router.complete(userRequest("some/unknown-model"))

        assertThat(anthropic.completedModel).isEqualTo("claude-sonnet-4-6")
        assertThat(openrouter.completedModel).isNull()
    }

    @Test
    fun `returns 503 when the matched provider is not configured (no silent cross-provider fallback)`() {
        val anthropic = FakeProvider("anthropic", "claude-sonnet-4-6", configured = true, served = setOf("claude-sonnet-4-6"))
        val openrouter = FakeProvider("openrouter", "openrouter/free", configured = false, served = setOf("openrouter/free"))
        val router = AiChatProxyService(listOf(anthropic, openrouter), defaultProviderId = "anthropic")

        val ex = assertThrows<ResponseStatusException> { router.complete(userRequest("openrouter/free")) }

        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(anthropic.completedModel).isNull()
    }

    @Test
    fun `returns 503 when the default provider for an unknown model is not configured`() {
        val anthropic = FakeProvider("anthropic", "claude-sonnet-4-6", configured = false, served = setOf("claude-sonnet-4-6"))
        val router = AiChatProxyService(listOf(anthropic), defaultProviderId = "anthropic")

        val ex = assertThrows<ResponseStatusException> { router.complete(userRequest("some/unknown-model")) }

        assertThat(ex.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    }
}
