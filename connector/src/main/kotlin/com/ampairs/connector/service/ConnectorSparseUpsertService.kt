package com.ampairs.connector.service

import com.ampairs.connector.domain.dto.SparseUpsertResult
import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.connector.domain.model.ConnectorSyncRun
import com.ampairs.connector.repository.ConnectorSyncRunRepository
import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.exception.BusinessException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * The connector data write path (FR-018). For each row the writable columns = the columns PRESENT in
 * the row ∩ the installation's mapping allowlist for the entity; only those are applied, omitted
 * columns are preserved, out-of-mapping keys are ignored. Match is by refId/uid only (the writer).
 * This is SEPARATE from the global `/sync` upsert (FR-018a). Each row runs in its own transaction via
 * [ConnectorRowWriter] (FR-022), so a partial-batch failure leaves the rest intact. A run is recorded
 * for observability (FR-020).
 *
 * Writers are contributed by target modules via the core [ConnectorEntityWriter] SPI.
 */
@Service
class ConnectorSparseUpsertService(
    writers: List<ConnectorEntityWriter>,
    private val mappingService: ConnectorMappingService,
    private val installationService: ConnectorInstallationService,
    private val rowWriter: ConnectorRowWriter,
    private val runRepository: ConnectorSyncRunRepository,
) {
    private val writersByType: Map<String, ConnectorEntityWriter> = writers.associateBy { it.entityType }

    /** Serialise concurrent upserts per (installation, entity) so checkpoint windows don't overlap (FR-021). */
    private val locks = ConcurrentHashMap<String, Any>()

    fun upsert(installationUid: String, entityType: String, rows: List<SparseUpsertRow>): List<SparseUpsertResult> {
        installationService.getInstallation(installationUid)
        val writer = writersByType[entityType]
            ?: throw BusinessException("CONNECTOR_UNSUPPORTED_ENTITY", "No connector writer for entity type '$entityType'")
        val allowlist = mappingService.allowlist(installationUid, entityType)

        val lock = locks.computeIfAbsent("$installationUid::$entityType") { Any() }
        return synchronized(lock) {
            var created = 0; var updated = 0; var failed = 0
            val results = rows.map { row ->
                try {
                    val result = rowWriter.writeRow(writer, allowlist, row)
                    when (result.outcome) {
                        WriteOutcome.CREATED.name -> created++
                        WriteOutcome.UPDATED.name -> updated++
                        WriteOutcome.FAILED.name -> failed++
                    }
                    result
                } catch (e: Exception) {
                    failed++
                    SparseUpsertResult(row.refId, null, WriteOutcome.FAILED.name, emptyList(), e.message)
                }
            }
            recordRun(installationUid, entityType, rows.size, created, updated, failed)
            results
        }
    }

    private fun recordRun(installationUid: String, entityType: String, processed: Int, created: Int, updated: Int, failed: Int) {
        runRepository.save(
            ConnectorSyncRun().apply {
                this.installationUid = installationUid
                this.entityType = entityType
                this.trigger = "MANUAL"
                this.startedAt = Instant.now()
                this.finishedAt = Instant.now()
                this.processed = processed
                this.created = created
                this.updated = updated
                this.failed = failed
                this.status = when {
                    failed == 0 -> "SUCCESS"
                    failed == processed -> "FAILED"
                    else -> "PARTIAL"
                }
            }
        )
    }
}
