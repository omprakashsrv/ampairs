package com.ampairs.sequence.service

import com.ampairs.sequence.domain.dto.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationResponse
import com.ampairs.sequence.domain.model.AllocationStatus

interface SequenceAllocationService {

    /**
     * Grants an exclusive contiguous block to a device (FR-008): resolves the definition
     * for (entityType, caller), atomically advances its counter past the range and persists
     * the allocation with a format snapshot.
     */
    fun allocate(request: SequenceAllocationRequest, userId: String?): SequenceAllocationResponse

    /**
     * Records device consumption progress (FR-010). Progress only moves forward —
     * regressions are ignored and the response carries the server state.
     */
    fun report(requests: List<SequenceAllocationReportRequest>): List<SequenceAllocationResponse>

    /** Allocations for a device (recovery after reinstall), optionally filtered by status. */
    fun listForDevice(deviceId: String, status: AllocationStatus?): List<SequenceAllocationResponse>
}
