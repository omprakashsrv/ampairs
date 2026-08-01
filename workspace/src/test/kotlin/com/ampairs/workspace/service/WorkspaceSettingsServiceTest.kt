package com.ampairs.workspace.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.MaterialColors
import com.ampairs.workspace.model.MaterialTheme
import com.ampairs.workspace.model.MaterialTypography
import com.ampairs.workspace.model.WorkspaceSettings
import com.ampairs.workspace.model.dto.UpdateSettingsRequest
import com.ampairs.workspace.repository.WorkspaceSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceSettingsServiceTest {

    @Mock
    private lateinit var settingsRepository: WorkspaceSettingsRepository

    @Mock
    private lateinit var activityService: WorkspaceActivityService

    private lateinit var service: WorkspaceSettingsService

    private fun settings(block: WorkspaceSettings.() -> Unit = {}): WorkspaceSettings =
        WorkspaceSettings().apply {
            uid = "WST-1"
            workspaceId = "WSP-1"
            createdAt = Instant.now()
            updatedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceSettingsService(settingsRepository, activityService)
        whenever(settingsRepository.save(any<WorkspaceSettings>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `initializeDefaultSettings saves new settings with workspace id`() {
        var saved: WorkspaceSettings? = null
        whenever(settingsRepository.save(any<WorkspaceSettings>())).thenAnswer {
            saved = it.arguments[0] as WorkspaceSettings
            saved
        }

        val result = service.initializeDefaultSettings("WSP-1")

        assertEquals("WSP-1", result.workspaceId)
        assertNull(saved!!.logoUrl)
    }

    @Test
    fun `getWorkspaceSettings maps to response`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings { logoUrl = "logo.png" }))

        val response = service.getWorkspaceSettings("WSP-1")

        assertEquals("WST-1", response.id)
        assertEquals("WSP-1", response.workspaceId)
        assertEquals("logo.png", response.logoUrl)
    }

    @Test
    fun `getWorkspaceSettings throws when not found`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-X")).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.getWorkspaceSettings("WSP-X") }
    }

    @Test
    fun `updateSettings applies non-null fields`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))
        val theme = MaterialTheme(mode = "dark")

        val response = service.updateSettings(
            "WSP-1",
            UpdateSettingsRequest(logoUrl = "new.png", materialTheme = theme),
            "USR-1",
        )

        assertEquals("new.png", response.logoUrl)
        assertEquals("dark", response.materialTheme.mode)
        assertEquals("USR-1", response.lastModifiedBy)
    }

    @Test
    fun `updateMaterialTheme sets theme and logs activity`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.updateMaterialTheme("WSP-1", MaterialTheme(mode = "dark"), "USR-1")

        assertEquals("dark", response.materialTheme.mode)
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("material_theme"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateMaterialColors sets colors and logs`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.updateMaterialColors("WSP-1", MaterialColors(primary = "#000000"), "USR-1")

        assertEquals("#000000", response.materialColors.primary)
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("material_colors"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateMaterialTypography sets typography and logs`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.updateMaterialTypography("WSP-1", MaterialTypography(bodyFont = "Inter"), "USR-1")

        assertEquals("Inter", response.materialTypography.bodyFont)
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("material_typography"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateBusinessSettings replaces business settings`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))
        val newSettings = mapOf<String, Any>("foo" to "bar")

        val response = service.updateBusinessSettings("WSP-1", newSettings, "USR-1")

        assertEquals("bar", response.businessSettings["foo"])
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("business_settings"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateLogo updates logo url`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.updateLogo("WSP-1", "brand.png", "USR-1")

        assertEquals("brand.png", response.logoUrl)
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("logo"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `updateBusinessSetting adds a single key`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.updateBusinessSetting("WSP-1", "taxMode", "inclusive", "USR-1")

        assertEquals("inclusive", response.businessSettings["taxMode"])
        verify(activityService).logSettingsUpdated(eq("WSP-1"), eq("business_setting_taxMode"), eq("USR-1"), any(), anyOrNull())
    }

    @Test
    fun `resetSettings material_theme resets to defaults`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1"))
            .thenReturn(Optional.of(settings { materialTheme = MaterialTheme(mode = "dark") }))

        val response = service.resetSettings("WSP-1", "material_theme", "USR-1")

        assertEquals("light", response.materialTheme.mode)
        verify(activityService).logSettingsReset(eq("WSP-1"), eq("material_theme"), eq("USR-1"), any())
    }

    @Test
    fun `resetSettings business_settings resets to defaults`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        val response = service.resetSettings("WSP-1", "business_settings", "USR-1")

        @Suppress("UNCHECKED_CAST")
        val prefixes = response.businessSettings["prefixes"] as Map<String, Any>
        assertEquals("INV", prefixes["invoice"])
    }

    @Test
    fun `resetSettings all (null section) resets everything`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1"))
            .thenReturn(Optional.of(settings { logoUrl = "x.png"; materialTheme = MaterialTheme(mode = "dark") }))

        val response = service.resetSettings("WSP-1", null, "USR-1")

        assertNull(response.logoUrl)
        assertEquals("light", response.materialTheme.mode)
        verify(activityService).logSettingsReset(eq("WSP-1"), eq("all"), eq("USR-1"), any())
    }

    @Test
    fun `resetSettings rejects invalid section`() {
        whenever(settingsRepository.findByWorkspaceId("WSP-1")).thenReturn(Optional.of(settings()))

        assertThrows(IllegalArgumentException::class.java) {
            service.resetSettings("WSP-1", "nonsense", "USR-1")
        }
    }

    @Test
    fun `deleteSettings delegates to repository`() {
        service.deleteSettings("WSP-1")
        verify(settingsRepository).deleteByWorkspaceId("WSP-1")
    }

    @Test
    fun `entity helper methods read business settings`() {
        val s = settings()
        assertEquals("09:00", s.getWorkingHoursStart())
        assertEquals("17:00", s.getWorkingHoursEnd())
        assertEquals("INV", s.getInvoicePrefix())
        assertEquals("ORD", s.getOrderPrefix())
        assertEquals(true, s.isAutoGenerateInvoicesEnabled())
        assertEquals(true, s.isAutoGenerateOrdersEnabled())
        assertEquals(5, s.getWorkingDays().size)
        assertNotNull(s.getPrimaryColor())
        assertEquals(false, s.isDarkTheme())
    }
}
