package com.ampairs.unit

import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.exception.UnitInUseException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.repository.UnitRepository
import com.ampairs.unit.service.UnitServiceImpl
import com.ampairs.unit.service.UnitUsageProvider
import com.ampairs.unit.service.UnitUsageSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class UnitServiceTest {

    @Mock
    private lateinit var unitRepository: UnitRepository

    @Mock
    private lateinit var usageProvider: UnitUsageProvider

    private lateinit var unitService: UnitServiceImpl

    @BeforeEach
    fun setUp() {
        unitService = UnitServiceImpl(unitRepository, listOf(usageProvider))
    }

    @Test
    fun `create unit should persist entity and return response`() {
        val request = UnitRequest(name = "Kilogram", shortName = "kg", decimalPlaces = 3)
        val savedUnit = Unit().apply {
            uid = "UNIT-001"
            name = "Kilogram"
            shortName = "kg"
            decimalPlaces = 3
            active = true
        }

        whenever(unitRepository.save(any())).thenReturn(savedUnit)

        val response = unitService.create(request)

        assertEquals("UNIT-001", response.uid)
        assertEquals("Kilogram", response.name)
        verify(unitRepository).save(any())
    }

    @Test
    fun `update unit should throw when uid not found`() {
        whenever(unitRepository.findByUid("UNIT-404")).thenReturn(null)

        assertThrows<UnitNotFoundException> {
            unitService.update("UNIT-404", UnitRequest(name = "Gram", shortName = "g"))
        }
    }

    @Test
    fun `delete unit should mark inactive when not in use`() {
        val unit = Unit().apply {
            uid = "UNIT-002"
            name = "Piece"
        }

        whenever(unitRepository.findByUid("UNIT-002")).thenReturn(unit)
        whenever(usageProvider.findUsage("UNIT-002"))
            .thenReturn(UnitUsageSnapshot(unitUid = "UNIT-002"))

        unitService.delete("UNIT-002")

        assertFalse(unit.active)
        verify(unitRepository).save(unit)
    }

    @Test
    fun `delete unit should throw when usage detected`() {
        val unit = Unit().apply { uid = "UNIT-003" }

        whenever(unitRepository.findByUid("UNIT-003")).thenReturn(unit)
        whenever(usageProvider.findUsage("UNIT-003"))
            .thenReturn(UnitUsageSnapshot(unitUid = "UNIT-003", entityIds = listOf("PROD-1")))

        assertThrows<UnitInUseException> {
            unitService.delete("UNIT-003")
        }
    }
}
