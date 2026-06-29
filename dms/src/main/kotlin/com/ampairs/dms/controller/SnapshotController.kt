package com.ampairs.dms.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.dms.domain.dto.DistributorStockRow
import com.ampairs.dms.domain.dto.SalesTargetRequest
import com.ampairs.dms.domain.dto.SalesTargetResponse
import com.ampairs.dms.domain.dto.SecondarySalesRow
import com.ampairs.dms.domain.dto.asResponse
import com.ampairs.dms.domain.dto.toEntity
import com.ampairs.dms.domain.service.SnapshotService
import com.ampairs.dms.domain.service.TargetService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Brand DMS reads (pull-only over published snapshots) + targets. Every cross-tenant read is gated by
 * the trade-module CrossTenantReadGuard (an ACCEPTED, sufficiently-scoped link), else 403. Base `/dms/v1`.
 */
@RestController
@RequestMapping("/dms/v1")
class SnapshotController(
    private val snapshotService: SnapshotService,
    private val targetService: TargetService,
) {

    @GetMapping("/snapshots/secondary-sales")
    fun secondarySales(
        @RequestParam("brand_workspace_id") brandWorkspaceId: String,
        @RequestParam("distributor_workspace_id") distributorWorkspaceId: String,
    ): ApiResponse<List<SecondarySalesRow>> =
        ApiResponse.success(snapshotService.readSecondarySales(brandWorkspaceId, distributorWorkspaceId))

    @GetMapping("/snapshots/distributor-stock")
    fun distributorStock(
        @RequestParam("brand_workspace_id") brandWorkspaceId: String,
        @RequestParam("distributor_workspace_id") distributorWorkspaceId: String,
    ): ApiResponse<List<DistributorStockRow>> =
        ApiResponse.success(snapshotService.readDistributorStock(brandWorkspaceId, distributorWorkspaceId))

    @PostMapping("/targets")
    fun createTarget(@RequestBody @Valid request: SalesTargetRequest): ApiResponse<SalesTargetResponse> =
        ApiResponse.success(targetService.create(request.toEntity()).asResponse())

    @GetMapping("/targets")
    fun listTargets(
        @RequestParam("brand_workspace_id") brandWorkspaceId: String,
        @RequestParam("distributor_workspace_id", required = false) distributorWorkspaceId: String?,
    ): ApiResponse<List<SalesTargetResponse>> =
        ApiResponse.success(targetService.readTargets(brandWorkspaceId, distributorWorkspaceId).map { it.asResponse() })
}
