package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * Cloud chat router for the on-device assistant's **online** model tier. The app's
 * `AmpairsProxyTransport` posts a provider-agnostic [ChatCompletionRequest]; this service routes it to
 * the [ChatProvider] that serves the requested model (Anthropic, OpenRouter, …) and returns the text.
 * Keys stay server-side in the providers — the app only carries its JWT + `X-Workspace-ID`.
 *
 * Routing: the first provider whose allow-list contains the requested model wins; an unrecognized model
 * falls back to [defaultProviderId]'s default model (so a stale app build can't request something we
 * don't serve). The chosen provider must be configured, else 503 — we never silently fall back to a
 * *different* (e.g. paid) provider than the one the model was meant for.
 */
@Service
class AiChatProxyService(
    private val providers: List<ChatProvider>,
    @Value("\${agent.chat.default-provider:anthropic}") private val defaultProviderId: String,
) {

    fun complete(request: ChatCompletionRequest): ChatCompletionResponse {
        if (request.messages.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "messages must not be empty")
        }

        val requested = request.model.trim()
        val matched = providers.firstOrNull { it.serves(requested) }
        val provider = matched
            ?: providers.firstOrNull { it.id == defaultProviderId }
            ?: throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No cloud chat provider is configured on this server",
            )

        if (!provider.isConfigured()) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Cloud chat provider '${provider.id}' is not configured on this server",
            )
        }

        // Use the requested model only if the chosen provider actually serves it; else its default.
        val model = if (provider.serves(requested)) requested else provider.defaultModel
        return provider.complete(request, model)
    }
}
