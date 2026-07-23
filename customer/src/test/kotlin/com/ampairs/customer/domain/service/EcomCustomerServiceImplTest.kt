package com.ampairs.customer.domain.service

import com.ampairs.core.domain.User
import com.ampairs.core.service.UserService
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.exception.CustomerNotFoundException
import com.ampairs.customer.exception.EcomLinkInvalidException
import com.ampairs.customer.exception.InvalidCustomerDataException
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class EcomCustomerServiceImplTest {

    @Mock private lateinit var customerRepository: CustomerRepository
    @Mock private lateinit var customerContactRepository: CustomerContactRepository
    @Mock private lateinit var userService: UserService

    private lateinit var service: EcomCustomerServiceImpl

    @BeforeEach
    fun setup() {
        service = EcomCustomerServiceImpl(customerRepository, customerContactRepository, userService)
    }

    private fun customer(uid: String) = Customer().also { it.uid = uid }

    private fun contact(customerId: String, isDefault: Boolean = false) =
        CustomerContact().also { it.customerId = customerId; it.isDefault = isDefault }

    // lenient: linkContactByPhone only reads a subset of these depending on the path taken
    // (e.g. the idempotent-link branch never touches email/getDisplayName at all).
    private fun appUser(uid: String, name: String = "", email: String? = null) = mock<User>(lenient = true) {
        on { this.uid } doReturn uid
        on { this.email } doReturn email
        on { this.getDisplayName() } doReturn name.ifBlank { uid }
    }

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
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-CHOSEN", "USR1", "ACTIVE"))
            .thenReturn(contact("CUS-CHOSEN"))

        val uid = service.resolveLinkedCustomerId("USR1", requestedCustomerId = "CUS-CHOSEN")

        assertEquals("CUS-CHOSEN", uid)
        // Never falls through to the default-contact resolution.
        verify(customerContactRepository, never()).findByEcomUserIdAndStatus(any(), any())
    }

    @Test
    fun `rejects an explicitly chosen account the login is not linked to`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-OTHER", "USR1", "ACTIVE")).thenReturn(null)

        val uid = service.resolveLinkedCustomerId("USR1", requestedCustomerId = "CUS-OTHER")

        assertNull(uid)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `rejects an explicitly chosen account that has been restricted`() {
        // A restricted (INACTIVE) contact must never satisfy the ACTIVE-scoped lookup.
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-RESTRICTED", "USR1", "ACTIVE")).thenReturn(null)

        val uid = service.resolveLinkedCustomerId("USR1", requestedCustomerId = "CUS-RESTRICTED")

        assertNull(uid)
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
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-2", "USR1", "ACTIVE")).thenReturn(null)
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
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-2", "USR1", "ACTIVE"))
            .thenReturn(contact("CUS-2", isDefault = true).also { it.role = "OWNER" })
        whenever(customerRepository.findByUid("CUS-2")).thenReturn(customer("CUS-2").also { it.name = "Sharma Store" })

        val account = service.confirmLink("USR1", "CUS-2", "Alice", "999", "a@x.com")

        assertEquals("Sharma Store", account.name)
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `confirmLink rejects a customer that does not exist`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-X", "USR1", "ACTIVE")).thenReturn(null)
        whenever(customerRepository.findByUid("CUS-X")).thenReturn(null)

        assertThrows(EcomLinkInvalidException::class.java) {
            service.confirmLink("USR1", "CUS-X", "Alice", "999", "a@x.com")
        }
    }

    @Test
    fun `confirmLink rejects a phone mismatch -- never trusts the client's claim`() {
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-2", "USR1", "ACTIVE")).thenReturn(null)
        whenever(customerRepository.findByUid("CUS-2")).thenReturn(customer("CUS-2").also { it.phone = "111" })

        assertThrows(EcomLinkInvalidException::class.java) {
            service.confirmLink("USR1", "CUS-2", "Alice", "999", "a@x.com")
        }
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `listContactsForCustomer returns every contact regardless of status`() {
        val active = contact("CUS-1", isDefault = true).also { it.uid = "CCT-1"; it.role = "OWNER"; it.status = "ACTIVE" }
        val restricted = contact("CUS-1").also { it.uid = "CCT-2"; it.role = "WORKER"; it.status = "INACTIVE" }
        whenever(customerContactRepository.findByCustomerId("CUS-1")).thenReturn(listOf(active, restricted))
        whenever(customerRepository.findByUid("CUS-1")).thenReturn(customer("CUS-1").also { it.name = "Sharma Store" })

        val contacts = service.listContactsForCustomer("CUS-1")

        assertEquals(2, contacts.size)
        assertEquals("Sharma Store", contacts[0].customerName)
        assertTrue(contacts[0].active)
        assertFalse(contacts[1].active)
    }

    @Test
    fun `linkContactByPhone links the app account registered with that phone`() {
        val user = appUser("USR-9", "Rahul")
        whenever(customerRepository.findByUid("CUS-1")).thenReturn(customer("CUS-1").also { it.name = "Sharma Store" })
        whenever(userService.getUserByPhone("999")).thenReturn(user)
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-1", "USR-9", "ACTIVE")).thenReturn(null)
        whenever(customerContactRepository.save(any<CustomerContact>())).thenAnswer { it.arguments[0] }

        val summary = service.linkContactByPhone("CUS-1", "999", null, "OWNER", isDefault = true)

        assertEquals("Sharma Store", summary.customerName)
        assertEquals("Rahul", summary.name)
        val captor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(captor.capture())
        assertEquals("USR-9", captor.firstValue.ecomUserId)
        assertEquals("ACTIVE", captor.firstValue.status)
    }

    @Test
    fun `linkContactByPhone is idempotent when already linked`() {
        val user = appUser("USR-9")
        whenever(customerRepository.findByUid("CUS-1")).thenReturn(customer("CUS-1").also { it.name = "Sharma Store" })
        whenever(userService.getUserByPhone("999")).thenReturn(user)
        whenever(customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus("CUS-1", "USR-9", "ACTIVE"))
            .thenReturn(contact("CUS-1"))

        service.linkContactByPhone("CUS-1", "999", null, "OWNER", isDefault = true)

        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `linkContactByPhone rejects an unknown customer`() {
        whenever(customerRepository.findByUid("CUS-X")).thenReturn(null)

        assertThrows(CustomerNotFoundException::class.java) {
            service.linkContactByPhone("CUS-X", "999", null, "OWNER", isDefault = false)
        }
    }

    @Test
    fun `linkContactByPhone rejects a phone with no app account`() {
        whenever(customerRepository.findByUid("CUS-1")).thenReturn(customer("CUS-1"))
        whenever(userService.getUserByPhone("999")).thenReturn(null)

        assertThrows(InvalidCustomerDataException::class.java) {
            service.linkContactByPhone("CUS-1", "999", null, "OWNER", isDefault = false)
        }
        verify(customerContactRepository, never()).save(any())
    }

    @Test
    fun `listAllContacts joins customer names across the workspace`() {
        val c1 = contact("CUS-1").also { it.uid = "CCT-1"; it.status = "ACTIVE" }
        val c2 = contact("CUS-2").also { it.uid = "CCT-2"; it.status = "INACTIVE" }
        whenever(customerContactRepository.findAll()).thenReturn(listOf(c1, c2))
        whenever(customerRepository.findByUidIn(listOf("CUS-1", "CUS-2"))).thenReturn(
            listOf(customer("CUS-1").also { it.name = "Sharma Store" }, customer("CUS-2").also { it.name = "Verma Kirana" }),
        )

        val contacts = service.listAllContacts()

        assertEquals(2, contacts.size)
        assertEquals("Sharma Store", contacts[0].customerName)
        assertTrue(contacts[0].active)
        assertEquals("Verma Kirana", contacts[1].customerName)
        assertFalse(contacts[1].active)
    }

    @Test
    fun `setContactActive restricts and re-enables a contact`() {
        val existing = contact("CUS-1").also { it.uid = "CCT-1"; it.status = "ACTIVE" }
        whenever(customerContactRepository.findByUid("CCT-1")).thenReturn(existing)
        whenever(customerContactRepository.save(any<CustomerContact>())).thenAnswer { it.arguments[0] }
        whenever(customerRepository.findByUid("CUS-1")).thenReturn(customer("CUS-1").also { it.name = "Sharma Store" })

        val restricted = service.setContactActive("CCT-1", active = false)

        assertFalse(restricted.active)
        val captor = argumentCaptor<CustomerContact>()
        verify(customerContactRepository).save(captor.capture())
        assertEquals("INACTIVE", captor.firstValue.status)
    }

    @Test
    fun `setContactActive rejects an unknown contact`() {
        whenever(customerContactRepository.findByUid("CCT-X")).thenReturn(null)

        assertThrows(EcomLinkInvalidException::class.java) {
            service.setContactActive("CCT-X", active = true)
        }
    }
}
