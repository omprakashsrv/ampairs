package com.ampairs.workspace.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.ModuleConfiguration
import com.ampairs.workspace.model.WorkspaceModule
import com.ampairs.workspace.model.dto.ModuleReorderItem
import com.ampairs.workspace.model.enums.ModuleStatus
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.ampairs.workspace.repository.MasterModuleRepository
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceModuleServiceTest {

    @Mock
    private lateinit var workspaceModuleRepository: WorkspaceModuleRepository

    @Mock
    private lateinit var masterModuleRepository: MasterModuleRepository

    private lateinit var service: WorkspaceModuleService

    private fun masterModule(block: MasterModule.() -> Unit = {}): MasterModule =
        MasterModule().apply {
            id = 1
            uid = "MMD-1"
            moduleCode = "customer-management"
            name = "Customer Management"
            description = "Manage customers"
            version = "1.0.0"
            status = ModuleStatus.ACTIVE
            active = true
            block()
        }

    private fun workspaceModule(master: MasterModule, block: WorkspaceModule.() -> Unit = {}): WorkspaceModule =
        WorkspaceModule().apply {
            uid = "WMD-1"
            workspaceId = "WSP-1"
            masterModule = master
            status = WorkspaceModuleStatus.ACTIVE
            enabled = true
            installedVersion = "1.0.0"
            displayOrder = 10
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceModuleService(workspaceModuleRepository, masterModuleRepository)
        whenever(workspaceModuleRepository.save(any<WorkspaceModule>())).thenAnswer { it.arguments[0] }
        whenever(masterModuleRepository.save(any<MasterModule>())).thenAnswer { it.arguments[0] }
        TenantContextHolder.setCurrentTenant("WSP-1")
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
    }

    @Test
    fun `getInstalledModules returns active modules`() {
        val m = masterModule()
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1")).thenReturn(listOf(workspaceModule(m)))

        val result = service.getInstalledModules()

        assertEquals(1, result.size)
        assertEquals("customer-management", result.first().moduleCode)
        assertEquals("Customer Management", result.first().name)
    }

    @Test
    fun `getInstalledModules returns empty when no tenant`() {
        TenantContextHolder.clearTenantContext()

        assertTrue(service.getInstalledModules().isEmpty())
    }

    @Test
    fun `getInstalledModules filters out disabled modules`() {
        val m = masterModule()
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1"))
            .thenReturn(listOf(workspaceModule(m) { enabled = false }))

        assertTrue(service.getInstalledModules().isEmpty())
    }

    @Test
    fun `getModuleInfo returns detail for installed module`() {
        val m = masterModule()
        whenever(masterModuleRepository.findByModuleCode("customer-management")).thenReturn(m)
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(workspaceModule(m))

        val detail = service.getModuleInfo("customer-management")!!

        assertEquals("Customer Management", detail.moduleInfo.name)
        assertTrue(detail.permissions.canConfigure)
    }

    @Test
    fun `getModuleInfo returns null for unknown module`() {
        whenever(masterModuleRepository.findByModuleCode("unknown")).thenReturn(null)

        assertNull(service.getModuleInfo("unknown"))
    }

    @Test
    fun `installModule installs a new module`() {
        val m = masterModule()
        whenever(workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(false)
        whenever(masterModuleRepository.findByModuleCode("customer-management")).thenReturn(m)
        whenever(workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue("WSP-1")).thenReturn(emptyList())
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1")).thenReturn(emptyList())

        val result = service.installModule("customer-management", "USR-1", "Jane")

        assertTrue(result.success)
        assertEquals("customer-management", result.moduleCode)
        verify(masterModuleRepository).save(any<MasterModule>())
    }

    @Test
    fun `installModule returns success when already installed`() {
        val m = masterModule()
        whenever(workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(true)
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(workspaceModule(m))

        val result = service.installModule("customer-management")

        assertTrue(result.success)
        assertTrue(result.message.contains("already installed"))
    }

    @Test
    fun `installModule fails when module not in catalog`() {
        whenever(workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "ghost")).thenReturn(false)
        whenever(masterModuleRepository.findByModuleCode("ghost")).thenReturn(null)

        val result = service.installModule("ghost")

        assertFalse(result.success)
        assertTrue(result.message.contains("not found in catalog"))
    }

    @Test
    fun `installModule fails when not production ready`() {
        val m = masterModule { status = ModuleStatus.DEPRECATED }
        whenever(workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(false)
        whenever(masterModuleRepository.findByModuleCode("customer-management")).thenReturn(m)

        val result = service.installModule("customer-management")

        assertFalse(result.success)
        assertTrue(result.message.contains("not ready for production"))
    }

    @Test
    fun `installModule fails when dependencies missing`() {
        val m = masterModule {
            configuration = ModuleConfiguration(dependencies = listOf("billing"))
        }
        whenever(workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(false)
        whenever(masterModuleRepository.findByModuleCode("customer-management")).thenReturn(m)
        whenever(workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue("WSP-1")).thenReturn(emptyList())

        val result = service.installModule("customer-management")

        assertFalse(result.success)
        assertTrue(result.message.contains("Missing required dependencies"))
    }

    @Test
    fun `installModule fails when there is no tenant`() {
        TenantContextHolder.clearTenantContext()

        val result = service.installModule("customer-management")

        assertFalse(result.success)
        assertTrue(result.message.contains("No workspace context"))
    }

    @Test
    fun `uninstallModule removes an installed module`() {
        val m = masterModule()
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(workspaceModule(m))
        whenever(workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue("WSP-1")).thenReturn(emptyList())

        val result = service.uninstallModule("customer-management")

        assertTrue(result.success)
        verify(workspaceModuleRepository).delete(any<WorkspaceModule>())
        verify(masterModuleRepository).save(any<MasterModule>())
    }

    @Test
    fun `uninstallModule fails when module not found`() {
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(null)

        val result = service.uninstallModule("customer-management")

        assertFalse(result.success)
        verify(workspaceModuleRepository, never()).delete(any<WorkspaceModule>())
    }

    @Test
    fun `uninstallModule fails when other modules depend on it`() {
        val m = masterModule()
        val dependent = masterModule { id = 2; uid = "MMD-2"; moduleCode = "orders"; name = "Orders" }
        dependent.configuration = ModuleConfiguration(dependencies = listOf("customer-management"))
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(workspaceModule(m))
        whenever(workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue("WSP-1"))
            .thenReturn(listOf(workspaceModule(dependent) { uid = "WMD-2" }))

        val result = service.uninstallModule("customer-management")

        assertFalse(result.success)
        assertTrue(result.message.contains("Required by"))
    }

    @Test
    fun `getAvailableModules excludes installed modules`() {
        val installed = masterModule()
        val available = masterModule { id = 2; uid = "MMD-2"; moduleCode = "orders"; name = "Orders" }
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1")).thenReturn(listOf(workspaceModule(installed)))
        whenever(masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(listOf(installed, available))

        val result = service.getAvailableModules()

        assertEquals(1, result.size)
        assertEquals("orders", result.first().moduleCode)
    }

    @Test
    fun `getAvailableModules returns empty for invalid category`() {
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1")).thenReturn(emptyList())

        assertTrue(service.getAvailableModules(category = "NOT_A_CATEGORY").isEmpty())
    }

    @Test
    fun `getModuleCatalog assembles installed, available, categories and stats`() {
        val installed = masterModule()
        val available = masterModule { id = 2; uid = "MMD-2"; moduleCode = "orders"; name = "Orders" }
        whenever(workspaceModuleRepository.findByWorkspaceId("WSP-1")).thenReturn(listOf(workspaceModule(installed)))
        whenever(masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(listOf(installed, available))

        val catalog = service.getModuleCatalog()

        assertEquals(1, catalog.installedModules.size)
        assertEquals(1, catalog.availableModules.size)
        assertTrue(catalog.categories.isNotEmpty())
        assertEquals(1, catalog.statistics.totalInstalled)
        assertEquals(1, catalog.statistics.enabledModules)
    }

    @Test
    fun `reorderModules updates display order`() {
        val m = masterModule()
        val wm = workspaceModule(m)
        whenever(workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode("WSP-1", "customer-management"))
            .thenReturn(wm)

        service.reorderModules(listOf(ModuleReorderItem("customer-management", 50)))

        assertEquals(50, wm.displayOrder)
        verify(workspaceModuleRepository).save(wm)
    }
}
