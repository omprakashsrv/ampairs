package com.ampairs.unit

import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.dto.UnitConversionResponse
import com.ampairs.unit.service.UnitConversionService
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class UnitConversionControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var unitConversionService: UnitConversionService

    @Test
    fun `list conversions returns payload`() {
        val conversion = UnitConversionResponse(
            uid = "CONV-1",
            baseUnitId = "UNIT-A",
            derivedUnitId = "UNIT-B",
            productId = null,
            multiplier = 10.0,
            baseUnit = null,
            derivedUnit = null,
            refId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(unitConversionService.findAll()).thenReturn(listOf(conversion))

        mockMvc.perform(
            get("/api/v1/unit/conversion")
                .header("X-Workspace-ID", "TEST_WS")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].uid", equalTo("CONV-1")))
    }

    @Test
    fun `create conversion delegates to service`() {
        val requestJson = """
            {
              "base_unit_id": "UNIT-A",
              "derived_unit_id": "UNIT-B",
              "multiplier": 10.0
            }
        """.trimIndent()

        val response = UnitConversionResponse(
            uid = "CONV-NEW",
            baseUnitId = "UNIT-A",
            derivedUnitId = "UNIT-B",
            productId = null,
            multiplier = 10.0,
            baseUnit = null,
            derivedUnit = null,
            refId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(unitConversionService.create(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/unit/conversion")
                .header("X-Workspace-ID", "TEST_WS")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.uid", equalTo("CONV-NEW")))

        verify(unitConversionService).create(any<UnitConversionRequest>())
    }
}
