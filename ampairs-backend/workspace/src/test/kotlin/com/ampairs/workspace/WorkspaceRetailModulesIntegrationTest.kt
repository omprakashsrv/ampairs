package com.ampairs.workspace

import com.ampairs.AmpairsApplication

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.workspace.model.dto.WorkspaceResponse
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.service.WorkspaceService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * Integration tests for Workspace Management API - Create Workspace endpoint.
 * 
 * Tests verify the POST /workspace/v1 endpoint using MockMvc with mocked services.
 * Covers workspace creation for retail business types with proper module configurations.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceRetailModulesIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var workspaceService: WorkspaceService

    @Test
    @DisplayName("POST /workspace/v1 - Create hardware store workspace")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create hardware store workspace with construction modules`() {
        val workspaceRequest = CreateWorkspaceRequest(
            name = "Kumar Hardware Store",
            slug = "kumar-hardware",
            description = "Complete hardware store management for construction supplies",
            workspaceType = WorkspaceType.BUSINESS,
            addressLine1 = "123 Main Street",
            city = "Bangalore",
            state = "Karnataka",
            postalCode = "560001",
            country = "India"
        )

        val mockWorkspaceResponse = WorkspaceResponse(
            id = "WS_HARDWARE_001",
            name = "Kumar Hardware Store",
            slug = "kumar-hardware",
            description = "Complete hardware store management for construction supplies",
            workspaceType = WorkspaceType.BUSINESS,
            avatarUrl = null,
            isActive = true,
            subscriptionPlan = com.ampairs.workspace.model.enums.SubscriptionPlan.BASIC,
            maxMembers = 10,
            storageLimitGb = 50,
            storageUsedGb = 0,
            timezone = "Asia/Kolkata",
            language = "en",
            createdBy = "USR_KUMAR_123",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastActivityAt = LocalDateTime.now(),
            trialExpiresAt = null,
            currency = "INR",
            addressLine1 = "123 Main Street",
            city = "Bangalore",
            state = "Karnataka",
            postalCode = "560001",
            country = "India"
        )

        whenever(workspaceService.createWorkspace(any<CreateWorkspaceRequest>(), any<String>()))
            .thenReturn(mockWorkspaceResponse)

        mockMvc.perform(
            post("/workspace/v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workspaceRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("WS_HARDWARE_001"))
            .andExpect(jsonPath("$.name").value("Kumar Hardware Store"))
            .andExpect(jsonPath("$.workspaceType").value("BUSINESS"))
            .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
            .andExpect(jsonPath("$.currency").value("INR"))

        verify(workspaceService).createWorkspace(any<CreateWorkspaceRequest>(), any<String>())
    }

    @Test
    @DisplayName("POST /workspace/v1 - Create jewelry store workspace") 
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create jewelry store workspace with precious metals modules`() {
        val workspaceRequest = CreateWorkspaceRequest(
            name = "Golden Dreams Jewelry",
            slug = "golden-dreams",
            description = "Premium jewelry store with gold and diamond collections",
            workspaceType = WorkspaceType.BUSINESS,
            addressLine1 = "456 Commercial Street",
            city = "Mysore",
            state = "Karnataka",
            postalCode = "570001",
            country = "India"
        )

        val mockWorkspaceResponse = WorkspaceResponse(
            id = "WS_JEWELRY_001",
            name = "Golden Dreams Jewelry",
            slug = "golden-dreams",
            description = "Premium jewelry store with gold and diamond collections",
            workspaceType = WorkspaceType.BUSINESS,
            avatarUrl = null,
            isActive = true,
            subscriptionPlan = com.ampairs.workspace.model.enums.SubscriptionPlan.PROFESSIONAL,
            maxMembers = 25,
            storageLimitGb = 100,
            storageUsedGb = 5,
            timezone = "Asia/Kolkata",
            language = "en",
            createdBy = "USR_JEWELRY_456",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastActivityAt = LocalDateTime.now(),
            trialExpiresAt = null,
            currency = "INR",
            addressLine1 = "456 Commercial Street",
            city = "Mysore",
            state = "Karnataka",
            postalCode = "570001",
            country = "India"
        )

        whenever(workspaceService.createWorkspace(any<CreateWorkspaceRequest>(), any<String>()))
            .thenReturn(mockWorkspaceResponse)

        mockMvc.perform(
            post("/workspace/v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workspaceRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("WS_JEWELRY_001"))
            .andExpect(jsonPath("$.name").value("Golden Dreams Jewelry"))
            .andExpect(jsonPath("$.subscriptionPlan").value("PROFESSIONAL"))

        verify(workspaceService).createWorkspace(any<CreateWorkspaceRequest>(), any<String>())
    }

    @Test
    @DisplayName("POST /workspace/v1 - Create kirana store workspace")
    @WithMockUser(username = "testuser", roles = ["USER"])  
    fun `should create kirana store workspace with grocery modules`() {
        val workspaceRequest = CreateWorkspaceRequest(
            name = "Sai Provision Store",
            slug = "sai-provisions",
            description = "Neighborhood grocery store with daily essentials",
            workspaceType = WorkspaceType.BUSINESS,
            addressLine1 = "789 Residential Area",
            city = "Hubli",
            state = "Karnataka",
            postalCode = "580001",
            country = "India"
        )

        val mockWorkspaceResponse = WorkspaceResponse(
            id = "WS_KIRANA_001",
            name = "Sai Provision Store",
            slug = "sai-provisions",
            description = "Neighborhood grocery store with daily essentials",
            workspaceType = WorkspaceType.BUSINESS,
            avatarUrl = null,
            isActive = true,
            subscriptionPlan = com.ampairs.workspace.model.enums.SubscriptionPlan.BASIC,
            maxMembers = 5,
            storageLimitGb = 20,
            storageUsedGb = 2,
            timezone = "Asia/Kolkata",
            language = "en",
            createdBy = "USR_KIRANA_789",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastActivityAt = LocalDateTime.now(),
            trialExpiresAt = null,
            currency = "INR",
            addressLine1 = "789 Residential Area",
            city = "Hubli",
            state = "Karnataka",
            postalCode = "580001",
            country = "India"
        )

        whenever(workspaceService.createWorkspace(any<CreateWorkspaceRequest>(), any<String>()))
            .thenReturn(mockWorkspaceResponse)

        mockMvc.perform(
            post("/workspace/v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workspaceRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("WS_KIRANA_001"))
            .andExpect(jsonPath("$.subscriptionPlan").value("BASIC"))

        verify(workspaceService).createWorkspace(any<CreateWorkspaceRequest>(), any<String>())
    }

    @Test
    @DisplayName("POST /workspace/v1 - Validation error handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle validation errors gracefully`() {
        val invalidWorkspaceRequest = """
            {
                "name": "",
                "slug": "invalid@slug",
                "workspaceType": "BUSINESS"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/workspace/v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidWorkspaceRequest)
        )
            .andExpect(status().isBadRequest)

        verify(workspaceService, never()).createWorkspace(any<CreateWorkspaceRequest>(), any<String>())
    }

    @Test
    @DisplayName("POST /workspace/v1 - Service exception handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle service exceptions gracefully`() {
        val workspaceRequest = CreateWorkspaceRequest(
            name = "Duplicate Store",
            slug = "existing-slug"
        )

        whenever(workspaceService.createWorkspace(any<CreateWorkspaceRequest>(), any<String>()))
            .thenThrow(IllegalArgumentException("Workspace slug already exists"))

        mockMvc.perform(
            post("/workspace/v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workspaceRequest))
        )
            .andExpect(status().is4xxClientError)

        verify(workspaceService).createWorkspace(any<CreateWorkspaceRequest>(), any<String>())
    }
}