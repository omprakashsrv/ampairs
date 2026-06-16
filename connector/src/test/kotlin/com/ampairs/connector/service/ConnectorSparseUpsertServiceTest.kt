package com.ampairs.connector.service

import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.connector.domain.model.ConnectorInstallation
import com.ampairs.connector.domain.model.ConnectorSyncRun
import com.ampairs.connector.repository.ConnectorSyncRunRepository
import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.connector.WriteResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Validates the connector sparse-upsert contract: per-row presence ∩ allowlist (FR-018, SC-004). */
class ConnectorSparseUpsertServiceTest {

    /** Captures the columns each row actually delivers to the entity writer, keyed by refId. */
    private class CapturingWriter : ConnectorEntityWriter {
        override val entityType = "product"
        val byRef = linkedMapOf<String?, Map<String, Any?>>()
        override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
            byRef[refId] = presentColumns
            return WriteResult(WriteOutcome.UPDATED, "P-$refId", presentColumns.keys.toList())
        }
    }

    private val mappingService = mock<ConnectorMappingService>()
    private val installationService = mock<ConnectorInstallationService>()
    private val runRepository = mock<ConnectorSyncRunRepository>()

    @Test
    fun `each row writes only its own present, allowlisted columns`() {
        val writer = CapturingWriter()
        whenever(installationService.getInstallation(any())).thenReturn(ConnectorInstallation())
        whenever(mappingService.allowlist(any(), any())).thenReturn(setOf("col1", "col2", "col3", "col4"))
        whenever(runRepository.save(any<ConnectorSyncRun>())).thenAnswer { it.arguments[0] }

        val service = ConnectorSparseUpsertService(
            listOf(writer), mappingService, installationService, ConnectorRowWriter(), runRepository,
        )
        val results = service.upsert(
            "inst1", "product",
            listOf(
                SparseUpsertRow(refId = "r1", values = mapOf("col1" to "a", "col2" to "b", "zzz" to "ignored")),
                SparseUpsertRow(refId = "r2", values = mapOf("col3" to "c", "col4" to "d")),
            ),
        )

        // Row 1 updates col1/col2 (out-of-allowlist 'zzz' dropped); Row 2 updates col3/col4 — no bleed-through.
        assertEquals(setOf("col1", "col2"), writer.byRef["r1"]!!.keys)
        assertEquals(setOf("col3", "col4"), writer.byRef["r2"]!!.keys)
        assertEquals(2, results.size)
        assertTrue(results.all { it.outcome == "UPDATED" })
    }

    @Test
    fun `unsupported entity type is rejected`() {
        whenever(installationService.getInstallation(any())).thenReturn(ConnectorInstallation())
        val service = ConnectorSparseUpsertService(
            emptyList(), mappingService, installationService, ConnectorRowWriter(), runRepository,
        )
        assertThrows(Exception::class.java) { service.upsert("inst1", "unknown", emptyList()) }
    }
}
