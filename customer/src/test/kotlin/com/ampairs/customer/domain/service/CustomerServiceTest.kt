package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
import com.ampairs.event.domain.events.CustomerCreatedEvent
import com.ampairs.event.domain.events.CustomerUpdatedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.time.LocalDateTime
import java.util.Optional

class CustomerServiceTest {

    private val customerRepository: CustomerRepository = mock()
    private val customerPagingRepository: CustomerPagingRepository = mock()
    private val stateRepository: StateRepository = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val entityChangePublisher: EntityChangePublisher = mock()

    private lateinit var customerService: CustomerService

    @BeforeEach
    fun setUp() {
        reset(customerRepository, customerPagingRepository, stateRepository, eventPublisher, entityChangePublisher)
        TenantContextHolder.setCurrentTenant("tenant-123")
        DeviceContextHolder.setCurrentDevice("device-456")

        customerService = CustomerService(
            customerRepository = customerRepository,
            customerPagingRepository = customerPagingRepository,
            stateRepository = stateRepository,
            eventPublisher = eventPublisher,
            entityChangePublisher = entityChangePublisher
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
        DeviceContextHolder.clearDeviceContext()
    }

    @Test
    fun `createCustomer sets defaults and publishes created event`() {
        val customer = buildCustomer().apply { gstNumber = null }
        whenever(customerRepository.save(any<Customer>())).thenAnswer { invocation ->
            (invocation.arguments.first() as Customer).apply {
                uid = "CUS-001"
                createdAt = Instant.now()
                updatedAt = createdAt
            }
        }

        val savedCustomer = customerService.createCustomer(customer)

        assertEquals("ACTIVE", savedCustomer.status)

        val eventCaptor = argumentCaptor<CustomerCreatedEvent>()
        verify(eventPublisher).publishEvent(eventCaptor.capture())

        val publishedEvent = eventCaptor.firstValue
        assertEquals("CUS-001", publishedEvent.entityId)
        assertEquals("tenant-123", publishedEvent.workspaceId)
        assertEquals("device-456", publishedEvent.deviceId)
    }

    @Test
    fun `createCustomer throws for invalid gst number`() {
        val customer = buildCustomer().apply {
            gstNumber = "INVALIDGST"
        }

        assertThrows(IllegalArgumentException::class.java) {
            customerService.createCustomer(customer)
        }

        verify(customerRepository, never()).save(any())
        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `createCustomer allows duplicate gst number`() {
        val customer = buildCustomer().apply {
            gstNumber = "22AAAAA0000A1Z5"
        }
        whenever(customerRepository.save(any<Customer>())).thenAnswer { invocation ->
            (invocation.arguments.first() as Customer).apply {
                uid = "CUS-DUP"
                createdAt = Instant.now()
                updatedAt = createdAt
            }
        }

        val saved = customerService.createCustomer(customer)

        assertEquals("ACTIVE", saved.status)
        verify(customerRepository).save(any())
        verify(eventPublisher).publishEvent(any())
    }

    @Test
    fun `getCustomersAfterSync returns filtered results when timestamp valid`() {
        val pageable = PageRequest.of(0, 10)
        val sampleCustomer = buildCustomer().apply { uid = "CUS-100" }
        whenever(customerRepository.findCustomersUpdatedAfter(any(), any()))
            .thenReturn(PageImpl(listOf(sampleCustomer), pageable, 1))

        val page = customerService.getCustomersAfterSync("2024-05-01T10:15:30Z", pageable)

        assertEquals(1, page.totalElements)
        verify(customerRepository).findCustomersUpdatedAfter(any(), any())
    }

    @Test
    fun `getCustomersAfterSync falls back to all customers on parse error`() {
        val pageable = PageRequest.of(0, 10)
        val sampleCustomer = buildCustomer().apply { uid = "CUS-200" }
        whenever(customerRepository.findAll(pageable))
            .thenReturn(PageImpl(listOf(sampleCustomer), pageable, 1))

        val page = customerService.getCustomersAfterSync("invalid-date", pageable)

        assertEquals(1, page.totalElements)
        verify(customerRepository).findAll(pageable)
    }

    private fun buildCustomer(): Customer {
        return Customer().apply {
            countryCode = 91
            name = "Test Customer"
            customerType = "RETAIL"
            customerGroup = "DEFAULT"
            phone = "9999999999"
            landline = "0800000000"
            email = "customer@example.com"
            creditLimit = 0.0
            creditDays = 0
            outstandingAmount = 0.0
            address = "123 Test Street"
            street = "Test Street"
            street2 = "Test Street 2"
            city = "Bengaluru"
            pincode = "560001"
            state = "KA"
            country = "India"
        }
    }
}
