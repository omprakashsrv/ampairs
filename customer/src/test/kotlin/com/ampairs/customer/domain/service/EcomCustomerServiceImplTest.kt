package com.ampairs.customer.domain.service

import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.model.Customer
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
    @Mock private lateinit var customerService: CustomerService

    private lateinit var service: EcomCustomerServiceImpl

    @BeforeEach
    fun setup() {
        service = EcomCustomerServiceImpl(customerRepository, customerService)
    }

    private fun customer(uid: String, ecomUserId: String? = null, phone: String = "") =
        Customer().also { it.uid = uid; it.ecomUserId = ecomUserId; it.phone = phone }

    @Test
    fun `returns existing customer already linked to the ecom user`() {
        whenever(customerRepository.findFirstByEcomUserId("USR1"))
            .thenReturn(customer("CUS1", ecomUserId = "USR1"))

        val uid = service.linkOrCreateEcomCustomer("USR1", "Alice", "999", "a@x.com", null, null)

        assertEquals("CUS1", uid)
        verify(customerService, never()).createCustomer(any())
    }

    @Test
    fun `adopts an existing customer matched by phone and back-fills the ecom link`() {
        whenever(customerRepository.findFirstByEcomUserId("USR1")).thenReturn(null)
        val existing = customer("CUS2", ecomUserId = null, phone = "999")
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(existing)

        val uid = service.linkOrCreateEcomCustomer("USR1", "Alice", "999", "a@x.com", null, null)

        assertEquals("CUS2", uid)
        assertEquals("USR1", existing.ecomUserId)
        verify(customerService).updateCustomer(existing)
        verify(customerService, never()).createCustomer(any())
    }

    @Test
    fun `creates a new customer when nothing matches`() {
        whenever(customerRepository.findFirstByEcomUserId("USR1")).thenReturn(null)
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(null)
        whenever(customerService.createCustomer(any())).thenReturn(customer("CUS3", ecomUserId = "USR1"))

        val uid = service.linkOrCreateEcomCustomer(
            "USR1", "Alice", "999", "a@x.com", Address(city = "Pune"), null,
        )

        assertEquals("CUS3", uid)
        val captor = argumentCaptor<Customer>()
        verify(customerService).createCustomer(captor.capture())
        val created = captor.firstValue
        assertEquals("USR1", created.ecomUserId)
        assertEquals("999", created.phone)
        assertEquals("Alice", created.name)
        assertEquals("ONLINE", created.customerType)
        assertEquals("ONLINE", created.customerGroup)
        assertEquals("Pune", created.city)
    }

    @Test
    fun `skips phone dedup when phone is blank and creates a new customer`() {
        whenever(customerRepository.findFirstByEcomUserId("USR9")).thenReturn(null)
        whenever(customerService.createCustomer(any())).thenReturn(customer("CUS9", ecomUserId = "USR9"))

        val uid = service.linkOrCreateEcomCustomer("USR9", "Guest", null, "g@x.com", null, null)

        assertEquals("CUS9", uid)
        verify(customerRepository, never()).findFirstByPhone(any())
        verify(customerService).createCustomer(any())
    }
}
