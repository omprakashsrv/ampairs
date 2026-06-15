package com.ampairs.connector.service

import com.ampairs.connector.config.ConnectorJson
import com.ampairs.connector.config.ConnectorSecretCipher
import com.ampairs.connector.domain.dto.ConfigRequest
import com.ampairs.connector.domain.dto.ConfigResponse
import com.ampairs.connector.domain.dto.ConnectionTestResponse
import com.ampairs.connector.domain.model.ConnectorConfig
import com.ampairs.connector.repository.ConnectorConfigRepository
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Connection config: non-secret values stored as JSON, secrets encrypted at rest and never returned.
 * For client-side connectors the reachability test runs on the client; this records the result
 * (`last_validated_at`) — the server does not reach the external system (FR-009).
 */
@Service
class ConnectorConfigService(
    private val configRepository: ConnectorConfigRepository,
    private val installationService: ConnectorInstallationService,
    private val cipher: ConnectorSecretCipher,
) {
    private val objectMapper = ConnectorJson.mapper
    private val mapType = object : TypeReference<Map<String, String>>() {}

    fun get(installationUid: String): ConfigResponse {
        installationService.getInstallation(installationUid) // 404 if missing
        val config = configRepository.findByInstallationUid(installationUid)
        return config?.toResponse() ?: ConfigResponse(installationUid, emptyMap(), emptyList(), null)
    }

    @Transactional
    fun upsert(installationUid: String, request: ConfigRequest): ConfigResponse {
        installationService.getInstallation(installationUid)
        val config = configRepository.findByInstallationUid(installationUid)
            ?: ConnectorConfig().apply { this.installationUid = installationUid }

        config.nonSecretValues = objectMapper.writeValueAsString(request.nonSecretValues)

        // Merge secrets so a partial update doesn't wipe existing secrets.
        val existingSecrets = config.decodeSecrets()
        if (request.secretValues.isNotEmpty()) {
            val merged = existingSecrets + request.secretValues
            config.secretValuesEncrypted = cipher.encrypt(objectMapper.writeValueAsString(merged))
        }
        val saved = configRepository.save(config)
        installationService.markEnabledIfReady(installationUid)
        return saved.toResponse()
    }

    @Transactional
    fun recordConnectionTest(installationUid: String, ok: Boolean, message: String?): ConnectionTestResponse {
        installationService.getInstallation(installationUid)
        val config = configRepository.findByInstallationUid(installationUid)
            ?: ConnectorConfig().apply { this.installationUid = installationUid }
        if (ok) config.lastValidatedAt = Instant.now()
        val saved = configRepository.save(config)
        return ConnectionTestResponse(ok = ok, message = message, lastValidatedAt = saved.lastValidatedAt)
    }

    private fun ConnectorConfig.decodeSecrets(): Map<String, String> =
        secretValuesEncrypted?.takeIf { it.isNotBlank() }
            ?.let { objectMapper.readValue(cipher.decrypt(it), mapType) }
            ?: emptyMap()

    private fun ConnectorConfig.toResponse(): ConfigResponse = ConfigResponse(
        installationUid = installationUid,
        nonSecretValues = nonSecretValues?.takeIf { it.isNotBlank() }
            ?.let { objectMapper.readValue(it, mapType) } ?: emptyMap(),
        secretKeysSet = decodeSecrets().keys.sorted(),
        lastValidatedAt = lastValidatedAt,
    )
}
