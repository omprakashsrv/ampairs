package com.ampairs.business

import com.ampairs.business.model.Business
import com.ampairs.business.model.BusinessImage
import com.ampairs.business.model.BusinessImageType
import com.ampairs.business.model.dto.BusinessCreateRequest
import com.ampairs.business.model.dto.BusinessUpdateRequest
import com.ampairs.business.model.dto.asBusinessImageResponse
import com.ampairs.business.model.dto.asBusinessImageResponses
import com.ampairs.business.model.dto.asBusinessOperationsResponse
import com.ampairs.business.model.dto.asBusinessOverviewResponse
import com.ampairs.business.model.dto.asBusinessProfileResponse
import com.ampairs.business.model.dto.asBusinessResponse
import com.ampairs.business.model.dto.asBusinessResponses
import com.ampairs.business.model.dto.applyUpdate
import com.ampairs.business.model.dto.toBusiness
import com.ampairs.business.model.enums.BusinessType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BusinessMapperTest {

    @Test
    fun `BusinessType fromString is case insensitive and validates`() {
        assertEquals(BusinessType.RETAIL, BusinessType.fromString("retail"))
        assertEquals(BusinessType.SERVICE, BusinessType.fromString("SERVICE"))
        assertTrue(BusinessType.isValid("wholesale"))
        assertFalse(BusinessType.isValid("nope"))
        assertThrows(IllegalArgumentException::class.java) { BusinessType.fromString("nope") }
    }

    @Test
    fun `BusinessImageType fromString validates`() {
        assertEquals(BusinessImageType.GALLERY, BusinessImageType.fromString("gallery"))
        assertThrows(IllegalArgumentException::class.java) { BusinessImageType.fromString("nope") }
    }

    @Test
    fun `validateBusinessHours throws when closing is before opening`() {
        val biz = Business().apply { openingHours = "18:00"; closingHours = "09:00" }
        assertThrows(IllegalStateException::class.java) { biz.validateBusinessHours() }
    }

    @Test
    fun `validateBusinessHours passes when ordering is correct or hours absent`() {
        Business().apply { openingHours = "09:00"; closingHours = "18:00" }.validateBusinessHours()
        Business().validateBusinessHours() // no hours -> no-op
    }

    @Test
    fun `operatesOn and getFullAddress`() {
        val biz = Business().apply {
            operatingDays = listOf("Monday", "Friday")
            addressLine1 = "1 Main"
            city = "Pune"
            country = "India"
        }
        assertTrue(biz.operatesOn("monday"))
        assertFalse(biz.operatesOn("Sunday"))
        assertEquals("1 Main, Pune, India", biz.getFullAddress())
        assertEquals(BusinessType.RETAIL.description, biz.getBusinessTypeDescription())
        assertTrue(biz.toString().contains("BIZ").not() || biz.toString().contains("name"))
    }

    @Test
    fun `toBusiness maps create request fields`() {
        val req = BusinessCreateRequest(
            name = "Acme",
            businessType = "service",
            description = "desc",
            ownerName = "Bob",
            city = "Pune",
            phone = "+919999999999",
            email = "a@b.com",
            currency = "USD",
            timezone = "UTC",
        )
        val biz = req.toBusiness("WSP-1", "USR-1")

        assertEquals("WSP-1", biz.ownerId)
        assertEquals("Acme", biz.name)
        assertEquals(BusinessType.SERVICE, biz.businessType)
        assertEquals("Pune", biz.city)
        assertEquals("USD", biz.currency)
        assertEquals("USR-1", biz.createdBy)
        assertTrue(biz.active)
    }

    @Test
    fun `applyUpdate only changes provided fields`() {
        val biz = Business().apply { name = "Old"; city = "OldCity"; currency = "INR" }
        biz.applyUpdate(BusinessUpdateRequest(name = "New", active = false), "USR-2")

        assertEquals("New", biz.name)
        assertEquals("OldCity", biz.city) // untouched
        assertEquals("INR", biz.currency) // untouched
        assertFalse(biz.active)
        assertEquals("USR-2", biz.updatedBy)
    }

    @Test
    fun `response mappers expose expected fields`() {
        val biz = Business().apply {
            uid = "BIZ-1"
            name = "Acme"
            businessType = BusinessType.RETAIL
            logoUrl = "object-key"
            email = "a@b.com"
            city = "Pune"
        }

        val resp = biz.asBusinessResponse()
        assertEquals("BIZ-1", resp.uid)
        assertEquals("RETAIL", resp.businessType)
        // logo URL becomes an API path, not the raw object key
        assertEquals("/business/v1/businesses/logo", resp.logoUrl)

        assertEquals(1, listOf(biz).asBusinessResponses().size)

        val overview = biz.asBusinessOverviewResponse()
        assertEquals("Acme", overview.name)
        assertEquals("Pune", overview.address)

        val profile = biz.asBusinessProfileResponse()
        assertEquals("a@b.com", profile.email)

        val ops = biz.asBusinessOperationsResponse()
        assertNotNull(ops.timezone)
    }

    @Test
    fun `asBusinessResponse leaves logo url null when no logo`() {
        val resp = Business().apply { uid = "BIZ-2" }.asBusinessResponse()
        assertNull(resp.logoUrl)
        assertNull(resp.logoThumbnailUrl)
    }

    @Test
    fun `business image response maps to api paths`() {
        val img = BusinessImage().apply {
            uid = "IMG-1"
            businessId = "BIZ-1"
            imageType = BusinessImageType.GALLERY
            imageUrl = "object-key"
            thumbnailUrl = "thumb-key"
            isPrimary = true
            displayOrder = 2
        }

        val resp = img.asBusinessImageResponse()
        assertEquals("IMG-1", resp.uid)
        assertEquals("/business/v1/businesses/images/IMG-1/file", resp.imageUrl)
        assertEquals("/business/v1/businesses/images/IMG-1/thumbnail", resp.thumbnailUrl)
        assertTrue(resp.isPrimary)
        assertEquals(1, listOf(img).asBusinessImageResponses().size)
    }
}
