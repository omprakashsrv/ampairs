package com.ampairs.connector.service

import com.ampairs.connector.domain.catalogue.CatalogueConnector
import com.ampairs.connector.domain.catalogue.ConnectorCatalogueRegistry
import com.ampairs.connector.domain.catalogue.HostingType
import com.ampairs.connector.domain.dto.ConnectionTestRequest
import com.ampairs.connector.domain.dto.ConnectionTestResponse
import com.ampairs.connector.domain.model.ConnectorInstallation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

/** Unit tests for connection-test routing by hosting type (FR-009 / FR-S06). */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectorConnectionTesterTest {

    @Mock private lateinit var installationService: ConnectorInstallationService
    @Mock private lateinit var registry: ConnectorCatalogueRegistry
    @Mock private lateinit var configService: ConnectorConfigService

    private val executor = mock<ServerSideConnectorSyncExecutor> { on { connectorType } doReturn "generic_http_json" }

    private fun tester() = ConnectorConnectionTester(installationService, registry, configService, listOf(executor))

    private fun install(type: String) = ConnectorInstallation().apply { uid = "INST1"; connectorType = type }
    private fun connector(type: String, hosting: HostingType) = CatalogueConnector(
        type = type, displayName = type, description = "", hostingType = hosting, supportedEntities = emptyList(),
    )
    private val okResp = ConnectionTestResponse(true, null, Instant.now())

    @Test
    fun `server-side reachable probe records a successful test`() {
        whenever(installationService.getInstallation("INST1")).thenReturn(install("generic_http_json"))
        whenever(registry.find("generic_http_json")).thenReturn(connector("generic_http_json", HostingType.SERVER_SIDE))
        whenever(executor.testConnection(any())).thenReturn(true)
        whenever(configService.recordConnectionTest(eq("INST1"), eq(true), any())).thenReturn(okResp)

        tester().test("INST1", ConnectionTestRequest(ok = false)) // client-reported ok is ignored for server-side

        verify(configService).recordConnectionTest(eq("INST1"), eq(true), any())
    }

    @Test
    fun `server-side unreachable probe records a failed test`() {
        whenever(installationService.getInstallation("INST1")).thenReturn(install("generic_http_json"))
        whenever(registry.find("generic_http_json")).thenReturn(connector("generic_http_json", HostingType.SERVER_SIDE))
        whenever(executor.testConnection(any())).thenReturn(false)
        whenever(configService.recordConnectionTest(eq("INST1"), eq(false), any())).thenReturn(okResp)

        tester().test("INST1", ConnectionTestRequest(ok = true))

        verify(configService).recordConnectionTest(eq("INST1"), eq(false), any())
    }

    @Test
    fun `client-side records the client-reported result`() {
        whenever(installationService.getInstallation("INST1")).thenReturn(install("tally"))
        whenever(registry.find("tally")).thenReturn(connector("tally", HostingType.CLIENT_SIDE))
        whenever(configService.recordConnectionTest(eq("INST1"), eq(true), any())).thenReturn(okResp)

        tester().test("INST1", ConnectionTestRequest(ok = true, message = "pinged locally"))

        verify(configService).recordConnectionTest("INST1", true, "pinged locally")
        verify(executor, never()).testConnection(any())
    }
}
