package com.ampairs.business

import com.ampairs.business.exception.BusinessAlreadyExistsException
import com.ampairs.business.exception.BusinessNotFoundException
import com.ampairs.business.exception.InvalidBusinessDataException
import com.ampairs.business.model.Business
import com.ampairs.business.model.dto.BusinessCreateRequest
import com.ampairs.business.model.dto.BusinessOperationsUpdateRequest
import com.ampairs.business.model.dto.BusinessProfileUpdateRequest
import com.ampairs.business.model.dto.BusinessUpdateRequest
import com.ampairs.business.model.enums.BusinessType
import com.ampairs.business.repository.BusinessRepository
import com.ampairs.business.service.BusinessService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangePublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BusinessServiceTest {

    @Mock
    private lateinit var businessRepository: BusinessRepository

    @Mock
    private lateinit var entityChangePublisher: EntityChangePublisher

    private lateinit var service: BusinessService

    private val workspaceId = "WSP-1"

    private fun business(block: Business.() -> Unit = {}): Business =
        Business().apply {
            uid = "BIZ-1"
            ownerId = workspaceId
            name = "Acme"
            businessType = BusinessType.RETAIL
            timezone = "Asia/Kolkata"
            currency = "INR"
            block()
        }

    @BeforeEach
    fun setUp() {
        service = BusinessService(businessRepository, entityChangePublisher)
        whenever(businessRepository.save(any<Business>())).thenAnswer { it.arguments[0] }
        TenantContextHolder.setCurrentTenant(workspaceId)
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
    }

    @Test
    fun `getBusinessProfile returns the workspace business`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertSame(biz, service.getBusinessProfile())
    }

    @Test
    fun `getBusinessProfile throws when none exists`() {
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(null)

        assertThrows(BusinessNotFoundException::class.java) { service.getBusinessProfile() }
    }

    @Test
    fun `getBusinessProfile throws when no tenant context`() {
        TenantContextHolder.clearTenantContext()
        assertThrows(IllegalStateException::class.java) { service.getBusinessProfile() }
    }

    @Test
    fun `getBusinessByUid returns or throws`() {
        val biz = business()
        whenever(businessRepository.findByUid("BIZ-1")).thenReturn(biz)
        assertSame(biz, service.getBusinessByUid("BIZ-1"))

        whenever(businessRepository.findByUid("none")).thenReturn(null)
        assertThrows(BusinessNotFoundException::class.java) { service.getBusinessByUid("none") }
    }

    @Test
    fun `createBusinessProfile saves a new business`() {
        whenever(businessRepository.existsByOwnerId(workspaceId)).thenReturn(false)

        val result = service.createBusinessProfile(
            BusinessCreateRequest(name = "Acme", businessType = "RETAIL")
        )

        assertEquals("Acme", result.name)
        assertEquals(workspaceId, result.ownerId)
        verify(businessRepository).save(any<Business>())
        verify(entityChangePublisher).updated(eqType("business"), any())
    }

    @Test
    fun `createBusinessProfile rejects a duplicate`() {
        whenever(businessRepository.existsByOwnerId(workspaceId)).thenReturn(true)

        assertThrows(BusinessAlreadyExistsException::class.java) {
            service.createBusinessProfile(BusinessCreateRequest(name = "Acme", businessType = "RETAIL"))
        }
        verify(businessRepository, never()).save(any<Business>())
    }

    @Test
    fun `createBusinessProfile rejects invalid business hours`() {
        whenever(businessRepository.existsByOwnerId(workspaceId)).thenReturn(false)

        assertThrows(InvalidBusinessDataException::class.java) {
            service.createBusinessProfile(
                BusinessCreateRequest(
                    name = "Acme",
                    businessType = "RETAIL",
                    openingHours = "18:00",
                    closingHours = "09:00",
                )
            )
        }
    }

    @Test
    fun `updateBusinessProfile applies partial changes`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        val result = service.updateBusinessProfile(BusinessUpdateRequest(name = "Acme Corp"))

        assertEquals("Acme Corp", result.name)
        verify(entityChangePublisher).updated(eqType("business"), any())
    }

    @Test
    fun `updateBusinessProfile throws when business missing`() {
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(null)

        assertThrows(BusinessNotFoundException::class.java) {
            service.updateBusinessProfile(BusinessUpdateRequest(name = "x"))
        }
    }

    @Test
    fun `updateBusinessProfile validates hours only when supplied`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertThrows(InvalidBusinessDataException::class.java) {
            service.updateBusinessProfile(
                BusinessUpdateRequest(openingHours = "20:00", closingHours = "08:00")
            )
        }
    }

    @Test
    fun `businessProfileExists delegates to repository`() {
        whenever(businessRepository.existsByOwnerId(workspaceId)).thenReturn(true)
        assertTrue(service.businessProfileExists())

        whenever(businessRepository.existsByOwnerId(workspaceId)).thenReturn(false)
        assertFalse(service.businessProfileExists())
    }

    @Test
    fun `operatesOnDay reflects the operating days`() {
        val biz = business { operatingDays = listOf("Monday", "Tuesday") }
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertTrue(service.operatesOnDay("monday"))
        assertFalse(service.operatesOnDay("Sunday"))
    }

    @Test
    fun `getFullAddress joins address parts`() {
        val biz = business {
            addressLine1 = "1 Main St"
            city = "Pune"
            country = "India"
        }
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertEquals("1 Main St, Pune, India", service.getFullAddress())
    }

    @Test
    fun `overview, profile details and operations getters return the business`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertSame(biz, service.getBusinessOverview())
        assertSame(biz, service.getBusinessProfileDetails())
        assertSame(biz, service.getBusinessOperations())
    }

    @Test
    fun `updateBusinessProfileDetails replaces profile fields`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        val result = service.updateBusinessProfileDetails(
            BusinessProfileUpdateRequest(name = "New Name", businessType = BusinessType.SERVICE, city = "Mumbai")
        )

        assertEquals("New Name", result.name)
        assertEquals(BusinessType.SERVICE, result.businessType)
        assertEquals("Mumbai", result.city)
    }

    @Test
    fun `updateBusinessProfileDetails throws when missing`() {
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(null)
        assertThrows(BusinessNotFoundException::class.java) {
            service.updateBusinessProfileDetails(
                BusinessProfileUpdateRequest(name = "x", businessType = BusinessType.RETAIL)
            )
        }
    }

    @Test
    fun `updateBusinessOperations applies and validates hours`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        val result = service.updateBusinessOperations(
            BusinessOperationsUpdateRequest(
                timezone = "UTC",
                currency = "USD",
                language = "en",
                dateFormat = "YYYY-MM-DD",
                timeFormat = "24H",
                openingHours = "09:00",
                closingHours = "18:00",
            )
        )

        assertEquals("USD", result.currency)
        assertEquals("UTC", result.timezone)
    }

    @Test
    fun `updateBusinessOperations rejects invalid hours`() {
        val biz = business()
        whenever(businessRepository.findByOwnerId(workspaceId)).thenReturn(biz)

        assertThrows(InvalidBusinessDataException::class.java) {
            service.updateBusinessOperations(
                BusinessOperationsUpdateRequest(
                    timezone = "UTC",
                    currency = "USD",
                    language = "en",
                    dateFormat = "YYYY-MM-DD",
                    timeFormat = "24H",
                    openingHours = "19:00",
                    closingHours = "08:00",
                )
            )
        }
    }

    // helper to keep `verify(...).updated("business", ...)` readable with mockito-kotlin matchers
    private fun eqType(value: String): String = org.mockito.kotlin.eq(value)
}
