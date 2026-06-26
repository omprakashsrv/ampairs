package com.ampairs.agent.controller

import com.ampairs.agent.domain.dto.ChatLogRequest
import com.ampairs.agent.domain.dto.ChatLogUploadResponse
import com.ampairs.agent.service.ChatLogService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.security.AuthenticationHelper
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Receives opt-in chat telemetry uploaded from the on-device assistant. Workspace-scoped: the
 * `SessionUserFilter` enforces the `X-Workspace-ID` header + membership and sets the tenant context,
 * so persisted rows carry `owner_id = workspace` automatically. The authenticated user is stamped
 * server-side from the security context — never trusted from the request body.
 *
 * - `POST /agent/v1/chat-logs` — upload a batch of assistant turns.
 */
@RestController
@RequestMapping("/agent/v1/chat-logs")
class ChatLogController(
    private val chatLogService: ChatLogService,
) {

    @PostMapping
    fun upload(@RequestBody @Valid logs: List<ChatLogRequest>): ApiResponse<ChatLogUploadResponse> {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not resolve user")
        val saved = chatLogService.save(logs, userId)
        return ApiResponse.success(ChatLogUploadResponse(saved = saved))
    }
}
