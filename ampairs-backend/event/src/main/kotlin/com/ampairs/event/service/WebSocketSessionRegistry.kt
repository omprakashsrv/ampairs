package com.ampairs.event.service

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry that keeps lightweight metadata about active WebSocket sessions.
 *
 * The session attributes provided by Spring are not reliably available during
 * disconnect callbacks or heartbeat frames, which makes it hard to resolve the
 * tenant/device context required by our multi-tenant persistence layer.
 *
 * We therefore keep a small in-memory lookup that maps a session id back to the
 * workspace/user/device information we discovered during the CONNECT handshake.
 * This lets heartbeat and disconnect handlers consistently resolve the correct
 * tenant context before touching the database.
 */
@Component
class WebSocketSessionRegistry {

    data class SessionMetadata(
        val workspaceId: String,
        val userId: String?,
        val deviceId: String?
    )

    private val sessions = ConcurrentHashMap<String, SessionMetadata>()

    fun register(sessionId: String, metadata: SessionMetadata) {
        sessions[sessionId] = metadata
    }

    fun get(sessionId: String): SessionMetadata? = sessions[sessionId]

    fun remove(sessionId: String): SessionMetadata? = sessions.remove(sessionId)
}
