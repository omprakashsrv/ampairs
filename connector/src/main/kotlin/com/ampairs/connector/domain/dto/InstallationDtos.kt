package com.ampairs.connector.domain.dto

import com.ampairs.connector.domain.model.ConnectorInstallation
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/** Request to install a connector into the current workspace (FR-003). */
data class InstallConnectorRequest(
    @field:NotBlank val connectorType: String = "",
)

/** Public view of an installation — no internal id, no audit internals beyond timestamps. */
data class InstallationResponse(
    val uid: String,
    val connectorType: String,
    val status: String,
    val autoStart: Boolean,
    val scheduleSeconds: Int?,
    val lastErrorMessage: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun ConnectorInstallation.asResponse(): InstallationResponse = InstallationResponse(
    uid = uid,
    connectorType = connectorType,
    status = status.name,
    autoStart = autoStart,
    scheduleSeconds = scheduleSeconds,
    lastErrorMessage = lastErrorMessage,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<ConnectorInstallation>.asResponses(): List<InstallationResponse> = map { it.asResponse() }
