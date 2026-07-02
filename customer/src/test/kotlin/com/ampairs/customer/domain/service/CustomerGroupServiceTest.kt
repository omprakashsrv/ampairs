package com.ampairs.customer.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.repository.CustomerGroupRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
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

class CustomerGroupServiceTest {

    private val repository: CustomerGroupRepository = mock()
    private val entityChangePublisher: EntityChangePublisher = mock()

    private lateinit var service: CustomerGroupService

    private fun group(block: CustomerGroup.() -> Unit = {}): CustomerGroup =
        CustomerGroup().apply {
            uid = "CGR-1"
            groupCode = "GOLD"
            name = "Gold"
            description = "Gold tier"
            displayOrder = 1
            active = true
            defaultDiscountPercentage = 5.0
            priorityLevel = 2
            block()
        }

    @BeforeEach
    fun setUp() {
        service = CustomerGroupService(repository, entityChangePublisher)
        whenever(repository.save(any<CustomerGroup>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `getCustomerGroupsAfterSync returns all rows when lastSync is null`() {
        whenever(repository.findAll(any<Pageable>())).thenReturn(PageImpl(listOf(group())))

        val result = service.getCustomerGroupsAfterSync(null, PageRequest.of(0, 10))

        assertEquals(1, result.content.size)
        verify(repository).findAll(any<Pageable>())
        verify(repository, never()).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `getCustomerGroupsAfterSync parses URL-encoded instant and queries incrementally`() {
        whenever(repository.findByUpdatedAtAfter(any<Instant>(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(group())))

        val encoded = "2026-03-15T09%3A30%3A00Z"
        service.getCustomerGroupsAfterSync(encoded, PageRequest.of(0, 10))

        verify(repository).findByUpdatedAtAfter(eq(Instant.parse("2026-03-15T09:30:00Z")), any<Pageable>())
    }

    @Test
    fun `getCustomerGroupsAfterSync falls back to findAll on bad lastSync`() {
        whenever(repository.findAll(any<Pageable>())).thenReturn(PageImpl(emptyList()))

        service.getCustomerGroupsAfterSync("garbage", PageRequest.of(0, 10))

        verify(repository).findAll(any<Pageable>())
        verify(repository, never()).findByUpdatedAtAfter(any(), any())
    }

    @Test
    fun `findByGroupCode matches case-insensitively and only active`() {
        val activeGroup = group { groupCode = "GOLD"; active = true }
        val inactiveGroup = group { uid = "CGR-2"; groupCode = "SILVER"; active = false }
        whenever(repository.findAll()).thenReturn(listOf(inactiveGroup, activeGroup))

        assertSame(activeGroup, service.findByGroupCode("gold"))
    }

    @Test
    fun `findByGroupCode returns null when only inactive match`() {
        whenever(repository.findAll()).thenReturn(listOf(group { active = false }))

        assertNull(service.findByGroupCode("GOLD"))
    }

    @Test
    fun `bulkUpsert updates existing group and publishes updated`() {
        val existing = group { name = "Old"; priorityLevel = 1 }
        whenever(repository.findAll()).thenReturn(listOf(existing))

        val incoming = group {
            name = "New"
            priorityLevel = 9
            defaultDiscountPercentage = 12.5
            refId = "REF-7"
        }

        val result = service.bulkUpsertCustomerGroups(listOf(incoming))

        assertEquals(1, result.size)
        assertEquals("New", existing.name)
        assertEquals(9, existing.priorityLevel)
        assertEquals(12.5, existing.defaultDiscountPercentage)
        assertEquals("REF-7", existing.refId)
        verify(entityChangePublisher).updated("customer_group", "CGR-1")
        verify(entityChangePublisher, never()).created(any(), any())
    }

    @Test
    fun `bulkUpsert creates a new group and publishes created`() {
        whenever(repository.findAll()).thenReturn(emptyList())
        whenever(repository.existsByUid("CGR-NEW")).thenReturn(false)

        val incoming = group { uid = "CGR-NEW"; groupCode = "PLATINUM" }

        service.bulkUpsertCustomerGroups(listOf(incoming))

        verify(repository).save(incoming)
        verify(entityChangePublisher).created("customer_group", "CGR-NEW")
    }

    @Test
    fun `bulkUpsert rejects a new group whose uid already exists`() {
        whenever(repository.findAll()).thenReturn(emptyList())
        whenever(repository.existsByUid("CGR-DUP")).thenReturn(true)

        val incoming = group { uid = "CGR-DUP"; groupCode = "X" }

        assertThrows(IllegalArgumentException::class.java) {
            service.bulkUpsertCustomerGroups(listOf(incoming))
        }
        verify(repository, never()).save(any<CustomerGroup>())
    }
}
