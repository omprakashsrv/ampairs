package com.ampairs.sequence.repository

import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocation
import org.springframework.data.repository.CrudRepository

interface SequenceAllocationRepository : CrudRepository<SequenceAllocation, Long> {

    fun findByUid(uid: String?): SequenceAllocation?

    fun findByDeviceIdOrderByCreatedAtDesc(deviceId: String): List<SequenceAllocation>

    fun findByDeviceIdAndStatusOrderByCreatedAtDesc(
        deviceId: String,
        status: AllocationStatus,
    ): List<SequenceAllocation>
}
