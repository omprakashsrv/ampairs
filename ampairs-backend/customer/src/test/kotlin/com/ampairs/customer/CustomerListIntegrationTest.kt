package com.ampairs.customer

import com.ampairs.AmpairsApplication
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.service.CustomerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Suppress("DEPRECATION")
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CustomerListIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @field:MockBean
    private lateinit var customerService: CustomerService

    @Test
    @DisplayName("GET /customer/v1 - Returns paginated customers")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return paginated customers`() {
        val customers = listOf(
            buildCustomer(uid = "cust-1", name = "Rajesh Kumar", type = "RETAIL"),
            buildCustomer(uid = "cust-2", name = "ABC Hardware", type = "WHOLESALE")
        )
        val pageable = PageRequest.of(0, 20, Sort.by("updatedAt").ascending())
        val customerPage = PageImpl(customers, pageable, customers.size.toLong())

        whenever(customerService.getCustomersAfterSync(anyOrNull(), any())).thenReturn(customerPage)

        mockMvc.perform(
            get("/customer/v1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))

        val pageableCaptor = argumentCaptor<PageRequest>()
        verify(customerService).getCustomersAfterSync(isNull(), pageableCaptor.capture())
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(20, pageableCaptor.firstValue.pageSize)
    }

    @Test
    @DisplayName("GET /customer/v1 - Passes last_sync to service")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should forward last sync parameter`() {
        val pageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending())
        val emptyPage = PageImpl<Customer>(emptyList(), pageable, 0)
        val lastSync = "2024-06-01T10:00:00"

        whenever(customerService.getCustomersAfterSync(eq(lastSync), any())).thenReturn(emptyPage)

        mockMvc.perform(
            get("/customer/v1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .param("last_sync", lastSync)
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).getCustomersAfterSync(eq(lastSync), any())
    }

    private fun buildCustomer(uid: String, name: String, type: String): Customer {
        return Customer().apply {
            this.uid = uid
            this.name = name
            this.customerType = type
            this.customerGroup = "DEFAULT"
            this.phone = "+919876543210"
            this.landline = "0800000000"
            this.email = "${name.lowercase().replace(" ", ".")}@example.com"
            this.creditLimit = 1000.0
            this.creditDays = 30
            this.outstandingAmount = 0.0
            this.address = "123 Test Street"
            this.street = "Test Street"
            this.street2 = "Test Street 2"
            this.city = "Bengaluru"
            this.pincode = "560001"
            this.state = "Karnataka"
            this.country = "India"
            this.status = "ACTIVE"
            this.lastUpdated = System.currentTimeMillis()
            this.createdAt = LocalDateTime.now()
            this.updatedAt = createdAt
        }
    }

}
