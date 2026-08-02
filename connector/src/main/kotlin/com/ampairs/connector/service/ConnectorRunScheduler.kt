package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.catalogue.HostingType
import com.ampairs.connector.domain.dto.SyncRunDto
import com.ampairs.connector.domain.model.ConnectorInstallation
import com.ampairs.connector.repository.ConnectorInstallationRepository
import com.ampairs.core.multitenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Drives SERVER-SIDE connector sync on the backend (spec 029 FR-S02). On a fixed interval it selects
 * every active, ENABLED installation ACROSS ALL TENANTS, keeps only those whose connector is
 * [HostingType.SERVER_SIDE], and dispatches each to its [ServerSideConnectorSyncExecutor]. CLIENT_SIDE
 * connectors (Tally) are never dispatched — their engine runs in the desktop app.
 *
 * Runs outside any request, so it sets tenant context per installation from `ownerId` and clears it in
 * a finally — this is the server-side entry point, analogous to a controller (rule 05-multi-tenancy).
 * `@EnableScheduling` is already active app-wide (AmpairsApplication).
 */
@Component
class ConnectorRunScheduler(
    private val installationRepository: ConnectorInstallationRepository,
    private val registry: ConnectorCatalogueRegistry,
    executors: List<ServerSideConnectorSyncExecutor>,
    private val syncStateService: ConnectorSyncStateService,
    @Value("\${connector.server-sync.enabled:true}") private val enabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(ConnectorRunScheduler::class.java)
    private val executorsByType: Map<String, ServerSideConnectorSyncExecutor> = executors.associateBy { it.connectorType }

    @Scheduled(fixedDelayString = "\${connector.server-sync.interval-ms:300000}", initialDelayString = "\${connector.server-sync.initial-delay-ms:60000}")
    fun runDueInstallations() {
        if (!enabled || executorsByType.isEmpty()) return
        val installations = runCatching { installationRepository.findAllEnabledAcrossTenants() }
            .onFailure { log.error("Connector scheduler: failed to load installations", it) }
            .getOrDefault(emptyList())

        val serverSide = installations.filter { registry.find(it.connectorType)?.hostingType == HostingType.SERVER_SIDE }
        if (serverSide.isEmpty()) return
        log.debug("Connector scheduler: {} server-side installation(s) due", serverSide.size)
        serverSide.forEach { runOne(it) }
    }

    private fun runOne(installation: ConnectorInstallation) {
        val tenant = installation.ownerId.takeIf { it.isNotBlank() } ?: return
        val executor = executorsByType[installation.connectorType] ?: return
        TenantContextHolder.setCurrentTenant(tenant)
        try {
            val outcome = executor.runCycle(installation)
            // Per-entity runs are recorded by the sparse-upsert path. Entities that failed to fetch/parse
            // never reached upsert, so record those as FAILED runs here for run-history visibility.
            outcome.entityOutcomes.filter { it.error != null }.forEach { failedEntity ->
                recordFailure(installation.uid, failedEntity.entityType, failedEntity.error)
            }
            if (outcome.entityOutcomes.isNotEmpty()) {
                log.info(
                    "Connector cycle {}/{}: processed={} created={} updated={} failed={}",
                    installation.connectorType, installation.uid,
                    outcome.processed, outcome.created, outcome.updated, outcome.failed,
                )
            }
        } catch (e: Exception) {
            log.error("Connector cycle failed for {}/{}", installation.connectorType, installation.uid, e)
            recordFailure(installation.uid, null, e.message)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    private fun recordFailure(installationUid: String, entityType: String?, error: String?) {
        runCatching {
            syncStateService.recordRun(
                installationUid,
                SyncRunDto(
                    installationUid = installationUid,
                    entityType = entityType,
                    trigger = "SCHEDULED",
                    startedAt = Instant.now(),
                    finishedAt = Instant.now(),
                    status = "FAILED",
                    errorDetail = error,
                ),
            )
        }.onFailure { log.warn("Failed to record connector failure run for {}: {}", installationUid, it.message) }
    }
}
