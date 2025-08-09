package com.ampairs.workspace.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.workspace.model.enums.WorkspaceActivityType
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*

/**
 * Central workspace activity logging entity
 * Tracks all workspace-related activities for audit and monitoring purposes
 */
@Entity
@Table(
    name = "workspace_activities",
    indexes = [
        Index(name = "idx_workspace_activity_owner_type", columnList = "owner_id, activity_type"),
        Index(name = "idx_workspace_activity_created_at", columnList = "created_at"),
        Index(name = "idx_workspace_activity_actor", columnList = "actor_id"),
        Index(name = "idx_workspace_activity_target", columnList = "target_entity_type, target_entity_id")
    ]
)
data class WorkspaceActivity(

    /**
     * Type of activity performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    @JsonProperty("activity_type")
    var activityType: WorkspaceActivityType = WorkspaceActivityType.WORKSPACE_CREATED,

    /**
     * User ID who performed the action
     */
    @Column(name = "actor_id", nullable = false, length = 36)
    @JsonProperty("actor_id")
    var actorId: String = "",

    /**
     * Display name of the actor at the time of action
     */
    @Column(name = "actor_name", nullable = false, length = 255)
    @JsonProperty("actor_name")
    var actorName: String = "",

    /**
     * Human-readable description of the activity
     */
    @Column(name = "description", nullable = false, length = 500)
    var description: String = "",

    /**
     * Type of entity that was acted upon (workspace, member, invitation, settings)
     */
    @Column(name = "target_entity_type", length = 50)
    @JsonProperty("target_entity_type")
    var targetEntityType: String? = null,

    /**
     * ID of the entity that was acted upon
     */
    @Column(name = "target_entity_id", length = 36)
    @JsonProperty("target_entity_id")
    var targetEntityId: String? = null,

    /**
     * Name or identifier of the target entity for display
     */
    @Column(name = "target_entity_name", length = 255)
    @JsonProperty("target_entity_name")
    var targetEntityName: String? = null,

    /**
     * Additional context data in JSON format
     * Contains information like old values, new values, counts, etc.
     */
    @Column(name = "context_data", columnDefinition = "JSON")
    @JsonProperty("context_data")
    var contextData: String? = null,

    /**
     * IP address from which the action was performed
     */
    @Column(name = "ip_address", length = 45)
    @JsonProperty("ip_address")
    var ipAddress: String? = null,

    /**
     * User agent string of the client
     */
    @Column(name = "user_agent", length = 500)
    @JsonProperty("user_agent")
    var userAgent: String? = null,

    /**
     * Session or device ID for correlation
     */
    @Column(name = "session_id", length = 100)
    @JsonProperty("session_id")
    var sessionId: String? = null,

    /**
     * Severity level of the activity (INFO, WARN, ERROR)
     */
    @Column(name = "severity", length = 10, nullable = false)
    var severity: String = "INFO",

    ) : OwnableBaseDomain() {

    /**
     * Implementation of abstract method from OwnableBaseDomain
     */
    override fun obtainSeqIdPrefix(): String {
        return "WA" // WorkspaceActivity prefix
    }

    companion object {

        /**
         * Create workspace activity builder
         */
        fun builder(actorId: String, actorName: String): WorkspaceActivityBuilder {
            return WorkspaceActivityBuilder(actorId, actorName)
        }
    }
}

/**
 * Builder pattern for creating WorkspaceActivity instances
 */
class WorkspaceActivityBuilder(
    private val actorId: String,
    private val actorName: String,
) {
    private var activityType: WorkspaceActivityType = WorkspaceActivityType.WORKSPACE_CREATED
    private var description: String = ""
    private var targetEntityType: String? = null
    private var targetEntityId: String? = null
    private var targetEntityName: String? = null
    private var contextData: String? = null
    private var ipAddress: String? = null
    private var userAgent: String? = null
    private var sessionId: String? = null
    private var severity: String = "INFO"

    fun activityType(type: WorkspaceActivityType) = apply { this.activityType = type }
    fun description(desc: String) = apply { this.description = desc }
    fun targetEntity(type: String, id: String, name: String? = null) = apply {
        this.targetEntityType = type
        this.targetEntityId = id
        this.targetEntityName = name
    }

    fun contextData(data: String?) = apply { this.contextData = data }
    fun ipAddress(ip: String?) = apply { this.ipAddress = ip }
    fun userAgent(ua: String?) = apply { this.userAgent = ua }
    fun sessionId(sessionId: String?) = apply { this.sessionId = sessionId }
    fun severity(level: String) = apply { this.severity = level }

    fun build(): WorkspaceActivity {
        return WorkspaceActivity(
            activityType = activityType,
            actorId = actorId,
            actorName = actorName,
            description = description,
            targetEntityType = targetEntityType,
            targetEntityId = targetEntityId,
            targetEntityName = targetEntityName,
            contextData = contextData,
            ipAddress = ipAddress,
            userAgent = userAgent,
            sessionId = sessionId,
            severity = severity
        )
    }
}