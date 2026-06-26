package com.ampairs.agent.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * One captured assistant turn (a user message + the assistant's reply) uploaded from the on-device
 * AI assistant for later analysis / quality improvement. Workspace-scoped via [OwnableBaseDomain]
 * (`owner_id` = the X-Workspace-ID the request carried). Write-mostly telemetry — there is no read
 * API yet; analysis is offline/admin.
 *
 * Capture is opt-in on the client (default OFF), so the absence of rows for a workspace is expected.
 */
@Entity(name = "agent_chat_log")
@Table(
    name = "agent_chat_log",
    indexes = [
        Index(name = "idx_agent_chat_log_owner", columnList = "owner_id"),
        Index(name = "idx_agent_chat_log_session", columnList = "session_id"),
    ],
)
class ChatLog : OwnableBaseDomain() {

    /** The authenticated user who produced the turn (uid). */
    @Column(name = "user_id", length = 200)
    var userId: String = ""

    /** Client-side conversation/thread id, for grouping turns of one chat. Optional. */
    @Column(name = "session_id", length = 200)
    var sessionId: String? = null

    @Column(name = "user_message", columnDefinition = "TEXT")
    var userMessage: String = ""

    @Column(name = "assistant_message", columnDefinition = "TEXT")
    var assistantMessage: String = ""

    /** Chat model id that produced the reply (e.g. gemma-3n-e2b), when known. */
    @Column(name = "model_id", length = 200)
    var modelId: String? = null

    /** Resolved intent kind: action / query / conversation / clarify, when known. */
    @Column(name = "intent", length = 50)
    var intent: String? = null

    /** Module the resolved action/query targeted (invoice, customer, …), when known. */
    @Column(name = "module_name", length = 100)
    var moduleName: String? = null

    /** Resolved action type (COUNT, TOTAL_SALES, …), when the turn ran a named action. */
    @Column(name = "action_type", length = 100)
    var actionType: String? = null

    /** When the turn happened on the device (UTC). May lag the upload time. */
    @Column(name = "client_timestamp")
    var clientTimestamp: Instant? = null

    override fun obtainSeqIdPrefix(): String = "CHATLOG"
}
