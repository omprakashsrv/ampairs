package com.ampairs.inventory

import com.ampairs.inventory.exception.BatchExpiredException
import com.ampairs.inventory.exception.BatchHasStockException
import com.ampairs.inventory.exception.BatchNotFoundException
import com.ampairs.inventory.exception.ConfigurationNotFoundException
import com.ampairs.inventory.exception.DuplicateBatchNumberException
import com.ampairs.inventory.exception.DuplicateSKUException
import com.ampairs.inventory.exception.DuplicateSerialNumberException
import com.ampairs.inventory.exception.DuplicateWarehouseCodeException
import com.ampairs.inventory.exception.InsufficientBatchStockException
import com.ampairs.inventory.exception.InsufficientSerialsException
import com.ampairs.inventory.exception.InsufficientStockException
import com.ampairs.inventory.exception.InvalidConfigurationException
import com.ampairs.inventory.exception.InvalidSerialStatusException
import com.ampairs.inventory.exception.InvalidTransactionException
import com.ampairs.inventory.exception.InventoryException
import com.ampairs.inventory.exception.InventoryItemNotFoundException
import com.ampairs.inventory.exception.LedgerGenerationException
import com.ampairs.inventory.exception.LedgerNotFoundException
import com.ampairs.inventory.exception.NegativeStockNotAllowedException
import com.ampairs.inventory.exception.SameWarehouseTransferException
import com.ampairs.inventory.exception.SerialNumberNotFoundException
import com.ampairs.inventory.exception.SerialNumberRequiredException
import com.ampairs.inventory.exception.TransactionNotFoundException
import com.ampairs.inventory.exception.TransactionValidationException
import com.ampairs.inventory.exception.WarehouseHasInventoryException
import com.ampairs.inventory.exception.WarehouseNotFoundException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InventoryExceptionsTest {

    @Test
    fun `all exceptions are InventoryException and carry message context`() {
        val cases = listOf(
            WarehouseNotFoundException("WHR-1") to "WHR-1",
            DuplicateWarehouseCodeException("WH-1") to "WH-1",
            WarehouseHasInventoryException("WHR-1", 5) to "5",
            InventoryItemNotFoundException("INV-1") to "INV-1",
            InsufficientStockException("INV-1", "Widget", "2", "10") to "Widget",
            NegativeStockNotAllowedException("INV-1", "-3") to "-3",
            DuplicateSKUException("SKU-1") to "SKU-1",
            BatchNotFoundException("BCH-1") to "BCH-1",
            DuplicateBatchNumberException("B-1", "INV-1", "WHR-1") to "B-1",
            InsufficientBatchStockException("BCH-1", "B-1", "1", "5") to "B-1",
            BatchHasStockException("BCH-1", "10", "2") to "BCH-1",
            BatchExpiredException("B-1", "2025-01-01") to "2025-01-01",
            SerialNumberNotFoundException("SN-1") to "SN-1",
            DuplicateSerialNumberException("SN-1") to "SN-1",
            InvalidSerialStatusException("SN-1", "SOLD", "delete") to "delete",
            SerialNumberRequiredException("INV-1", "Widget") to "Widget",
            InsufficientSerialsException("INV-1", "WHR-1", 1, 5) to "Requested: 5",
            TransactionNotFoundException("TXN-1") to "TXN-1",
            InvalidTransactionException("bad txn") to "bad txn",
            TransactionValidationException("missing field") to "missing field",
            SameWarehouseTransferException("WHR-1") to "WHR-1",
            LedgerNotFoundException("LDG-1") to "LDG-1",
            LedgerGenerationException("2025-01-01", "boom") to "boom",
            ConfigurationNotFoundException() to "configuration",
            InvalidConfigurationException("hour", "99", "out of range") to "out of range"
        )

        cases.forEach { (ex, fragment) ->
            val asBase: InventoryException = ex // compile-time proof every case is an InventoryException
            assertTrue(asBase.message != null, "${ex::class.simpleName} should carry a message")
            assertTrue(
                ex.message!!.contains(fragment, ignoreCase = true),
                "${ex::class.simpleName} message '${ex.message}' should contain '$fragment'"
            )
        }
    }

    @Test
    fun `exceptions propagate cause`() {
        val cause = RuntimeException("root")
        val ex = WarehouseNotFoundException("WHR-1", cause)
        assertTrue(ex.cause === cause)
    }
}
