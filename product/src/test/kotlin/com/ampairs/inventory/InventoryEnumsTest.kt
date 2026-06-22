package com.ampairs.inventory

import com.ampairs.inventory.domain.enums.SerialStatus
import com.ampairs.inventory.domain.enums.TransactionReason
import com.ampairs.inventory.domain.enums.TransactionType
import com.ampairs.inventory.domain.enums.WarehouseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InventoryEnumsTest {

    @Test
    fun `TransactionType fromValue and getAllValues`() {
        assertEquals(TransactionType.STOCK_IN, TransactionType.fromValue("STOCK_IN"))
        assertNull(TransactionType.fromValue("NOPE"))
        assertTrue(TransactionType.getAllValues().containsAll(listOf("STOCK_IN", "STOCK_OUT", "TRANSFER", "ADJUSTMENT", "COUNT")))
        assertEquals("Inter-Warehouse Transfer", TransactionType.TRANSFER.description)
    }

    @Test
    fun `TransactionReason fromValue and type filtering`() {
        assertEquals(TransactionReason.PURCHASE, TransactionReason.fromValue("PURCHASE"))
        assertNull(TransactionReason.fromValue("NOPE"))
        assertTrue(TransactionReason.getAllValues().contains("SALE"))

        val stockInReasons = TransactionReason.getReasonsForType(TransactionType.STOCK_IN)
        assertTrue(stockInReasons.contains(TransactionReason.PURCHASE))
        assertTrue(stockInReasons.contains(TransactionReason.RETURN))
        assertFalse(stockInReasons.contains(TransactionReason.SALE))

        assertTrue(TransactionReason.isValidForType(TransactionReason.SALE, TransactionType.STOCK_OUT))
        assertFalse(TransactionReason.isValidForType(TransactionReason.SALE, TransactionType.STOCK_IN))
    }

    @Test
    fun `SerialStatus transitions`() {
        assertTrue(SerialStatus.AVAILABLE.canTransitionTo(SerialStatus.RESERVED))
        assertTrue(SerialStatus.AVAILABLE.canTransitionTo(SerialStatus.SOLD))
        assertFalse(SerialStatus.SOLD.canTransitionTo(SerialStatus.AVAILABLE))

        assertEquals(SerialStatus.SOLD, SerialStatus.fromValue("SOLD"))
        assertNull(SerialStatus.fromValue("NOPE"))
        assertTrue(SerialStatus.getAllValues().contains("DAMAGED"))
    }

    @Test
    fun `SerialStatus validateTransition throws on invalid`() {
        assertThrows(IllegalArgumentException::class.java) {
            SerialStatus.validateTransition(SerialStatus.SOLD, SerialStatus.AVAILABLE)
        }
        // valid does not throw
        SerialStatus.validateTransition(SerialStatus.AVAILABLE, SerialStatus.RESERVED)
    }

    @Test
    fun `WarehouseType lookups and groupings`() {
        assertEquals(WarehouseType.STORE, WarehouseType.fromValue("STORE"))
        assertNull(WarehouseType.fromValue("NOPE"))
        assertTrue(WarehouseType.getAllValues().contains("FACTORY"))

        assertEquals(listOf(WarehouseType.STORE, WarehouseType.SHOWROOM), WarehouseType.getRetailTypes())
        assertTrue(WarehouseType.getStorageTypes().contains(WarehouseType.WAREHOUSE))
        assertEquals(listOf(WarehouseType.FACTORY), WarehouseType.getManufacturingTypes())
    }
}
