package com.ampairs.unit

import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.service.UnitService
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class UnitControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var unitService: UnitService

    @Test
    fun `should list units`() {
        val unitResponse = UnitResponse(
            uid = "UNIT-100",
            name = "Kilogram",
            shortName = "kg",
            decimalPlaces = 3,
            refId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(unitService.findAll(true)).thenReturn(listOf(unitResponse))

        mockMvc.perform(
            get("/api/v1/unit")
                .header("X-Workspace-ID", "TEST_WS")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data[0].name", equalTo("Kilogram")))

        verify(unitService).findAll(true)
    }

    @Test
    fun `should create unit`() {
        val requestJson = """
            {
              "name": "Gram",
              "short_name": "g",
              "decimal_places": 2
            }
        """.trimIndent()

        val response = UnitResponse(
            uid = "UNIT-200",
            name = "Gram",
            shortName = "g",
            decimalPlaces = 2,
            refId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        whenever(unitService.create(any())).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/unit")
                .header("X-Workspace-ID", "TEST_WS")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.uid", equalTo("UNIT-200")))

        verify(unitService).create(any<UnitRequest>())
    }

    @Test
    fun `should delete unit`() {
        mockMvc.perform(
            delete("/api/v1/unit/UNIT-300")
                .header("X-Workspace-ID", "TEST_WS")
        )
            .andExpect(status().isOk)

        verify(unitService).delete("UNIT-300")
    }
}
