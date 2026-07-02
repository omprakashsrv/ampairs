package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    private lateinit var service: EcomCustomerServiceImpl

    @BeforeEach
    fun setup() {
        service = EcomCustomerServiceImpl(customerRepository, customerContactRepository)
    }

    private fun customer(uid: String) = Customer().also { it.uid = uid }

    private fun contact(customerId: String, isDefault: Boolean = false) =
        CustomerContact().also { it.customerId = customerId; it.isDefault = isDefault }

    @Test
    fun `returns the default account when the login already has contacts`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE"))
            .thenReturn(listOf(contact("CUS-A"), contact("CUS-DEFAULT", isDefault = true)))

        val uid = service.resolveLinkedCustomerId("USR1", "999", "Alice", "a@x.com", null)

        assertEquals("CUS-DEFAULT", uid)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `falls back to the first contact when none is marked default`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE"))
            .thenReturn(listOf(contact("CUS-A"), contact("CUS-B")))

        val uid = service.resolveLinkedCustomerId("USR1", "999", "Alice", "a@x.com", null)

        assertEquals("CUS-A", uid)
    }

    @Test
    fun `attaches a contact to an existing customer matched by phone`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(emptyList())
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(customer("CUS-2"))

        val uid = service.resolveLinkedCustomerId("USR1", "999", "Alice", "a@x.com", null)

        assertEquals("CUS-2", uid)
        val captor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("CUS-2", saved.customerId)
        assertEquals("USR1", saved.ecomUserId)
        assertEquals(true, saved.isDefault)
    }

    @Test
    fun `returns null when nothing matches -- buyer is not linked to any distributor`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(emptyList())
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(null)

        val uid = service.resolveLinkedCustomerId("USR1", "999", "Alice", "a@x.com", null)

        assertNull(uid)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `skips phone lookup when phone is blank and returns null`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR9", "ACTIVE")).thenReturn(emptyList())

        val uid = service.resolveLinkedCustomerId("USR9", null, "Guest", "g@x.com", null)

        assertNull(uid)
        verify(customerRepository, never()).findFirstByPhone(any())
    }

    @Test
    fun `honours an explicitly chosen account only if the login is linked to it`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-CHOSEN", "USR1"))
            .thenReturn(contact("CUS-CHOSEN"))

        val uid = service.resolveLinkedCustomerId(
            "USR1", "999", "Alice", "a@x.com", requestedCustomerId = "CUS-CHOSEN",
        )

        assertEquals("CUS-CHOSEN", uid)
        // Never falls through to the default-contact or phone-match resolution.
        verify(customerContactRepository, never()).findByEcomUserIdAndStatus(any(), any())
        verify(customerRepository, never()).findFirstByPhone(any())
    }

    @Test
    fun `rejects an explicitly chosen account the login is not linked to`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-OTHER", "USR1")).thenReturn(null)

        val uid = service.resolveLinkedCustomerId(
            "USR1", "999", "Alice", "a@x.com", requestedCustomerId = "CUS-OTHER",
        )

        assertNull(uid)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `listAccountsForUser maps contacts to accounts labelled by the CRM account name`() {
        val c1 = contact("CUS-A", isDefault = true).also { it.role = "OWNER" }
        val c2 = contact("CUS-B").also { it.role = "WORKER"; it.name = "Bob" }
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(listOf(c1, c2))
        whenever(customerRepository.findByUid("CUS-A")).thenReturn(customer("CUS-A").also { it.name = "Acme" })
        whenever(customerRepository.findByUid("CUS-B")).thenReturn(null)

        val accounts = service.listAccountsForUser("USR1")

        assertEquals(2, accounts.size)
        assertEquals("CUS-A", accounts[0].customerId)
        assertEquals("Acme", accounts[0].name)
        assertEquals(true, accounts[0].isDefault)
        assertEquals("OWNER", accounts[0].role)
        // Falls back to the contact's own name when the CRM customer can't be loaded.
        assertEquals("Bob", accounts[1].name)
    }
}
