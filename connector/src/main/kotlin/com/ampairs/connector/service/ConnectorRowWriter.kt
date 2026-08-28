package com.ampairs.connector.service

import com.ampairs.connector.domain.dto.SparseUpsertResult
import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.WriteOutcome
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Applies ONE connector row in its OWN transaction (FR-022): the writable columns are the row's
 * present columns ∩ the mapping allowlist, applied via the entity writer. Running each row in a
 * `REQUIRES_NEW` transaction isolates failures — one bad row rolls back alone and does not poison the
 * rest of the batch. A failure throws so this row's partial writes are rolled back; the caller records
 * it as FAILED. Separate bean so Spring's transactional proxy applies (no self-invocation).
 */
@Component
class ConnectorRowWriter {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun writeRow(writer: ConnectorEntityWriter, allowlist: Set<String>, row: SparseUpsertRow): SparseUpsertResult {
        val present = row.values.filterKeys { it in allowlist }
        val result = writer.applySparse(row.refId, row.uid, present)
        // Rollback must not depend on the writer THROWING. A writer that legally RETURNS a FAILED
        // outcome (the SPI allows it) would otherwise have its partial writes committed by this
        // REQUIRES_NEW transaction, defeating the per-row isolation guarantee. Throw here so the row's
        // transaction rolls back; the caller (ConnectorSparseUpsertService) records it as FAILED.
        if (result.outcome == WriteOutcome.FAILED) {
            throw IllegalStateException(result.message ?: "Connector write failed for entity '${writer.entityType}'")
        }
        return SparseUpsertResult(
            refId = row.refId,
            ampairsUid = result.ampairsUid,
            outcome = result.outcome.name,
            appliedColumns = result.appliedColumns,
            message = result.message,
        )
    }
}
