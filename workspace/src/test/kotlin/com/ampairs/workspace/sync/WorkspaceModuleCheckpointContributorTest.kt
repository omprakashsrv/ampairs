package com.ampairs.workspace.sync

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceModuleCheckpointContributorTest {

    @Mock private lateinit var workspaceModuleRepository: WorkspaceModuleRepository

    private fun contributor() = WorkspaceModuleCheckpointContributor(workspaceModuleRepository)

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `checkpoints reports module max updatedAt for current workspace`() {
        TenantContextHolder.setCurrentTenant("WSP-1")
        val moduleAt = Instant.parse("2026-06-01T10:00:00Z")
        whenever(workspaceModuleRepository.findMaxUpdatedAtByWorkspaceId("WSP-1")).thenReturn(moduleAt)

        val checkpoints = contributor().checkpoints()

        assertEquals(setOf("module"), checkpoints.keys)
        assertEquals(moduleAt, checkpoints["module"])
    }

    @Test
    fun `checkpoints reports null module when workspace has no installed modules`() {
        TenantContextHolder.setCurrentTenant("WSP-1")
        whenever(workspaceModuleRepository.findMaxUpdatedAtByWorkspaceId("WSP-1")).thenReturn(null)

        val checkpoints = contributor().checkpoints()

        assertEquals(setOf("module"), checkpoints.keys)
        assertNull(checkpoints["module"])
    }

    @Test
    fun `checkpoints reports null module when no tenant context`() {
        val checkpoints = contributor().checkpoints()

        assertEquals(setOf("module"), checkpoints.keys)
        assertNull(checkpoints["module"])
    }
}
