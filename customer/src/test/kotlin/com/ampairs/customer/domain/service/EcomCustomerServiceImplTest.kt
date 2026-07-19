package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.exception.EcomLinkInvalidException
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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

        val uid = service.resolveLinkedCustomerId("USR1", null)

        assertEquals("CUS-DEFAULT", uid)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `falls back to the first contact when none is marked default`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE"))
            .thenReturn(listOf(contact("CUS-A"), contact("CUS-B")))

        val uid = service.resolveLinkedCustomerId("USR1", null)

        assertEquals("CUS-A", uid)
    }

    @Test
    fun `returns null when the login has no contacts -- never auto-links by phone anymore`() {
        whenever(customerContactRepository.findByEcomUserIdAndStatus("USR1", "ACTIVE")).thenReturn(emptyList())

        val uid = service.resolveLinkedCustomerId("USR1", null)

        assertNull(uid)
        verify(customerRepository, never()).findFirstByPhone(any())
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `honours an explicitly chosen account only if the login is linked to it`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-CHOSEN", "USR1"))
            .thenReturn(contact("CUS-CHOSEN"))

        val uid = service.resolveLinkedCustomerId("USR1", requestedCustomerId = "CUS-CHOSEN")

        assertEquals("CUS-CHOSEN", uid)
        // Never falls through to the default-contact resolution.
        verify(customerContactRepository, never()).findByEcomUserIdAndStatus(any(), any())
    }

    @Test
    fun `rejects an explicitly chosen account the login is not linked to`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-OTHER", "USR1")).thenReturn(null)

        val uid = service.resolveLinkedCustomerId("USR1", requestedCustomerId = "CUS-OTHER")

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

    @Test
    fun `findLinkCandidateByPhone returns a candidate with contact details when a CRM customer matches`() {
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(
            customer("CUS-2").also {
                it.name = "Sharma Store"
                it.phone = "999"
                it.gstNumber = "27AAAPL1234C1ZV"
                it.street = "12 MG Road"
                it.city = "Pune"
                it.state = "MH"
                it.pincode = "411001"
            },
        )

        val candidate = service.findLinkCandidateByPhone("999")

        assertEquals("CUS-2", candidate?.customerId)
        assertEquals("Sharma Store", candidate?.name)
        assertEquals("999", candidate?.phone)
        assertEquals("27AAAPL1234C1ZV", candidate?.gstNumber)
        assertEquals("12 MG Road, Pune, MH, 411001", candidate?.address)
    }

    @Test
    fun `findLinkCandidateByPhone returns a null address and gstin when nothing is on file`() {
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(customer("CUS-2").also { it.name = "Sharma Store"; it.phone = "999" })

        val candidate = service.findLinkCandidateByPhone("999")

        assertNull(candidate?.gstNumber)
        assertNull(candidate?.address)
    }

    @Test
    fun `findLinkCandidateByPhone returns null when nothing matches`() {
        whenever(customerRepository.findFirstByPhone("999")).thenReturn(null)

        assertNull(service.findLinkCandidateByPhone("999"))
    }

    @Test
    fun `findLinkCandidateByPhone returns null for a blank phone without querying`() {
        assertNull(service.findLinkCandidateByPhone(""))
        verify(customerRepository, never()).findFirstByPhone(any())
    }

    @Test
    fun `confirmLink creates a contact when the phone matches`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-2", "USR1")).thenReturn(null)
        whenever(customerRepository.findByUid("CUS-2"))
            .thenReturn(customer("CUS-2").also { it.name = "Sharma Store"; it.phone = "999" })

        val account = service.confirmLink("USR1", "CUS-2", "Alice", "999", "a@x.com")

        assertEquals("CUS-2", account.customerId)
        assertEquals("Sharma Store", account.name)
        val captor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(captor.capture())
        assertEquals("CUS-2", captor.firstValue.customerId)
        assertEquals("USR1", captor.firstValue.ecomUserId)
    }

    @Test
    fun `confirmLink is idempotent when already linked`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-2", "USR1"))
            .thenReturn(contact("CUS-2", isDefault = true).also { it.role = "OWNER" })
        whenever(customerRepository.findByUid("CUS-2")).thenReturn(customer("CUS-2").also { it.name = "Sharma Store" })

        val account = service.confirmLink("USR1", "CUS-2", "Alice", "999", "a@x.com")

        assertEquals("Sharma Store", account.name)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `confirmLink rejects a customer that does not exist`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-X", "USR1")).thenReturn(null)
        whenever(customerRepository.findByUid("CUS-X")).thenReturn(null)

        assertThrows(EcomLinkInvalidException::class.java) {
            service.confirmLink("USR1", "CUS-X", "Alice", "999", "a@x.com")
        }
    }

    @Test
    fun `confirmLink rejects a phone mismatch -- never trusts the client's claim`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserId("CUS-2", "USR1")).thenReturn(null)
        whenever(customerRepository.findByUid("CUS-2")).thenReturn(customer("CUS-2").also { it.phone = "111" })

        assertThrows(EcomLinkInvalidException::class.java) {
            service.confirmLink("USR1", "CUS-2", "Alice", "999", "a@x.com")
        }
        verify(customerContactRepository, never()).save(any())
    }
}
