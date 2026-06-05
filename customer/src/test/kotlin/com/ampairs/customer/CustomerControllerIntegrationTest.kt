package com.ampairs.customer

import com.ampairs.AmpairsApplication
import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.dto.CustomerUpdateRequest
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.workspace.service.WorkspaceMemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @field:MockitoBean
    private lateinit var customerService: CustomerService

    @field:MockitoBean
    private lateinit var workspaceMemberService: WorkspaceMemberService

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

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
        val customer = buildCustomer(uid = "cust-1", name = "Lookup Customer")
        whenever(customerService.getCustomerByUid("cust-1")).thenReturn(customer)

        mockMvc.perform(
            get("/customer/v1/cust-1")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(customerService).getCustomerByUid("cust-1")
    }

    @Test
    @DisplayName("GET /customer/v1/{id} - Returns error when customer missing")
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `should return error when customer not found`() {
        whenever(customerService.getCustomerByUid("missing-id")).thenReturn(null)

        mockMvc.perform(
            get("/customer/v1/missing-id")
                .header("X-Workspace-ID", "TEST_WORKSPACE")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))

        verify(customerService).getCustomerByUid("missing-id")
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
