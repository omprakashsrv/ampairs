package com.ampairs.sequence.service

import com.ampairs.sequence.config.Constants
import com.ampairs.sequence.domain.dto.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationResponse
import com.ampairs.sequence.domain.dto.asSequenceAllocationResponse
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.exception.SequenceAllocationNotFoundException
import com.ampairs.sequence.repository.SequenceAllocationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SequenceAllocationServiceImpl(
    private val allocationRepository: SequenceAllocationRepository,
    private val counterService: SequenceCounterService,
) : SequenceAllocationService {

    private val logger = LoggerFactory.getLogger(SequenceAllocationServiceImpl::class.java)

    @Transactional
    override fun allocate(request: SequenceAllocationRequest, userId: String?): SequenceAllocationResponse {
        val resolved = counterService.resolveOrProvision(request.entityType, userId)
        val blockSize = request.blockSize ?: Constants.DEFAULT_BLOCK_SIZE
        val issued = counterService.advance(resolved.uid, blockSize)
        val definition = issued.definition

        val allocation = SequenceAllocation().apply {
            definitionUid = definition.uid
            entityType = definition.entityType
            deviceId = request.deviceId
            this.userId = userId?.takeIf { it.isNotBlank() }
            rangeStart = issued.first
            rangeEnd = issued.last
            nextAvailable = issued.first
            status = AllocationStatus.ACTIVE
            prefix = definition.prefix
            suffix = definition.suffix
            paddingLength = definition.paddingLength
            incrementStep = definition.incrementStep
        }
        val saved = allocationRepository.save(allocation)
        logger.info(
            "Allocated sequence block {}..{} of '{}' ({}) to device {}",
            issued.first, issued.last, definition.entityType, definition.uid, request.deviceId
        )
        return saved.asSequenceAllocationResponse()
    }

    @Transactional
    override fun report(requests: List<SequenceAllocationReportRequest>): List<SequenceAllocationResponse> {
        return requests.map { request ->
            val allocation = allocationRepository.findByUid(request.uid)
                ?: throw SequenceAllocationNotFoundException("Sequence allocation not found for uid: ${request.uid}")

            // Forward-only, clamped to one step past the end of the range (= fully consumed)
            val reported = request.nextAvailable ?: allocation.nextAvailable
            val ceiling = allocation.rangeEnd + allocation.incrementStep
            val newNext = reported.coerceAtMost(ceiling)
            if (newNext > allocation.nextAvailable) {
                allocation.nextAvailable = newNext
                if (newNext > allocation.rangeEnd) {
                    allocation.status = AllocationStatus.EXHAUSTED
                }
                allocationRepository.save(allocation)
            }
            allocation.asSequenceAllocationResponse()
        }
    }

    @Transactional(readOnly = true)
    override fun listForDevice(deviceId: String, status: AllocationStatus?): List<SequenceAllocationResponse> {
        val allocations = if (status != null) {
            allocationRepository.findByDeviceIdAndStatusOrderByCreatedAtDesc(deviceId, status)
        } else {
            allocationRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId)
        }
        return allocations.map { it.asSequenceAllocationResponse() }
    }
}
