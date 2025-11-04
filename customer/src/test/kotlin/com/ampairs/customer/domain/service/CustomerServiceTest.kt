package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
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

    private lateinit var customerService: CustomerService

    @BeforeEach
    fun setUp() {
        reset(customerRepository, customerPagingRepository, stateRepository, eventPublisher)
        TenantContextHolder.setCurrentTenant("tenant-123")
        DeviceContextHolder.setCurrentDevice("device-456")

        customerService = CustomerService(
            customerRepository = customerRepository,
            customerPagingRepository = customerPagingRepository,
            stateRepository = stateRepository,
            eventPublisher = eventPublisher
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
        whenever(customerRepository.findByGstNumber(any())).thenReturn(Optional.empty())
        whenever(customerRepository.save(any())).thenAnswer { invocation ->
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
    fun `createCustomer rejects duplicate gst number`() {
        val customer = buildCustomer().apply {
            gstNumber = "22AAAAA0000A1Z5"
        }
        whenever(customerRepository.findByGstNumber("22AAAAA0000A1Z5"))
            .thenReturn(Optional.of(buildCustomer()))

        assertThrows(IllegalArgumentException::class.java) {
            customerService.createCustomer(customer)
        }

        verify(customerRepository, never()).save(any())
        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `updateCustomer applies changes and publishes updated event`() {
        val existing = buildCustomer().apply {
            uid = "CUS-001"
            name = "Old Name"
            phone = "1111111111"
            email = "old@example.com"
            customerType = "RETAIL"
            creditLimit = 1000.0
            creditDays = 10
        }
        whenever(customerRepository.findByUid("CUS-001")).thenReturn(existing)
        whenever(customerRepository.save(existing)).thenReturn(existing)

        val updates = buildCustomer().apply {
            name = "New Name"
            phone = "2222222222"
            email = "new@example.com"
            customerType = "WHOLESALE"
            creditLimit = 1500.0
            creditDays = 20
            status = "INACTIVE"
            attributes = mapOf("tier" to "gold")
        }

        val result = customerService.updateCustomer("CUS-001", updates)

        assertNotNull(result)
        assertEquals("New Name", existing.name)
        assertEquals("2222222222", existing.phone)
        assertEquals("INACTIVE", existing.status)

        val eventCaptor = argumentCaptor<CustomerUpdatedEvent>()
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        val event = eventCaptor.firstValue
        assertEquals("CUS-001", event.entityId)
        assertTrue(event.fieldChanges.containsKey("name"))
        assertTrue(event.fieldChanges.containsKey("phone"))
        assertTrue(event.fieldChanges.containsKey("status"))
    }

    @Test
    fun `updateCustomer returns null when customer not found`() {
        whenever(customerRepository.findByUid("missing")).thenReturn(null)

        val result = customerService.updateCustomer("missing", buildCustomer())

        assertNull(result)
        verify(customerRepository, never()).save(any())
        verify(eventPublisher, never()).publishEvent(any())
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

    @Test
    fun `updateOutstanding adjusts balance for charges`() {
        val existing = buildCustomer().apply {
            uid = "CUS-300"
            outstandingAmount = 100.0
        }
        whenever(customerRepository.findByUid("CUS-300")).thenReturn(existing)
        whenever(customerRepository.save(existing)).thenReturn(existing)

        val updated = customerService.updateOutstanding("CUS-300", 50.0, isPayment = false)

        assertEquals(150.0, updated?.outstandingAmount)
        verify(customerRepository).save(existing)
    }

    @Test
    fun `updateOutstanding does not allow negative balance`() {
        val existing = buildCustomer().apply {
            uid = "CUS-400"
            outstandingAmount = 40.0
        }
        whenever(customerRepository.findByUid("CUS-400")).thenReturn(existing)
        whenever(customerRepository.save(existing)).thenReturn(existing)

        val updated = customerService.updateOutstanding("CUS-400", 100.0, isPayment = true)

        assertEquals(0.0, updated?.outstandingAmount)
        verify(customerRepository).save(existing)
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
