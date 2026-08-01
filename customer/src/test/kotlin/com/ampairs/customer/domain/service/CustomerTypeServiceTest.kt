package com.ampairs.customer.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.repository.CustomerTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.Instant

class CustomerTypeServiceTest {

    private val repository: CustomerTypeRepository = mock()
    private val entityChangePublisher: EntityChangePublisher = mock()

    private lateinit var service: CustomerTypeService

    private fun type(block: CustomerType.() -> Unit = {}): CustomerType =
        CustomerType().apply {
            uid = "CTY-1"
            typeCode = "RETAIL"
            name = "Retail"
            description = "Retail customers"
            displayOrder = 1
            active = true
            defaultCreditLimit = 5000.0
            defaultCreditDays = 30
            block()
        }

    @BeforeEach
    fun setUp() {
        service = CustomerTypeService(repository, entityChangePublisher)
        whenever(repository.save(any<CustomerType>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `getCustomerTypesAfterSync returns all rows when lastSync is null`() {
        val page: Page<CustomerType> = PageImpl(listOf(type()))
        whenever(repository.findAll(any<Pageable>())).thenReturn(page)

        val result = service.getCustomerTypesAfterSync(null, PageRequest.of(0, 10))

        assertEquals(1, result.content.size)
        verify(repository).findAll(any<Pageable>())
        verify(repository, never()).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `getCustomerTypesAfterSync returns all rows when lastSync is blank`() {
        whenever(repository.findAll(any<Pageable>())).thenReturn(PageImpl(emptyList()))

        service.getCustomerTypesAfterSync("   ", PageRequest.of(0, 10))

        verify(repository).findAll(any<Pageable>())
    }

    @Test
    fun `getCustomerTypesAfterSync parses URL-encoded instant and queries incrementally`() {
        val page: Page<CustomerType> = PageImpl(listOf(type()))
        whenever(repository.findByUpdatedAtAfter(any<Instant>(), any<Pageable>())).thenReturn(page)

        // URL-encoded ISO-8601 (':' -> %3A)
        val encoded = "2026-01-01T00%3A00%3A00Z"
        val result = service.getCustomerTypesAfterSync(encoded, PageRequest.of(0, 10))

        assertEquals(1, result.content.size)
        verify(repository).findByUpdatedAtAfter(eq(Instant.parse("2026-01-01T00:00:00Z")), any<Pageable>())
    }

    @Test
    fun `getCustomerTypesAfterSync falls back to findAll on unparseable lastSync`() {
        whenever(repository.findAll(any<Pageable>())).thenReturn(PageImpl(emptyList()))

        service.getCustomerTypesAfterSync("not-a-date", PageRequest.of(0, 10))

        verify(repository).findAll(any<Pageable>())
        verify(repository, never()).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `findByTypeCode matches case-insensitively and only active`() {
        val activeType = type { typeCode = "RETAIL"; active = true }
        val inactiveType = type { uid = "CTY-2"; typeCode = "OLD"; active = false }
        whenever(repository.findAll()).thenReturn(listOf(inactiveType, activeType))

        val found = service.findByTypeCode("retail")

        assertSame(activeType, found)
    }

    @Test
    fun `findByTypeCode skips inactive matches`() {
        val inactive = type { typeCode = "RETAIL"; active = false }
        whenever(repository.findAll()).thenReturn(listOf(inactive))

        assertNull(service.findByTypeCode("RETAIL"))
    }

    @Test
    fun `bulkUpsert updates an existing type by code and publishes updated`() {
        val existing = type { name = "Old Name"; defaultCreditDays = 10 }
        whenever(repository.findAll()).thenReturn(listOf(existing))

        val incoming = type {
            uid = "ignored"
            name = "New Name"
            defaultCreditDays = 45
            description = "changed"
            refId = "REF-99"
        }

        val result = service.bulkUpsertCustomerTypes(listOf(incoming))

        assertEquals(1, result.size)
        assertEquals("New Name", existing.name)
        assertEquals(45, existing.defaultCreditDays)
        assertEquals("REF-99", existing.refId)
        verify(repository).save(existing)
        verify(entityChangePublisher).updated("customer_type", "CTY-1")
        verify(entityChangePublisher, never()).created(any(), any())
    }

    @Test
    fun `bulkUpsert creates a new type when code not found and publishes created`() {
        whenever(repository.findAll()).thenReturn(emptyList())
        whenever(repository.existsByUid("CTY-NEW")).thenReturn(false)

        val incoming = type { uid = "CTY-NEW"; typeCode = "WHOLESALE" }

        val result = service.bulkUpsertCustomerTypes(listOf(incoming))

        assertEquals(1, result.size)
        verify(repository).save(incoming)
        verify(entityChangePublisher).created("customer_type", "CTY-NEW")
    }

    @Test
    fun `bulkUpsert rejects a new type whose uid already exists`() {
        whenever(repository.findAll()).thenReturn(emptyList())
        whenever(repository.existsByUid("CTY-DUP")).thenReturn(true)

        val incoming = type { uid = "CTY-DUP"; typeCode = "NEW" }

        assertThrows(IllegalArgumentException::class.java) {
            service.bulkUpsertCustomerTypes(listOf(incoming))
        }
        verify(repository, never()).save(any<CustomerType>())
    }
}
