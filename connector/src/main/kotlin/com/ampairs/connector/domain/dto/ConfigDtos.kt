package com.ampairs.connector.domain.dto

import java.time.Instant

/** Set/update connection config. `secretValues` is write-only and never echoed back. */
data class ConfigRequest(
    val nonSecretValues: Map<String, String> = emptyMap(),
    val secretValues: Map<String, String> = emptyMap(),
)

/** Connection config view — secrets are masked (only the set keys are listed). */
data class ConfigResponse(
    val installationUid: String,
    val nonSecretValues: Map<String, String>,
    val secretKeysSet: List<String>,
    val lastValidatedAt: Instant?,
)

/** Result of a client-reported (or server) connection test. */
data class ConnectionTestRequest(
    val ok: Boolean = false,
    val message: String? = null,
)

data class ConnectionTestResponse(
    val ok: Boolean,
    val message: String?,
    val lastValidatedAt: Instant?,
)
