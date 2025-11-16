package com.ampairs.customer

import com.ampairs.AmpairsApplication
import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.dto.CustomerUpdateRequest
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.domain.service.CustomerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Suppress("DEPRECATION")
@SpringBootTest(classes = [AmpairsApplication::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @field:MockBean
    private lateinit var customerService: CustomerService

    @Test
    @DisplayName("POST /customer/v1 - Upsert customer")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should upsert customer`() {
        val request = buildUpdateRequest(uid = "cust-1", name = "Updated Name")
        val savedCustomer = buildCustomer(uid = "cust-1", name = "Updated Name")
        whenever(customerService.upsertCustomer(any())).thenReturn(savedCustomer)

        mockMvc.perform(
            post("/customer/v1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        val customerCaptor = argumentCaptor<Customer>()
        verify(customerService).upsertCustomer(customerCaptor.capture())
        assertEquals("Updated Name", customerCaptor.firstValue.name)
        assertEquals("cust-1", customerCaptor.firstValue.uid)
    }

    @Test
    @DisplayName("POST /customer/v1/customers - Bulk update customers")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should bulk update customers`() {
        val request = listOf(
            buildUpdateRequest(uid = "cust-1", name = "Customer One"),
            buildUpdateRequest(uid = "cust-2", name = "Customer Two")
        )
        val updatedEntities = listOf(
            buildCustomer(uid = "cust-1", name = "Customer One"),
            buildCustomer(uid = "cust-2", name = "Customer Two")
        )

        whenever(customerService.updateCustomers(any())).thenReturn(updatedEntities)

        mockMvc.perform(
            post("/customer/v1/customers")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).updateCustomers(any())
    }

    @Test
    @DisplayName("GET /customer/v1/states - Returns state list")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return states list`() {
        val state = State().apply {
            uid = "state-1"
            name = "Karnataka"
            shortName = "KA"
            country = "India"
            displayOrder = 1
        }
        whenever(customerService.getStates()).thenReturn(listOf(state))

        mockMvc.perform(
            get("/customer/v1/states")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .param("last_updated", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).getStates()
    }

    @Test
    @DisplayName("GET /customer/v1/{id} - Finds customer by id")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return customer by id`() {
        whenever(customerService.getCustomers()).thenReturn(
            listOf(buildCustomer(uid = "cust-1", name = "Lookup Customer"))
        )

        mockMvc.perform(
            get("/customer/v1/cust-1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).getCustomers()
    }

    @Test
    @DisplayName("GET /customer/v1/{id} - Returns error when customer missing")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return error when customer not found`() {
        whenever(customerService.getCustomers()).thenReturn(emptyList())

        mockMvc.perform(
            get("/customer/v1/missing-id")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))

        verify(customerService).getCustomers()
    }

    @Test
    @DisplayName("PUT /customer/v1/{id} - Updates customer")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should update customer`() {
        val request = buildUpdateRequest(uid = "cust-1", name = "Updated Name")
        val updatedCustomer = buildCustomer(uid = "cust-1", name = "Updated Name")
        whenever(customerService.updateCustomer(eq("cust-1"), any())).thenReturn(updatedCustomer)

        mockMvc.perform(
            put("/customer/v1/cust-1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).updateCustomer(eq("cust-1"), any())
    }

    @Test
    @DisplayName("PUT /customer/v1/{id} - Returns error when missing")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return error when updating missing customer`() {
        val request = buildUpdateRequest(uid = "cust-404", name = "Missing")
        whenever(customerService.updateCustomer(eq("cust-404"), any())).thenReturn(null)

        mockMvc.perform(
            put("/customer/v1/cust-404")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))

        verify(customerService).updateCustomer(eq("cust-404"), any())
    }

    @Test
    @DisplayName("GET /customer/v1/gst/{gst} - Finds customer by GST number")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return customer by gst number`() {
        val customer = buildCustomer(uid = "cust-1", name = "GST Customer").apply {
            gstNumber = "29ABCDE1234F1Z5"
        }
        whenever(customerService.getCustomerByGstNumber("29ABCDE1234F1Z5")).thenReturn(customer)

        mockMvc.perform(
            get("/customer/v1/gst/29ABCDE1234F1Z5")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.gst_number").value("29ABCDE1234F1Z5"))

        verify(customerService).getCustomerByGstNumber("29ABCDE1234F1Z5")
    }

    @Test
    @DisplayName("GET /customer/v1/gst/{gst} - Returns error when not found")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return error when gst not found`() {
        whenever(customerService.getCustomerByGstNumber("29ABCDE1234F1Z5")).thenReturn(null)

        mockMvc.perform(
            get("/customer/v1/gst/29ABCDE1234F1Z5")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))

        verify(customerService).getCustomerByGstNumber("29ABCDE1234F1Z5")
    }

    @Test
    @DisplayName("POST /customer/v1/validate-gst - Validates GST number")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should validate gst number`() {
        whenever(customerService.validateGstNumber("29ABCDE1234F1Z5")).thenReturn(true)

        mockMvc.perform(
            post("/customer/v1/validate-gst")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"gst_number":"29ABCDE1234F1Z5"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).validateGstNumber("29ABCDE1234F1Z5")
    }

    @Test
    @DisplayName("PUT /customer/v1/{id}/outstanding - Updates outstanding balance")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should update outstanding amount`() {
        val updatedCustomer = buildCustomer(uid = "cust-1", name = "Outstanding Customer").apply {
            outstandingAmount = 150.0
        }
        whenever(customerService.updateOutstanding("cust-1", 50.0, false)).thenReturn(updatedCustomer)

        mockMvc.perform(
            put("/customer/v1/cust-1/outstanding")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":50.0,"is_payment":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).updateOutstanding("cust-1", 50.0, false)
    }

    @Test
    @DisplayName("PUT /customer/v1/{id}/outstanding - Returns error when customer missing")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return error when updating outstanding for missing customer`() {
        whenever(customerService.updateOutstanding("cust-404", 25.0, true)).thenReturn(null)

        mockMvc.perform(
            put("/customer/v1/cust-404/outstanding")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":25.0,"is_payment":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))

        verify(customerService).updateOutstanding("cust-404", 25.0, true)
    }

    @Test
    @DisplayName("POST /customer/v1/validate-gst - Requires gst number")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return validation error when gst missing`() {
        mockMvc.perform(
            post("/customer/v1/validate-gst")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.message").value("Validation failed"))
            .andExpect(jsonPath("$.error.validation_errors.gst_number").value("GST number is required"))

        verify(customerService, never()).validateGstNumber(any())
    }

    private fun buildCustomer(uid: String, name: String): Customer {
        return Customer().apply {
            this.uid = uid
            this.countryCode = 91
            this.name = name
            this.customerType = "RETAIL"
            this.customerGroup = "DEFAULT"
            this.phone = "9876543210"
            this.landline = "0800000000"
            this.email = "${name.lowercase().replace(" ", ".")}@example.com"
            this.creditLimit = 1000.0
            this.creditDays = 15
            this.outstandingAmount = 100.0
            this.address = "123 Test Street"
            this.street = "Test Street"
            this.street2 = "Test Street 2"
            this.city = "Bengaluru"
            this.pincode = "560001"
            this.state = "Karnataka"
            this.country = "India"
            this.status = "ACTIVE"
            this.billingAddress = Address(
                street = "123 Test Street",
                city = "Bengaluru",
                state = "Karnataka",
                country = "India",
                pincode = "560001"
            )
            this.shippingAddress = this.billingAddress
            this.createdAt = Instant.now()
            this.updatedAt = createdAt
        }
    }

    private fun buildUpdateRequest(uid: String, name: String): CustomerUpdateRequest {
        return CustomerUpdateRequest(
            uid = uid,
            refId = null,
            name = name,
            gstin = null,
            countryCode = 91,
            phone = "9876543210",
            landline = null,
            email = "${name.lowercase().replace(" ", ".")}@example.com",
            pincode = "560001",
            customerType = "RETAIL",
            customerGroup = "DEFAULT",
            businessName = null,
            companyId = null,
            gstNumber = null,
            panNumber = null,
            creditLimit = 1000.0,
            creditDays = 15,
            customerNumber = null,
            status = "ACTIVE",
            attributes = mapOf("tier" to "gold"),
            address = "123 Test Street",
            state = "Karnataka",
            street = "Test Street",
            street2 = "Test Street 2",
            city = "Bengaluru",
            country = "India",
            billingAddress = Address(
                street = "123 Test Street",
                city = "Bengaluru",
                state = "Karnataka",
                country = "India",
                pincode = "560001"
            ),
            shippingAddress = Address(
                street = "123 Test Street",
                city = "Bengaluru",
                state = "Karnataka",
                country = "India",
                pincode = "560001"
            ),
            latitude = null,
            longitude = null,
            active = true,
            softDeleted = false
        )
    }

}
