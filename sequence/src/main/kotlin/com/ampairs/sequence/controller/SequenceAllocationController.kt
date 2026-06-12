package com.ampairs.sequence.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.sequence.domain.dto.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationResponse
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.service.SequenceAllocationService
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sequence/v1/allocations")
@Validated
class SequenceAllocationController(
    private val allocationService: SequenceAllocationService,
) {

    /** Grant an exclusive number block to a device (FR-008). */
    @PostMapping
    fun requestAllocation(
        @Valid @RequestBody request: SequenceAllocationRequest,
    ): ApiResponse<SequenceAllocationResponse> {
        return ApiResponse.success(allocationService.allocate(request, currentUserId()))
    }

    /** Bulk consumption report from a device on reconnect (FR-010). */
    @PostMapping("/report")
    fun reportConsumption(
        @Valid @RequestBody requests: List<SequenceAllocationReportRequest>,
    ): ApiResponse<List<SequenceAllocationResponse>> {
        return ApiResponse.success(allocationService.report(requests))
    }

    /** Allocations for a device — recovery after reinstall. */
    @GetMapping
    fun listAllocations(
        @RequestParam("device_id") deviceId: String,
        @RequestParam("status", required = false) status: AllocationStatus?,
    ): ApiResponse<List<SequenceAllocationResponse>> {
        return ApiResponse.success(allocationService.listForDevice(deviceId, status))
    }

    private fun currentUserId(): String? = runCatching {
        SecurityContextHolder.getContext().authentication?.let { AuthenticationHelper.getCurrentUserId(it) }
    }.getOrNull()
}
