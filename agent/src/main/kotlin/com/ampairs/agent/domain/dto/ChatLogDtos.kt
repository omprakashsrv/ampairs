package com.ampairs.agent.domain.dto

import com.ampairs.agent.domain.model.ChatLog
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/**
 * One uploaded assistant turn. `clientTimestamp` is epoch-millis (unambiguous across the kotlinx
 * client / Jackson server boundary). `userId` and `ownerId` are NOT accepted from the client — the
 * server stamps the authenticated user and the workspace (tenant) itself.
 */
data class ChatLogRequest(
    val sessionId: String? = null,
    @field:NotBlank val userMessage: String = "",
    val assistantMessage: String = "",
    val modelId: String? = null,
    val intent: String? = null,
    val moduleName: String? = null,
    val actionType: String? = null,
    val clientTimestamp: Long? = null,
)

/** Result of an upload batch. */
data class ChatLogUploadResponse(
    val saved: Int,
)

fun ChatLogRequest.toEntity(userId: String): ChatLog = ChatLog().also {
    it.userId = userId
    it.sessionId = sessionId
    it.userMessage = userMessage
    it.assistantMessage = assistantMessage
    it.modelId = modelId
    it.intent = intent
    it.moduleName = moduleName
    it.actionType = actionType
    it.clientTimestamp = clientTimestamp?.let(Instant::ofEpochMilli)
}
