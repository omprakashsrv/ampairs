package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.catalogue.HostingType
import com.ampairs.connector.domain.dto.SyncRunDto
import com.ampairs.connector.domain.model.ConnectorInstallation
import com.ampairs.connector.repository.ConnectorInstallationRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

/** Unit tests for the server-side run scheduler (spec 029 FR-S02): dispatch only SERVER_SIDE, isolate failures. */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectorRunSchedulerTest {

    @Mock private lateinit var installationRepository: ConnectorInstallationRepository
    @Mock private lateinit var registry: ConnectorCatalogueRegistry
    @Mock private lateinit var syncStateService: ConnectorSyncStateService

    private val serverExecutor = mock<ServerSideConnectorSyncExecutor> {
        on { connectorType } doReturn "generic_http_json"
    }

    private fun scheduler(enabled: Boolean = true) =
        ConnectorRunScheduler(installationRepository, registry, listOf(serverExecutor), syncStateService, enabled)

    private fun installation(type: String, owner: String) = ConnectorInstallation().apply {
        uid = "INST-$type"; connectorType = type; ownerId = owner
    }

    private fun connector(type: String, hosting: HostingType) = CatalogueConnector(
        type = type, displayName = type, description = "", hostingType = hosting, supportedEntities = emptyList(),
    )

    @Test
    fun `dispatches SERVER_SIDE installations and skips CLIENT_SIDE`() {
        val server = installation("generic_http_json", "W1")
        val client = installation("tally", "W2")
        whenever(installationRepository.findAllEnabledAcrossTenants()).thenReturn(listOf(server, client))
        whenever(registry.find("generic_http_json")).thenReturn(connector("generic_http_json", HostingType.SERVER_SIDE))
        whenever(registry.find("tally")).thenReturn(connector("tally", HostingType.CLIENT_SIDE))
        whenever(serverExecutor.runCycle(any())).thenReturn(ServerSyncOutcome(emptyList()))

        scheduler().runDueInstallations()

        verify(serverExecutor).runCycle(check { it.uid == "INST-generic_http_json" })
    }

    @Test
    fun `does nothing when disabled`() {
        scheduler(enabled = false).runDueInstallations()
        verify(installationRepository, never()).findAllEnabledAcrossTenants()
        verify(serverExecutor, never()).runCycle(any())
    }

    @Test
    fun `records a FAILED run for an entity that errored`() {
        val server = installation("generic_http_json", "W1")
        whenever(installationRepository.findAllEnabledAcrossTenants()).thenReturn(listOf(server))
        whenever(registry.find("generic_http_json")).thenReturn(connector("generic_http_json", HostingType.SERVER_SIDE))
        whenever(serverExecutor.runCycle(any()))
            .thenReturn(ServerSyncOutcome(listOf(EntitySyncOutcome("customer", error = "network down"))))

        scheduler().runDueInstallations()

        verify(syncStateService).recordRun(eq("INST-generic_http_json"), check<SyncRunDto> {
            assert(it.status == "FAILED")
            assert(it.entityType == "customer")
            assert(it.errorDetail == "network down")
        })
    }

    @Test
    fun `records a FAILED run when the whole cycle throws`() {
        val server = installation("generic_http_json", "W1")
        whenever(installationRepository.findAllEnabledAcrossTenants()).thenReturn(listOf(server))
        whenever(registry.find("generic_http_json")).thenReturn(connector("generic_http_json", HostingType.SERVER_SIDE))
        whenever(serverExecutor.runCycle(any())).thenThrow(RuntimeException("boom"))

        scheduler().runDueInstallations()

        verify(syncStateService).recordRun(eq("INST-generic_http_json"), check<SyncRunDto> {
            assert(it.status == "FAILED")
            assert(it.errorDetail == "boom")
        })
    }
}
