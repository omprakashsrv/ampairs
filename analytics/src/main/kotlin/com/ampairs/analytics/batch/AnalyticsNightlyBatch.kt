package com.ampairs.analytics.batch

import com.ampairs.analytics.service.ForecastService
import com.ampairs.analytics.service.KpiRollupService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.repository.WorkspaceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Nightly cross-tenant maintenance (R2/P2): for every active workspace, reconcile the trailing KPI
 * window from source (self-healing against missed events/backdated edits) and recompute demand
 * forecasts. This is an **entry point** (like the event listener), so it sets the tenant context per
 * workspace in a try/finally — services never touch `TenantContextHolder` themselves.
 *
 * `Workspace` is the tenant registry (not tenant-scoped), so enumeration uses a native query.
 */
@Component
class AnalyticsNightlyBatch(
    private val workspaceRepository: WorkspaceRepository,
    private val rollupService: KpiRollupService,
    private val forecastService: ForecastService,
    @Value("\${analytics.reconcile.trailing-days:7}") private val trailingDays: Int,
    @Value("\${analytics.forecast.lookback-days:90}") private val lookbackDays: Int,
    @Value("\${analytics.forecast.horizon-days:7}") private val horizonDays: Int,
) {
    private val log = LoggerFactory.getLogger(AnalyticsNightlyBatch::class.java)

    @Scheduled(cron = "\${analytics.nightly.cron:0 30 2 * * ?}") // daily at 02:30
    fun runNightly() = runForAllWorkspaces()

    /** Visible for testing — iterate active workspaces, reconciling + forecasting each under its tenant. */
    fun runForAllWorkspaces() {
        val workspaceUids = workspaceRepository.findActiveWorkspaceUids()
        log.info("Analytics nightly batch starting for {} workspaces", workspaceUids.size)
        for (uid in workspaceUids) {
            TenantContextHolder.setCurrentTenant(uid)
            try {
                rollupService.reconcileTrailing(trailingDays)
                forecastService.recompute(lookbackDays, horizonDays)
            } catch (e: Exception) {
                // One workspace failing must not abort the rest of the run.
                log.error("Analytics nightly batch failed for workspace {}: {}", uid, e.message, e)
            } finally {
                TenantContextHolder.clearTenantContext()
            }
        }
    }
}
