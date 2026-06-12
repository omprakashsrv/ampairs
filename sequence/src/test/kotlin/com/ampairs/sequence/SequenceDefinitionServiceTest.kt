package com.ampairs.sequence

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sequence.domain.dto.SequenceDefinitionRequest
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.domain.model.SequenceScope
import com.ampairs.sequence.exception.DuplicateSequenceDefinitionException
import com.ampairs.sequence.exception.InvalidSequenceRequestException
import com.ampairs.sequence.repository.SequenceDefinitionRepository
import com.ampairs.sequence.service.SequenceCounterService
import com.ampairs.sequence.service.SequenceDefinitionServiceImpl
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
class SequenceDefinitionServiceTest {

    @Mock
    private lateinit var definitionRepository: SequenceDefinitionRepository

    @Mock
    private lateinit var entityChangePublisher: EntityChangePublisher

    private lateinit var counterService: SequenceCounterService
    private lateinit var service: SequenceDefinitionServiceImpl

    private fun definition(block: SequenceDefinition.() -> Unit = {}): SequenceDefinition =
        SequenceDefinition().apply {
            uid = "SQD-TEST"
            entityType = "invoice"
            scope = SequenceScope.WORKSPACE
            prefix = "INV"
            startValue = 1
            incrementStep = 1
            currentValue = 1250
            active = true
            block()
        }

    @BeforeEach
    fun setUp() {
        counterService = SequenceCounterService(definitionRepository)
        service = SequenceDefinitionServiceImpl(definitionRepository, counterService, entityChangePublisher)
        whenever(definitionRepository.save(any<SequenceDefinition>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `next advances counter and formats from the workspace definition`() {
        val def = definition()
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(def)
        whenever(definitionRepository.findByUidForUpdate("SQD-TEST")).thenReturn(def)

        val result = service.next("invoice", userId = "USR-1")

        assertEquals(1251, result.value)
        assertEquals("INV-1251", result.formatted)
        assertEquals(1251, def.currentValue)
    }

    @Test
    fun `next prefers the caller's user-scoped definition`() {
        val userDef = definition {
            uid = "SQD-USER"
            scope = SequenceScope.USER
            userId = "USR-1"
            prefix = "AINV"
            currentValue = 1000
        }
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndUserIdAndActiveTrue(
                "invoice", SequenceScope.USER, "USR-1"
            )
        ).thenReturn(userDef)
        whenever(definitionRepository.findByUidForUpdate("SQD-USER")).thenReturn(userDef)

        val result = service.next("invoice", userId = "USR-1")

        assertEquals("AINV-1001", result.formatted)
    }

    @Test
    fun `next auto-provisions a default definition when none exists`() {
        whenever(
            definitionRepository.findFirstByEntityTypeAndScopeAndActiveTrue("invoice", SequenceScope.WORKSPACE)
        ).thenReturn(null)
        whenever(definitionRepository.save(any<SequenceDefinition>())).thenAnswer { invocation ->
            (invocation.arguments[0] as SequenceDefinition).also { saved ->
                if (saved.uid.isBlank()) saved.uid = "SQD-NEW"
                whenever(definitionRepository.findByUidForUpdate(saved.uid)).thenReturn(saved)
            }
        }

        val result = service.next("invoice", userId = null)

        assertEquals(1, result.value)
        assertEquals("INV-1", result.formatted)
    }

    @Test
    fun `preview does not advance the counter`() {
        val def = definition()
        whenever(definitionRepository.findByUid("SQD-TEST")).thenReturn(def)

        val preview = service.preview("SQD-TEST")

        assertEquals(1251, preview.nextValue)
        assertEquals("INV-1251", preview.formatted)
        assertEquals(1250, def.currentValue)
    }

    @Test
    fun `create rejects a second active definition for the same key`() {
        whenever(
            definitionRepository.existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
                "invoice", SequenceScope.WORKSPACE, ""
            )
        ).thenReturn(true)

        assertThrows(DuplicateSequenceDefinitionException::class.java) {
            service.create(SequenceDefinitionRequest(entityType = "invoice", prefix = "INV"))
        }
    }

    @Test
    fun `create requires user_id for user scope`() {
        assertThrows(InvalidSequenceRequestException::class.java) {
            service.create(SequenceDefinitionRequest(entityType = "invoice", scope = SequenceScope.USER))
        }
    }

    @Test
    fun `update rejects lowering the counter`() {
        val def = definition()
        whenever(definitionRepository.findByUid("SQD-TEST")).thenReturn(def)
        whenever(
            definitionRepository.existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
                "invoice", SequenceScope.WORKSPACE, "SQD-TEST"
            )
        ).thenReturn(false)

        assertThrows(InvalidSequenceRequestException::class.java) {
            service.update(
                "SQD-TEST",
                SequenceDefinitionRequest(entityType = "invoice", prefix = "INV", currentValue = 100)
            )
        }
    }

    @Test
    fun `update allows fast-forwarding the counter`() {
        val def = definition()
        whenever(definitionRepository.findByUid("SQD-TEST")).thenReturn(def)
        whenever(
            definitionRepository.existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
                "invoice", SequenceScope.WORKSPACE, "SQD-TEST"
            )
        ).thenReturn(false)

        val response = service.update(
            "SQD-TEST",
            SequenceDefinitionRequest(entityType = "invoice", prefix = "INV", currentValue = 9000)
        )

        assertEquals(9000, response.currentValue)
    }

    @Test
    fun `bulk upsert silently keeps the server counter when the client value is lower`() {
        val def = definition()
        whenever(definitionRepository.findByUid("SQD-TEST")).thenReturn(def)
        whenever(
            definitionRepository.existsByEntityTypeAndScopeAndActiveTrueAndUidNot(
                "invoice", SequenceScope.WORKSPACE, "SQD-TEST"
            )
        ).thenReturn(false)

        val responses = service.bulkUpsert(
            listOf(SequenceDefinitionRequest(uid = "SQD-TEST", entityType = "invoice", prefix = "INV", currentValue = 5))
        )

        assertEquals(1250, responses.single().currentValue)
    }
}
