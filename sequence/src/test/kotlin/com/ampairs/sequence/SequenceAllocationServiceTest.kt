package com.ampairs.sequence

import com.ampairs.sequence.domain.dto.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.dto.SequenceAllocationRequest
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.exception.InactiveSequenceDefinitionException
import com.ampairs.sequence.repository.SequenceAllocationRepository
import com.ampairs.sequence.repository.SequenceDefinitionRepository
import com.ampairs.sequence.service.SequenceAllocationServiceImpl
import com.ampairs.sequence.service.SequenceCounterService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SequenceAllocationServiceTest {

    @Mock
    private lateinit var definitionRepository: SequenceDefinitionRepository

    @Mock
    private lateinit var allocationRepository: SequenceAllocationRepository

    private lateinit var service: SequenceAllocationServiceImpl

    private fun definition(block: SequenceDefinition.() -> Unit = {}): SequenceDefinition =
        SequenceDefinition().apply {
            uid = "SQD-TEST"
            entityType = "invoice"
            scope = SequenceScope.WORKSPACE
            prefix = "INV"
            startValue = 1001
            incrementStep = 1
            currentValue = 0
            active = true
            block()
        }

    @BeforeEach
    fun setUp() {
        service = SequenceAllocationServiceImpl(allocationRepository, SequenceCounterService(definitionRepository))
        whenever(definitionRepository.save(any<SequenceDefinition>())).thenAnswer { it.arguments[0] }
        whenever(allocationRepository.save(any<SequenceAllocation>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `allocate grants an exclusive range and advances the high-water mark`() {
        val def = definition()
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(def)
        whenever(definitionRepository.findByUidForUpdate("SQD-TEST")).thenReturn(def)

        val response = service.allocate(
            SequenceAllocationRequest(entityType = "invoice", deviceId = "DEV-A", blockSize = 50),
            userId = "USR-1",
        )

        assertEquals(1001, response.rangeStart)
        assertEquals(1050, response.rangeEnd)
        assertEquals(1001, response.nextAvailable)
        assertEquals(AllocationStatus.ACTIVE, response.status)
        assertEquals("INV", response.prefix)
        assertEquals(1050, def.currentValue)
    }

    @Test
    fun `consecutive allocations for two devices receive disjoint ranges`() {
        val def = definition()
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(def)
        whenever(definitionRepository.findByUidForUpdate("SQD-TEST")).thenReturn(def)

        val first = service.allocate(SequenceAllocationRequest("invoice", "DEV-A", 50), null)
        val second = service.allocate(SequenceAllocationRequest("invoice", "DEV-B", 50), null)

        assertEquals(1001L..1050L, first.rangeStart..first.rangeEnd)
        assertEquals(1051L..1100L, second.rangeStart..second.rangeEnd)
    }

    @Test
    fun `allocation range honors the increment step`() {
        val def = definition { startValue = 10; incrementStep = 5; currentValue = 10 }
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(def)
        whenever(definitionRepository.findByUidForUpdate("SQD-TEST")).thenReturn(def)

        val response = service.allocate(SequenceAllocationRequest("invoice", "DEV-A", 10), null)

        assertEquals(15, response.rangeStart)
        assertEquals(60, response.rangeEnd)
    }

    @Test
    fun `allocate refuses an inactive definition`() {
        val def = definition { active = false }
        // resolve skips inactive rows, but a direct race can still hit the lock-time check:
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(def)
        whenever(definitionRepository.findByUidForUpdate("SQD-TEST")).thenReturn(def)

        assertThrows(InactiveSequenceDefinitionException::class.java) {
            service.allocate(SequenceAllocationRequest("invoice", "DEV-A", 50), null)
        }
    }

    private fun allocation(block: SequenceAllocation.() -> Unit = {}): SequenceAllocation =
        SequenceAllocation().apply {
            uid = "SQA-TEST"
            definitionUid = "SQD-TEST"
            entityType = "invoice"
            deviceId = "DEV-A"
            rangeStart = 1001
            rangeEnd = 1050
            nextAvailable = 1001
            incrementStep = 1
            status = AllocationStatus.ACTIVE
            block()
        }

    @Test
    fun `report moves progress forward`() {
        val alloc = allocation()
        whenever(allocationRepository.findByUid("SQA-TEST")).thenReturn(alloc)

        val response = service.report(listOf(SequenceAllocationReportRequest("SQA-TEST", 1010))).single()

        assertEquals(1010, response.nextAvailable)
        assertEquals(AllocationStatus.ACTIVE, response.status)
    }

    @Test
    fun `report ignores regressions`() {
        val alloc = allocation { nextAvailable = 1020 }
        whenever(allocationRepository.findByUid("SQA-TEST")).thenReturn(alloc)

        val response = service.report(listOf(SequenceAllocationReportRequest("SQA-TEST", 1005))).single()

        assertEquals(1020, response.nextAvailable)
    }

    @Test
    fun `report clamps past-the-end progress and flips status to exhausted`() {
        val alloc = allocation { nextAvailable = 1050 }
        whenever(allocationRepository.findByUid("SQA-TEST")).thenReturn(alloc)

        val response = service.report(listOf(SequenceAllocationReportRequest("SQA-TEST", 9999))).single()

        assertEquals(1051, response.nextAvailable)
        assertEquals(AllocationStatus.EXHAUSTED, response.status)
    }
}
