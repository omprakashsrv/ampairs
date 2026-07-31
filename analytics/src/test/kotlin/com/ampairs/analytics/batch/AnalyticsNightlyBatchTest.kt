package com.ampairs.analytics.batch

import com.ampairs.analytics.service.ForecastService
import com.ampairs.analytics.service.KpiRollupService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.repository.WorkspaceRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for the cross-tenant nightly batch: it must run reconcile + forecast for each active
 * workspace under that workspace's tenant context, clear the context afterwards, and not let one
 * workspace's failure abort the rest.
 */
class AnalyticsNightlyBatchTest {

    private val workspaceRepository = mock<WorkspaceRepository>()
    private val rollupService = mock<KpiRollupService>()
    private val forecastService = mock<ForecastService>()

    private fun batch() = AnalyticsNightlyBatch(
        workspaceRepository, rollupService, forecastService,
        trailingDays = 7, lookbackDays = 90, horizonDays = 7,
    )

    @AfterEach
    fun cleanup() = TenantContextHolder.clearTenantContext()

    @Test
    fun `runs reconcile and forecast per workspace under its tenant, then clears context`() {
        whenever(workspaceRepository.findActiveWorkspaceUids()).thenReturn(listOf("WS1", "WS2"))
        val reconcileTenants = mutableListOf<String?>()
        val forecastTenants = mutableListOf<String?>()
        whenever(rollupService.reconcileTrailing(any())).thenAnswer {
            reconcileTenants.add(TenantContextHolder.getCurrentTenant()); 0
        }
        whenever(forecastService.recompute(any(), any())).thenAnswer {
            forecastTenants.add(TenantContextHolder.getCurrentTenant()); 0
        }

        batch().runForAllWorkspaces()

        verify(rollupService, times(2)).reconcileTrailing(eq(7))
        verify(forecastService, times(2)).recompute(eq(90), eq(7))
        assertEquals(listOf("WS1", "WS2"), reconcileTenants)
        assertEquals(listOf("WS1", "WS2"), forecastTenants)
        assertNull(TenantContextHolder.getCurrentTenant(), "tenant context must be cleared after the run")
    }

    @Test
    fun `one workspace failing does not abort the others`() {
        whenever(workspaceRepository.findActiveWorkspaceUids()).thenReturn(listOf("WS1", "WS2"))
        whenever(rollupService.reconcileTrailing(any())).thenAnswer {
            if (TenantContextHolder.getCurrentTenant() == "WS1") throw RuntimeException("boom") else 0
        }
        whenever(forecastService.recompute(any(), any())) doReturn 0

        batch().runForAllWorkspaces()

        // WS2 still reconciled+forecast despite WS1 throwing; WS1's forecast is skipped.
        verify(rollupService, times(2)).reconcileTrailing(eq(7))
        verify(forecastService, times(1)).recompute(eq(90), eq(7))
        assertNull(TenantContextHolder.getCurrentTenant())
    }
}
