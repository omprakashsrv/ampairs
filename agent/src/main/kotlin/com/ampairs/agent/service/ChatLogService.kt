package com.ampairs.agent.service

import com.ampairs.agent.domain.dto.ChatLogRequest
import com.ampairs.agent.domain.dto.toEntity
import com.ampairs.agent.repository.ChatLogRepository
import org.springframework.stereotype.Service

@Service
class ChatLogService(
    private val chatLogRepository: ChatLogRepository,
) {

    /**
     * Persist a batch of uploaded assistant turns for the given user. The workspace (`owner_id`) is
     * stamped automatically from the tenant context (set by SessionUserFilter from X-Workspace-ID).
     * Blank-user-message rows are dropped defensively. Returns the number saved.
     */
    fun save(requests: List<ChatLogRequest>, userId: String): Int {
        val entities = requests
            .filter { it.userMessage.isNotBlank() }
            .map { it.toEntity(userId) }
        if (entities.isEmpty()) return 0
        chatLogRepository.saveAll(entities)
        return entities.size
    }
}
