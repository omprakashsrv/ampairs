package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.domain.model.MasterState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomerDtoMappersTest {

    // ---------- CustomerType ----------

    @Test
    fun `CustomerTypeUpdateRequest toCustomerType uppercases code and applies defaults`() {
        val req = CustomerTypeUpdateRequest(
            typeCode = "retail",
            name = null,
            description = null,
            displayOrder = null,
            active = null,
            defaultCreditLimit = null,
            defaultCreditDays = null,
            metadata = null,
        )

        val entity = req.toCustomerType()

        assertEquals("RETAIL", entity.typeCode)
        assertEquals("", entity.name)
        assertEquals(0, entity.displayOrder)
        assertTrue(entity.active)
        assertEquals(0.0, entity.defaultCreditLimit)
        assertEquals(0, entity.defaultCreditDays)
    }

    @Test
    fun `CustomerTypeUpdateRequest list maps all entries`() {
        val list = listOf(
            CustomerTypeUpdateRequest("a", "A", null, 1, true, 1.0, 1, null),
            CustomerTypeUpdateRequest("b", "B", null, 2, false, 2.0, 2, null),
        )
        val entities = list.toCustomerTypes()
        assertEquals(2, entities.size)
        assertEquals("A", entities[0].typeCode)
        assertEquals("B", entities[1].typeCode)
    }

    @Test
    fun `CustomerType asCustomerTypeResponse copies fields`() {
        val type = CustomerType().apply {
            uid = "CTY-1"; typeCode = "RETAIL"; name = "Retail"
            description = "d"; displayOrder = 3; active = true
            defaultCreditLimit = 5000.0; defaultCreditDays = 30; metadata = "m"
        }

        val res = type.asCustomerTypeResponse()

        assertEquals("CTY-1", res.uid)
        assertEquals("RETAIL", res.typeCode)
        assertEquals(5000.0, res.defaultCreditLimit)
        assertEquals(30, res.defaultCreditDays)
        assertEquals(1, listOf(type).asCustomerTypeResponses().size)
    }

    // ---------- CustomerGroup ----------

    @Test
    fun `CustomerGroupUpdateRequest toCustomerGroup uppercases code and applies defaults`() {
        val req = CustomerGroupUpdateRequest(
            groupCode = "gold",
            name = null,
            description = null,
            displayOrder = null,
            active = null,
            defaultDiscountPercentage = null,
            priorityLevel = null,
            metadata = null,
            refId = "REF-1",
        )

        val entity = req.toCustomerGroup()

        assertEquals("GOLD", entity.groupCode)
        assertEquals("", entity.name)
        assertTrue(entity.active)
        assertEquals(0.0, entity.defaultDiscountPercentage)
        assertEquals(0, entity.priorityLevel)
        assertEquals("REF-1", entity.refId)
    }

    @Test
    fun `CustomerGroupUpdateRequest list maps all entries`() {
        val list = listOf(
            CustomerGroupUpdateRequest("g1", "G1", null, 1, true, 1.0, 1, null),
            CustomerGroupUpdateRequest("g2", "G2", null, 2, false, 2.0, 2, null),
        )
        assertEquals(2, list.toCustomerGroups().size)
    }

    @Test
    fun `CustomerGroup asCustomerGroupResponse copies fields`() {
        val group = CustomerGroup().apply {
            uid = "CGR-1"; refId = "R"; groupCode = "GOLD"; name = "Gold"
            description = "d"; displayOrder = 2; active = false
            defaultDiscountPercentage = 7.5; priorityLevel = 4; metadata = "m"
        }

        val res = group.asCustomerGroupResponse()

        assertEquals("CGR-1", res.uid)
        assertEquals("GOLD", res.groupCode)
        assertEquals(7.5, res.defaultDiscountPercentage)
        assertEquals(4, res.priorityLevel)
        assertEquals(false, res.active)
        assertEquals(1, listOf(group).asCustomerGroupResponses().size)
    }

    // ---------- MasterState ----------

    @Test
    fun `MasterState asMasterStateResponse copies fields`() {
        val ms = MasterState().apply {
            uid = "MST-1"; stateCode = "KA"; name = "Karnataka"; shortName = "KA"
            countryCode = "IN"; countryName = "India"; gstCode = "29"; active = true
            population = 60_000_000L; areaSqKm = 191_791.0
        }

        val res = ms.asMasterStateResponse()

        assertEquals("KA", res.stateCode)
        assertEquals("Karnataka", res.name)
        assertEquals("29", res.gstCode)
        assertEquals(60_000_000L, res.population)
        assertNull(res.region)
        assertEquals(1, listOf(ms).asMasterStateResponses().size)
    }
}
