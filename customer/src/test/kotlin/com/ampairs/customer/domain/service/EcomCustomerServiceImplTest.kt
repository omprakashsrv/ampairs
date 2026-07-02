package com.ampairs.customer.domain.service

import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class EcomCustomerServiceImplTest {

    @Mock private lateinit var customerRepository: CustomerRepository
    @Mock private lateinit var customerContactRepository: CustomerContactRepository
    @Mock private lateinit var customerService: CustomerService

    private lateinit var service: EcomCustomerServiceImpl

    @BeforeEach
    fun setup() {
        service = EcomCustomerServiceImpl(customerRepository, customerContactRepository, customerService)
    }

    private fun customer(uid: String) = Customer().also { it.uid = uid }

    private fun contact(customerId: String, isDefault: Boolean = false) =
        CustomerContact().also { it.customerId = customerId; it.isDefault = isDefault }

    @Test
    fun `returns the default account when the login already has contacts`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE"))
            .thenReturn(listOf(contact("CUS-A"), contact("CUS-DEFAULT", isDefault = true)))

        val uid = service.linkOrCreateEcomCustomer("USR1", "Alice", "999", "a@x.com", null, null)

        assertEquals("CUS-DEFAULT", uid)
        verify(customerService, never()).createCustomer(any())
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `attaches a contact to an existing customer matched by phone`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(emptyList())
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(customer("CUS-2"))

        val uid = service.linkOrCreateEcomCustomer("USR1", "Alice", "999", "a@x.com", null, null)

        assertEquals("CUS-2", uid)
        verify(customerService, never()).createCustomer(any())
        val captor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("CUS-2", saved.customerId)
        assertEquals("USR1", saved.ecomUserId)
        assertEquals(true, saved.isDefault)
    }

    @Test
    fun `creates a customer and its first contact when nothing matches`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(emptyList())
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(null)
        whenever(customerService.createCustomer(any())).thenReturn(customer("CUS-3"))

        val uid = service.linkOrCreateEcomCustomer("USR1", "Alice", "999", "a@x.com", Address(city = "Pune"), null)

        assertEquals("CUS-3", uid)
        val custCaptor = argumentCaptor<Customer>()
        verify(customerService).createCustomer(custCaptor.capture())
        assertEquals("Alice", custCaptor.firstValue.name)
        assertEquals("ONLINE", custCaptor.firstValue.customerType)
        assertEquals("Pune", custCaptor.firstValue.city)
        val contactCaptor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(contactCaptor.capture())
        assertEquals("CUS-3", contactCaptor.firstValue.customerId)
        assertEquals("USR1", contactCaptor.firstValue.ecomUserId)
    }

    @Test
    fun `skips phone lookup when phone is blank`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR9", "ACTIVE")).thenReturn(emptyList())
        whenever(customerService.createCustomer(any())).thenReturn(customer("CUS-9"))

        val uid = service.linkOrCreateEcomCustomer("USR9", "Guest", null, "g@x.com", null, null)

        assertEquals("CUS-9", uid)
        verify(customerRepository, never()).findFirstByPhone(any())
        verify(customerService).createCustomer(any())
    }
}
