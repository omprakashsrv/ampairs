package com.ampairs.agent.controller

import com.ampairs.agent.domain.dto.ChatCompletionRequest
import com.ampairs.agent.domain.dto.ChatCompletionResponse
import com.ampairs.agent.service.AiChatProxyService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Cloud chat completion proxy for the on-device assistant's **online** model tier. The app's
 * `AmpairsProxyTransport` (cloud `LlmBackend`) calls this; the backend forwards to the hosted model
 * with a server-side key and returns the assistant text. Global (not tenant-scoped) but
 * JWT-authenticated like [AiModelController] — the app always carries `X-Workspace-ID` from inside a
 * workspace, so no `SessionUserFilter` exemption is needed; the endpoint doesn't use tenant context.
 *
 * - `POST /agent/v1/chat/completions` — one-shot completion. `ApiResponse<ChatCompletionResponse>`.
 */
@RestController
@RequestMapping("/agent/v1/chat")
class AiChatController(
    private val chatProxyService: AiChatProxyService,
) {

    @PostMapping("/completions")
    fun complete(@RequestBody @Valid request: ChatCompletionRequest): ApiResponse<ChatCompletionResponse> =
        ApiResponse.success(chatProxyService.complete(request))
}
