package com.ampairs.unit

import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.domain.model.UnitConversion
import com.ampairs.unit.exception.CircularConversionException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.repository.UnitConversionRepository
import com.ampairs.unit.repository.UnitRepository
import com.ampairs.unit.service.UnitConversionServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UnitConversionServiceTest {

    @Mock private lateinit var unitConversionRepository: UnitConversionRepository
    @Mock private lateinit var unitRepository: UnitRepository

    private fun service(): UnitConversionServiceImpl = UnitConversionServiceImpl(
        unitConversionRepository = unitConversionRepository,
        unitRepository = unitRepository
    )

    @Test
    fun `convert should use direct multiplier`() {
        val conversion = UnitConversion().apply {
            uid = "CONV-1"
            baseUnitId = "UNIT-A"
            derivedUnitId = "UNIT-B"
            multiplier = 10.0
            active = true
        }

        whenever(unitConversionRepository.findExactConversion("UNIT-A", "UNIT-B", null)).thenReturn(conversion)

        val result = service().convert(2.0, "UNIT-A", "UNIT-B", null)

        assertEquals(20.0, result)
    }

    @Test
    fun `convert should use inverse multiplier when reverse conversion exists`() {
        val conversion = UnitConversion().apply {
            uid = "CONV-2"
            baseUnitId = "UNIT-B"
            derivedUnitId = "UNIT-A"
            multiplier = 4.0
            active = true
        }

        whenever(unitConversionRepository.findExactConversion("UNIT-B", "UNIT-A", null)).thenReturn(conversion)

        val result = service().convert(8.0, "UNIT-A", "UNIT-B", null)

        assertEquals(2.0, result)
    }

    @Test
    fun `create should validate units exist`() {
        val request = UnitConversionRequest(
            baseUnitId = "UNIT-X",
            derivedUnitId = "UNIT-Y",
            multiplier = 2.0
        )

        whenever(unitRepository.findByUid("UNIT-X")).thenReturn(Unit())
        whenever(unitRepository.findByUid("UNIT-Y")).thenReturn(null)

        assertThrows<UnitNotFoundException> {
            service().create(request)
        }
    }

    @Test
    fun `validate should detect circular conversions`() {
        val existing = UnitConversion().apply {
            uid = "CONV-3"
            baseUnitId = "UNIT-A"
            derivedUnitId = "UNIT-B"
            multiplier = 10.0
            active = true
        }

        whenever(unitRepository.findByUid(any())).thenReturn(Unit())
        whenever(unitConversionRepository.findExactConversion("UNIT-A", "UNIT-B", null)).thenReturn(null)
        whenever(unitConversionRepository.findAll()).thenReturn(listOf(existing))

        val service = service()

        assertThrows<CircularConversionException> {
            service.create(
                UnitConversionRequest(
                    baseUnitId = "UNIT-B",
                    derivedUnitId = "UNIT-A",
                    multiplier = 0.1
                )
            )
        }
    }
}
