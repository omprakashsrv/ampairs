package com.ampairs.customer

import com.ampairs.AmpairsApplication

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.controller.CustomerAddressRequest
import com.ampairs.customer.controller.CustomerCreateRequest
import com.ampairs.customer.domain.dto.CustomerResponse
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.domain.service.CustomerService
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
 * Integration tests for Customer Management API - Create Customer endpoint.
 * 
 * Tests verify the POST /customer/v1/create endpoint using MockMvc with mocked services.
 * Covers customer creation with retail business-specific fields and attributes.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerCreateIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var customerService: CustomerService

    @Test
    @DisplayName("POST /customer/v1/create - Create basic retail customer")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create basic customer with required fields`() {
        val address = CustomerAddressRequest(
            street = "123 MG Road",
            city = "Bangalore",
            state = "Karnataka",
            postalCode = "560001",
            country = "India"
        )
        
        val customerRequest = CustomerCreateRequest(
            name = "Rajesh Kumar",
            phone = "+919876543210",
            email = "rajesh.kumar@example.com",
            customerType = CustomerType.RETAIL,
            address = address
        )

        val mockCustomer = Customer().apply {
            uid = "cust-123"
            customerNumber = "CUST-20250909-001"
            name = "Rajesh Kumar"
            phone = "+919876543210"
            email = "rajesh.kumar@example.com"
            customerType = CustomerType.RETAIL
            status = "ACTIVE"
            city = "Bangalore"
            state = "Karnataka"
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        whenever(customerService.createCustomer(any<Customer>()))
            .thenReturn(mockCustomer)

        mockMvc.perform(
            post("/customer/v1/create")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("cust-123"))
            .andExpect(jsonPath("$.data.name").value("Rajesh Kumar"))
            .andExpect(jsonPath("$.data.phone").value("+919876543210"))

        verify(customerService).createCustomer(any<Customer>())
    }

    @Test
    @DisplayName("POST /customer/v1/create - Create wholesale customer with credit")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should create wholesale customer with credit limit`() {
        val address = CustomerAddressRequest(
            street = "456 Commercial Street",
            city = "Mysore",
            state = "Karnataka",
            postalCode = "570001",
            country = "India"
        )
        
        val customerRequest = CustomerCreateRequest(
            name = "Sree Balaji General Stores",
            phone = "+919123456789",
            customerType = CustomerType.WHOLESALE,
            businessName = "Sree Balaji General Stores",
            gstNumber = "29ABCDE1234F1Z5",
            creditLimit = 50000.00,
            creditDays = 30,
            address = address,
            attributes = mapOf(
                "preferredDeliveryTime" to "MORNING",
                "bulkOrderDiscount" to 5.0,
                "paymentTerms" to "NET_30"
            )
        )

        val mockCustomer = Customer().apply {
            uid = "cust-456"
            customerNumber = "CUST-20250909-002"
            name = "Sree Balaji General Stores"
            phone = "+919123456789"
            customerType = CustomerType.WHOLESALE
            businessName = "Sree Balaji General Stores"
            gstNumber = "29ABCDE1234F1Z5"
            creditLimit = 50000.00
            creditDays = 30
            status = "ACTIVE"
            city = "Mysore"
            state = "Karnataka"
            attributes = mapOf(
                "preferredDeliveryTime" to "MORNING",
                "bulkOrderDiscount" to 5.0,
                "paymentTerms" to "NET_30"
            )
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        whenever(customerService.createCustomer(any<Customer>()))
            .thenReturn(mockCustomer)

        mockMvc.perform(
            post("/customer/v1/create")
                .header("X-Workspace-ID", "TEST_KIRANA_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Sree Balaji General Stores"))
            .andExpect(jsonPath("$.data.companyId").exists())

        verify(customerService).createCustomer(any<Customer>())
    }

    @Test
    @DisplayName("POST /customer/v1/create - Validation error for missing required fields")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return validation error when required fields are missing`() {
        val invalidCustomerRequest = """
            {
                "email": "incomplete@example.com",
                "customerType": "RETAIL"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/customer/v1/create")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCustomerRequest)
        )
            .andExpect(status().isBadRequest)

        verify(customerService, never()).createCustomer(any<Customer>())
    }

    @Test
    @DisplayName("POST /customer/v1/create - Service exception handling")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should handle service exceptions gracefully`() {
        val address = CustomerAddressRequest(
            street = "123 Test Street",
            city = "Test City",
            state = "Test State",
            postalCode = "123456",
            country = "India"
        )
        
        val customerRequest = CustomerCreateRequest(
            name = "Test Customer",
            phone = "+919999999999",
            customerType = CustomerType.RETAIL,
            address = address
        )

        whenever(customerService.createCustomer(any<Customer>()))
            .thenThrow(IllegalArgumentException("Customer number already exists"))

        mockMvc.perform(
            post("/customer/v1/create")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest))
        )
            .andExpect(status().is4xxClientError)

        verify(customerService).createCustomer(any<Customer>())
    }
}