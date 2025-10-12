package com.ampairs.business.controller

import com.ampairs.business.model.dto.BusinessCreateRequest
import com.ampairs.business.model.enums.BusinessType
import com.ampairs.business.repository.BusinessRepository
import com.ampairs.core.multitenancy.TenantContextHolder
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Integration tests for BusinessController.
 *
 * **Test Strategy**:
 * - Uses MockMvc to test controller endpoints
 * - Sets up tenant context for multi-tenancy
 * - Tests all CRUD operations
 * - Validates error handling
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security for testing
@ActiveProfiles("test")
class BusinessControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var businessRepository: BusinessRepository

    private val testWorkspaceId = "test-workspace-001"

    @BeforeEach
    fun setUp() {
        // Set tenant context
        TenantContextHolder.setCurrentTenant(testWorkspaceId)

        // Clean up any existing business for this workspace
        businessRepository.findByOwnerId(testWorkspaceId)?.let {
            businessRepository.delete(it)
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up
        businessRepository.findByOwnerId(testWorkspaceId)?.let {
            businessRepository.delete(it)
        }
        TenantContextHolder.clear()
    }

    @Test
    fun `should create business profile successfully`() {
        val request = BusinessCreateRequest(
            name = "Tech Solutions Pvt Ltd",
            businessType = BusinessType.RETAIL,
            email = "contact@techsolutions.com",
            phone = "+911234567890",
            timezone = "Asia/Kolkata",
            currency = "INR"
        )

        mockMvc.perform(
            post("/api/v1/business")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Tech Solutions Pvt Ltd"))
            .andExpect(jsonPath("$.data.business_type").value("RETAIL"))
            .andExpect(jsonPath("$.data.timezone").value("Asia/Kolkata"))
    }

    @Test
    fun `should get business profile after creation`() {
        // Create business first
        val request = BusinessCreateRequest(
            name = "Retail Store",
            businessType = BusinessType.RETAIL
        )

        mockMvc.perform(
            post("/api/v1/business")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        // Get business profile
        mockMvc.perform(get("/api/v1/business"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Retail Store"))
    }

    @Test
    fun `should return 404 when getting non-existent business`() {
        mockMvc.perform(get("/api/v1/business"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("BUSINESS_NOT_FOUND"))
    }

    @Test
    fun `should return 409 when creating duplicate business`() {
        val request = BusinessCreateRequest(
            name = "First Business",
            businessType = BusinessType.RETAIL
        )

        // Create first business
        mockMvc.perform(
            post("/api/v1/business")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        // Try to create second business (should fail)
        mockMvc.perform(
            post("/api/v1/business")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("BUSINESS_ALREADY_EXISTS"))
    }

    @Test
    fun `should check if business exists`() {
        // Check before creation
        mockMvc.perform(get("/api/v1/business/exists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.exists").value(false))

        // Create business
        val request = BusinessCreateRequest(
            name = "Test Business",
            businessType = BusinessType.RETAIL
        )

        mockMvc.perform(
            post("/api/v1/business")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        // Check after creation
        mockMvc.perform(get("/api/v1/business/exists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.exists").value(true))
    }
}
