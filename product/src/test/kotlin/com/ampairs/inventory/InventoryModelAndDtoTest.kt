package com.ampairs.inventory

import com.ampairs.inventory.domain.dto.InventoryRequest
import com.ampairs.inventory.domain.dto.asDatabaseModel
import com.ampairs.inventory.domain.dto.asResponse
import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.inventory.domain.model.InventoryLedger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class InventoryModelAndDtoTest {

    private fun request() = InventoryRequest(
        id = "INV-1",
        refId = "REF-1",
        productId = "PRD-1",
        productRefId = null,
        description = "A widget",
        code = "CODE1",
        taxCode = "GST18",
        baseUnitId = "UNT-1",
        active = true,
        mrp = 120.0,
        dp = 100.0,
        stock = 50.0,
        sellingPrice = 90.0,
        buyingPrice = 70.0,
        lastUpdated = null,
        createdAt = null,
        updatedAt = null
    )

    @Test
    fun `InventoryRequest asDatabaseModel maps fields`() {
        val inv = request().asDatabaseModel()
        assertEquals("INV-1", inv.uid)
        assertEquals("REF-1", inv.refId)
        assertEquals("PRD-1", inv.productId)
        assertEquals("UNT-1", inv.unitId)
        assertEquals("A widget", inv.description)
        assertEquals(120.0, inv.mrp)
        assertEquals(50.0, inv.stock)
        assertEquals(90.0, inv.sellingPrice)
        assertEquals(70.0, inv.buyingPrice)
    }

    @Test
    fun `List InventoryRequest asDatabaseModel maps all`() {
        val list = listOf(request(), request()).asDatabaseModel()
        assertEquals(2, list.size)
    }

    @Test
    fun `Inventory asResponse maps fields`() {
        val inv = Inventory().apply {
            uid = "INV-1"
            refId = "REF-1"
            productId = "PRD-1"
            unitId = "UNT-1"
            description = "Widget"
            mrp = 120.0
            dp = 100.0
            stock = 50.0
            sellingPrice = 90.0
            buyingPrice = 70.0
        }
        val resp = inv.asResponse()
        assertEquals("INV-1", resp.id)
        assertEquals("REF-1", resp.refId)
        assertEquals("PRD-1", resp.productId)
        assertEquals(50.0, resp.stock)
        assertEquals("UNT-1", resp.unitId)
    }

    @Test
    fun `List Inventory asResponse maps all`() {
        val list = listOf(Inventory(), Inventory()).asResponse()
        assertEquals(2, list.size)
    }

    @Test
    fun `InventoryLedger inflows outflows and net movement`() {
        val ledger = InventoryLedger().apply {
            openingStock = BigDecimal("100")
            stockIn = BigDecimal("30")
            transferIn = BigDecimal("10")
            adjustmentIn = BigDecimal("5")
            stockOut = BigDecimal("20")
            transferOut = BigDecimal("8")
            adjustmentOut = BigDecimal("2")
            averageCost = BigDecimal("4")
        }

        assertEquals(BigDecimal("45"), ledger.getTotalInflows()) // 30+10+5
        assertEquals(BigDecimal("30"), ledger.getTotalOutflows()) // 20+8+2
        assertEquals(BigDecimal("15"), ledger.getNetMovement())

        ledger.calculateClosingStock() // 100 + 45 - 30 = 115
        assertEquals(BigDecimal("115"), ledger.closingStock)

        ledger.calculateClosingValue() // 115 * 4 = 460
        assertEquals(BigDecimal("460"), ledger.closingValue)
    }

    @Test
    fun `InventoryLedger date helpers round-trip`() {
        val date = LocalDate.of(2025, 6, 15)
        val instant = InventoryLedger.ledgerDateFromLocalDate(date)
        assertEquals(date, instant.atZone(ZoneId.of("UTC")).toLocalDate())

        val ledger = InventoryLedger().apply { ledgerDate = instant }
        assertEquals(date, ledger.getLedgerLocalDate())
    }
}
