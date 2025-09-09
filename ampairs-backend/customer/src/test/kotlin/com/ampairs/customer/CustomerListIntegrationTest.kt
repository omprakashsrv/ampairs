package com.ampairs.customer

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.controller.CustomerController
import com.ampairs.customer.domain.dto.CustomerResponse
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.domain.service.CustomerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * Integration tests for Customer Management API - List Customer endpoint.
 * 
 * Tests verify the GET /customer/v1/list endpoint using MockMvc with mocked services.
 * Covers customer listing with search, filtering, and pagination.
 */
@WebMvcTest(controllers = [CustomerController::class])
@ActiveProfiles("test")
class CustomerListIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var customerService: CustomerService

    @Test
    @DisplayName("GET /customer/v1/list - Get paginated customer list")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return paginated list of active customers`() {
        val customers = listOf(
            createMockCustomerResponse("cust-1", "Rajesh Kumar", "+919876543210", CustomerType.RETAIL),
            createMockCustomerResponse("cust-2", "ABC Hardware Store", "+919123456789", CustomerType.WHOLESALE)
        )

        val customerPage = PageImpl(customers.map { mockCustomer(it) }, PageRequest.of(0, 20), 2)
        
        whenever(customerService.searchCustomers(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1/list")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customers").isArray)
            .andExpect(jsonPath("$.data.customers").isNotEmpty)
            .andExpect(jsonPath("$.data.pagination.page").value(0))
            .andExpect(jsonPath("$.data.pagination.size").value(20))
            .andExpect(jsonPath("$.data.pagination.total_elements").value(2))

        verify(customerService).searchCustomers(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )
    }

    @Test
    @DisplayName("GET /customer/v1/list - Search customers by name")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should search customers by name with partial matching`() {
        val customers = listOf(
            createMockCustomerResponse("cust-1", "Kumar Hardware", "+919876543210", CustomerType.WHOLESALE),
            createMockCustomerResponse("cust-2", "Kumar Traders", "+919123456789", CustomerType.WHOLESALE)
        )

        val customerPage = PageImpl(customers.map { mockCustomer(it) }, PageRequest.of(0, 20), 2)
        
        whenever(customerService.searchCustomers(
            eq("Kumar"), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1/list")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .param("search", "Kumar")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customers").isArray)
            .andExpect(jsonPath("$.data.customers[0].name").value("Kumar Hardware"))
            .andExpect(jsonPath("$.data.customers[1].name").value("Kumar Traders"))

        verify(customerService).searchCustomers(
            eq("Kumar"), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter customers by type")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should filter customers by customer type`() {
        val customers = listOf(
            createMockCustomerResponse("cust-1", "ABC Wholesale", "+919876543210", CustomerType.WHOLESALE),
            createMockCustomerResponse("cust-2", "XYZ Distributors", "+919123456789", CustomerType.WHOLESALE)
        )

        val customerPage = PageImpl(customers.map { mockCustomer(it) }, PageRequest.of(0, 20), 2)
        
        whenever(customerService.searchCustomers(
            isNull(), eq(CustomerType.WHOLESALE), isNull(), isNull(), isNull(), isNull(), any()
        )).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1/list")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .param("customer_type", "WHOLESALE")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customers").isArray)
            .andExpect(jsonPath("$.data.customers[0].customerType").value("WHOLESALE"))
            .andExpect(jsonPath("$.data.customers[1].customerType").value("WHOLESALE"))

        verify(customerService).searchCustomers(
            isNull(), eq(CustomerType.WHOLESALE), isNull(), isNull(), isNull(), isNull(), any()
        )
    }

    @Test
    @DisplayName("GET /customer/v1/list - Filter customers with credit")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should filter customers who have credit limits`() {
        val customers = listOf(
            createMockCustomerResponse("cust-1", "Credit Customer 1", "+919876543210", CustomerType.WHOLESALE, creditLimit = 50000.0),
            createMockCustomerResponse("cust-2", "Credit Customer 2", "+919123456789", CustomerType.WHOLESALE, creditLimit = 25000.0)
        )

        val customerPage = PageImpl(customers.map { mockCustomer(it) }, PageRequest.of(0, 20), 2)
        
        whenever(customerService.searchCustomers(
            isNull(), isNull(), isNull(), isNull(), eq(true), isNull(), any()
        )).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1/list")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .param("has_credit", "true")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customers").isArray)
            .andExpect(jsonPath("$.data.customers[0].creditLimit").value(50000.0))
            .andExpect(jsonPath("$.data.customers[1].creditLimit").value(25000.0))

        verify(customerService).searchCustomers(
            isNull(), isNull(), isNull(), isNull(), eq(true), isNull(), any()
        )
    }

    @Test
    @DisplayName("GET /customer/v1/list - Empty search results")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return empty results for non-matching search`() {
        val customerPage = PageImpl<Customer>(emptyList(), PageRequest.of(0, 20), 0)
        
        whenever(customerService.searchCustomers(
            eq("NonExistentCustomer"), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1/list")
                .header("X-Workspace-ID", "TEST_RETAIL_WS_001")
                .param("search", "NonExistentCustomer")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customers").isArray)
            .andExpect(jsonPath("$.data.customers").isEmpty)
            .andExpect(jsonPath("$.data.pagination.total_elements").value(0))

        verify(customerService).searchCustomers(
            eq("NonExistentCustomer"), isNull(), isNull(), isNull(), isNull(), isNull(), any()
        )
    }

    private fun createMockCustomerResponse(
        id: String, 
        name: String, 
        phone: String, 
        type: CustomerType,
        creditLimit: Double = 0.0
    ): CustomerResponse {
        return CustomerResponse(
            id = id,
            name = name,
            companyId = "COMP_001",
            countryCode = 91,
            phone = phone,
            landline = "",
            email = "${name.lowercase().replace(" ", ".")}@example.com",
            gstin = "",
            address = "Test Address",
            pincode = "560001",
            state = "Karnataka",
            latitude = 12.9716,
            longitude = 77.5946,
            active = true,
            softDeleted = false,
            billingSameAsRegistered = true,
            shippingSameAsBilling = true,
            lastUpdated = System.currentTimeMillis(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun mockCustomer(response: CustomerResponse): Customer {
        return Customer().apply {
            uid = response.id
            name = response.name
            companyId = response.companyId
            countryCode = response.countryCode
            phone = response.phone
            email = response.email ?: ""
            address = response.address ?: ""
            state = response.state ?: ""
            active = response.active
            createdAt = response.createdAt
            updatedAt = response.updatedAt
        }
    }
}