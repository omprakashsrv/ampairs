package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.catalogue.HostingType
import com.ampairs.connector.domain.dto.FieldMappingDto
import com.ampairs.connector.domain.dto.FieldMappingRuleDto
import com.ampairs.connector.domain.dto.SparseUpsertResult
import com.ampairs.connector.domain.dto.SparseUpsertRow
import com.ampairs.connector.domain.dto.SyncCheckpointDto
import com.ampairs.connector.domain.model.ConnectorInstallation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

/** Unit tests for the Generic HTTP/JSON server-side executor (spec 029 FR-S03/S04). */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenericHttpJsonExecutorTest {

    @Mock private lateinit var registry: ConnectorCatalogueRegistry
    @Mock private lateinit var configService: ConnectorConfigService
    @Mock private lateinit var mappingService: ConnectorMappingService
    @Mock private lateinit var sparseUpsertService: ConnectorSparseUpsertService
    @Mock private lateinit var syncStateService: ConnectorSyncStateService
    @Mock private lateinit var httpClient: ConnectorHttpClient

    private lateinit var executor: GenericHttpJsonExecutor

    private val installation = ConnectorInstallation().apply { uid = "INST1"; connectorType = "generic_http_json" }

    @BeforeEach
    fun setUp() {
        executor = GenericHttpJsonExecutor(registry, configService, mappingService, sparseUpsertService, syncStateService, httpClient)
    }

    private fun connectorWith(vararg entities: String) = CatalogueConnector(
        type = "generic_http_json", displayName = "Generic", description = "",
        hostingType = HostingType.SERVER_SIDE, supportedEntities = entities.toList(),
    )

    private fun customerMapping() = FieldMappingDto(
        installationUid = "INST1", entityType = "customer",
        rules = listOf(FieldMappingRuleDto("id", "refId"), FieldMappingRuleDto("name", "name")),
    )

    @Test
    fun `entityType is generic_http_json`() {
        assertEquals("generic_http_json", executor.connectorType)
    }

    @Test
    fun `missing base_url yields an error outcome and never fetches`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(ResolvedConnectorConfig(emptyMap(), emptyMap()))

        val outcome = executor.runCycle(installation)

        assertEquals(1, outcome.entityOutcomes.size)
        assertTrue(outcome.hadFailure)
        verify(httpClient, never()).fetchRecords(any())
    }

    @Test
    fun `maps present columns, upserts with SCHEDULED trigger, advances checkpoint`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(
            ResolvedConnectorConfig(mapOf("base_url" to "https://api.test", "path.customer" to "/customers"), mapOf("api_key" to "SECRET")),
        )
        whenever(registry.find("generic_http_json")).thenReturn(connectorWith("customer"))
        whenever(mappingService.list("INST1")).thenReturn(listOf(customerMapping()))
        whenever(syncStateService.listCheckpoints("INST1")).thenReturn(emptyList())
        whenever(httpClient.fetchRecords(any())).thenReturn(listOf(mapOf("id" to "C1", "name" to "Acme")))
        whenever(sparseUpsertService.upsert(eq("INST1"), eq("customer"), any(), eq("SCHEDULED")))
            .thenReturn(listOf(SparseUpsertResult("C1", "CUS1", "CREATED", listOf("name"))))

        val outcome = executor.runCycle(installation)

        // Auth header carries the secret, but the row carries only mapped, present columns; refId is identity.
        verify(sparseUpsertService).upsert(eq("INST1"), eq("customer"), check<List<SparseUpsertRow>> {
            assertEquals(1, it.size)
            assertEquals("C1", it[0].refId)
            assertEquals(mapOf("name" to "Acme"), it[0].values)
        }, eq("SCHEDULED"))
        verify(syncStateService).upsertCheckpoint(eq("INST1"), check<SyncCheckpointDto> {
            assertEquals("customer", it.entityType)
            assertEquals("INBOUND", it.direction)
        })
        assertEquals(1, outcome.created)
        assertEquals(1, outcome.processed)
    }

    @Test
    fun `entity without a configured path is skipped`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(
            ResolvedConnectorConfig(mapOf("base_url" to "https://api.test"), emptyMap()), // no path.customer
        )
        whenever(registry.find("generic_http_json")).thenReturn(connectorWith("customer"))
        whenever(mappingService.list("INST1")).thenReturn(listOf(customerMapping()))
        whenever(syncStateService.listCheckpoints("INST1")).thenReturn(emptyList())

        val outcome = executor.runCycle(installation)

        assertTrue(outcome.entityOutcomes.isEmpty())
        verify(httpClient, never()).fetchRecords(any())
    }

    @Test
    fun `failed rows leave the checkpoint un-advanced`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(
            ResolvedConnectorConfig(mapOf("base_url" to "https://api.test", "path.customer" to "/customers"), mapOf("api_key" to "K")),
        )
        whenever(registry.find("generic_http_json")).thenReturn(connectorWith("customer"))
        whenever(mappingService.list("INST1")).thenReturn(listOf(customerMapping()))
        whenever(syncStateService.listCheckpoints("INST1")).thenReturn(emptyList())
        whenever(httpClient.fetchRecords(any())).thenReturn(listOf(mapOf("id" to "C1", "name" to "Acme")))
        whenever(sparseUpsertService.upsert(any(), any(), any(), any()))
            .thenReturn(listOf(SparseUpsertResult("C1", null, "FAILED", emptyList(), "boom")))

        val outcome = executor.runCycle(installation)

        assertEquals(1, outcome.failed)
        verify(syncStateService, never()).upsertCheckpoint(any(), any())
    }

    @Test
    fun `a fetch exception is captured as an entity error, no checkpoint`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(
            ResolvedConnectorConfig(mapOf("base_url" to "https://api.test", "path.customer" to "/customers"), mapOf("api_key" to "K")),
        )
        whenever(registry.find("generic_http_json")).thenReturn(connectorWith("customer"))
        whenever(mappingService.list("INST1")).thenReturn(listOf(customerMapping()))
        whenever(syncStateService.listCheckpoints("INST1")).thenReturn(emptyList())
        whenever(httpClient.fetchRecords(any())).thenThrow(RuntimeException("network down"))

        val outcome = executor.runCycle(installation)

        assertEquals("network down", outcome.entityOutcomes.single().error)
        verify(sparseUpsertService, never()).upsert(any(), any(), any(), any())
        verify(syncStateService, never()).upsertCheckpoint(any(), any())
    }

    @Test
    fun `a record with no mapped refId is dropped before upsert`() {
        whenever(configService.resolveForExecution("INST1")).thenReturn(
            ResolvedConnectorConfig(mapOf("base_url" to "https://api.test", "path.customer" to "/customers"), mapOf("api_key" to "K")),
        )
        whenever(registry.find("generic_http_json")).thenReturn(connectorWith("customer"))
        whenever(mappingService.list("INST1")).thenReturn(listOf(customerMapping()))
        whenever(syncStateService.listCheckpoints("INST1")).thenReturn(emptyList())
        // record has 'name' but no 'id' → no refId → dropped; upsert gets an empty row list → not called
        whenever(httpClient.fetchRecords(any())).thenReturn(listOf(mapOf("name" to "NoId")))

        val outcome = executor.runCycle(installation)

        verify(sparseUpsertService, never()).upsert(any(), any(), any(), any())
        assertEquals(0, outcome.processed)
        assertNull(outcome.entityOutcomes.single().error)
    }
}
