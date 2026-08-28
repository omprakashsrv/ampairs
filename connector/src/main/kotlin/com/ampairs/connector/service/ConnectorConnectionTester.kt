package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.catalogue.HostingType
import com.ampairs.connector.domain.dto.ConnectionTestRequest
import com.ampairs.connector.domain.dto.ConnectionTestResponse
import org.springframework.stereotype.Service

/**
 * Routes the connection-test endpoint by hosting type (FR-009 / FR-S06):
 * - CLIENT_SIDE (Tally): the client ran the real reachability check; we just record its reported result.
 * - SERVER_SIDE (Generic HTTP/JSON, …): the backend performs the probe via the connector's executor.
 * Either way the outcome is persisted as `last_validated_at` through [ConnectorConfigService].
 */
@Service
class ConnectorConnectionTester(
    private val installationService: ConnectorInstallationService,
    private val registry: ConnectorCatalogueRegistry,
    private val configService: ConnectorConfigService,
    executors: List<ServerSideConnectorSyncExecutor>,
) {
    private val executorsByType = executors.associateBy { it.connectorType }

    fun test(installationUid: String, request: ConnectionTestRequest): ConnectionTestResponse {
        val installation = installationService.getInstallation(installationUid)
        val connector = registry.find(installation.connectorType)
        val serverProbe = if (connector?.hostingType == HostingType.SERVER_SIDE) {
            executorsByType[installation.connectorType]?.testConnection(installation)
        } else null

        return when (serverProbe) {
            // Server-side connector performed the probe.
            true -> configService.recordConnectionTest(installationUid, true, "Reachable")
            false -> configService.recordConnectionTest(installationUid, false, "Unreachable with the configured connection details")
            // Client-side (or a server connector with no probe): trust the client-reported result.
            null -> configService.recordConnectionTest(installationUid, request.ok, request.message)
        }
    }
}
