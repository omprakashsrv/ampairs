package com.ampairs.inventory

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventoryLedger
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryLedgerRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import com.ampairs.inventory.service.InventoryLedgerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryLedgerServiceTest {

    @Mock
    private lateinit var ledgerRepository: InventoryLedgerRepository

    @Mock
    private lateinit var transactionRepository: InventoryTransactionRepository

    @Mock
    private lateinit var itemRepository: InventoryItemRepository

    private lateinit var service: InventoryLedgerService

    private val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(LocalDate.of(2025, 1, 15))

    private fun txn(type: String, qty: BigDecimal, warehouse: String = "WHR-1", to: String? = null, unitCost: BigDecimal = BigDecimal.ZERO): InventoryTransaction =
        InventoryTransaction().apply {
            transactionType = type
            quantity = qty
            warehouseId = warehouse
            toWarehouseId = to
            this.unitCost = unitCost
            calculateTotalCost()
        }

    @BeforeEach
    fun setUp() {
        service = InventoryLedgerService(ledgerRepository, transactionRepository, itemRepository)
        whenever(ledgerRepository.save(any<InventoryLedger>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `getLedgerByUid throws when missing`() {
        whenever(ledgerRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getLedgerByUid("X") }
    }

    @Test
    fun `generateDailyLedgerForDate returns 0 with no transactions`() {
        whenever(transactionRepository.findByTransactionDateBetween(any(), any())).thenReturn(emptyList())
        val count = service.generateDailyLedgerForDate(LocalDate.of(2025, 1, 15))
        assertEquals(0, count)
    }

    @Test
    fun `generateDailyLedgerForDate creates one entry per item-warehouse`() {
        val txns = listOf(
            txn(Constants.TXN_TYPE_STOCK_IN, BigDecimal("10")).apply { inventoryItemId = "INV-1"; warehouseId = "WHR-1" },
            txn(Constants.TXN_TYPE_STOCK_OUT, BigDecimal("3")).apply { inventoryItemId = "INV-1"; warehouseId = "WHR-1" },
            txn(Constants.TXN_TYPE_STOCK_IN, BigDecimal("5")).apply { inventoryItemId = "INV-2"; warehouseId = "WHR-1" }
        )
        whenever(transactionRepository.findByTransactionDateBetween(any(), any())).thenReturn(txns)
        whenever(ledgerRepository.findByInventoryItemIdAndWarehouseIdAndLedgerDate(any(), any(), any())).thenReturn(null)
        whenever(ledgerRepository.findMostRecentBefore(any(), any(), any())).thenReturn(null)
        whenever(itemRepository.findByUid(any())).thenReturn(InventoryItem().apply { costPrice = BigDecimal("4") })

        val count = service.generateDailyLedgerForDate(LocalDate.of(2025, 1, 15))

        assertEquals(2, count) // two distinct item/warehouse groups
    }

    @Test
    fun `generateLedgerEntry aggregates movements and computes closing`() {
        whenever(ledgerRepository.findByInventoryItemIdAndWarehouseIdAndLedgerDate("INV-1", "WHR-1", ledgerDate)).thenReturn(null)
        whenever(ledgerRepository.findMostRecentBefore("INV-1", "WHR-1", ledgerDate)).thenReturn(
            InventoryLedger().apply { closingStock = BigDecimal("100") }
        )
        whenever(itemRepository.findByUid("INV-1")).thenReturn(InventoryItem().apply { costPrice = BigDecimal("2") })

        val txns = listOf(
            txn(Constants.TXN_TYPE_STOCK_IN, BigDecimal("50"), unitCost = BigDecimal("3")).apply { inventoryItemId = "INV-1" },
            txn(Constants.TXN_TYPE_STOCK_OUT, BigDecimal("20")).apply { inventoryItemId = "INV-1" },
            txn(Constants.TXN_TYPE_TRANSFER, BigDecimal("10"), warehouse = "WHR-1", to = "WHR-2").apply { inventoryItemId = "INV-1" }, // transfer out
            txn(Constants.TXN_TYPE_TRANSFER, BigDecimal("5"), warehouse = "WHR-9", to = "WHR-1").apply { inventoryItemId = "INV-1" },  // transfer in
            txn(Constants.TXN_TYPE_ADJUSTMENT, BigDecimal("8")).apply { inventoryItemId = "INV-1" },   // adjustment in
            txn(Constants.TXN_TYPE_COUNT, BigDecimal("-2")).apply { inventoryItemId = "INV-1" }        // adjustment out
        )

        val ledger = service.generateLedgerEntry("INV-1", "WHR-1", ledgerDate, txns)

        assertEquals(BigDecimal("100"), ledger.openingStock)
        assertEquals(BigDecimal("50"), ledger.stockIn)
        assertEquals(BigDecimal("20"), ledger.stockOut)
        assertEquals(BigDecimal("10"), ledger.transferOut)
        assertEquals(BigDecimal("5"), ledger.transferIn)
        assertEquals(BigDecimal("8"), ledger.adjustmentIn)
        assertEquals(BigDecimal("2"), ledger.adjustmentOut)
        // closing = 100 + 50 + 5 + 8 - 20 - 10 - 2 = 131
        assertEquals(BigDecimal("131"), ledger.closingStock)
        // average cost from stock-in: totalCost 150 / qty 50 = 3.00
        assertEquals(BigDecimal("3.00"), ledger.averageCost)
        assertEquals(BigDecimal("393.00"), ledger.closingValue) // 131 * 3.00
    }

    @Test
    fun `generateLedgerEntry uses item cost when no stock-in`() {
        whenever(ledgerRepository.findByInventoryItemIdAndWarehouseIdAndLedgerDate("INV-1", "WHR-1", ledgerDate)).thenReturn(null)
        whenever(ledgerRepository.findMostRecentBefore(any(), any(), any())).thenReturn(null)
        whenever(itemRepository.findByUid("INV-1")).thenReturn(InventoryItem().apply { costPrice = BigDecimal("7") })

        val txns = listOf(txn(Constants.TXN_TYPE_STOCK_OUT, BigDecimal("5")).apply { inventoryItemId = "INV-1" })

        val ledger = service.generateLedgerEntry("INV-1", "WHR-1", ledgerDate, txns)

        assertEquals(BigDecimal("7"), ledger.averageCost)
        assertEquals(BigDecimal("-5"), ledger.closingStock) // 0 opening - 5 out
    }

    @Test
    fun `generateLedgerForDateRange iterates all days`() {
        whenever(transactionRepository.findByTransactionDateBetween(any(), any())).thenReturn(emptyList())
        val count = service.generateLedgerForDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3))
        assertEquals(0, count) // no transactions but 3 days iterated without error
    }

    @Test
    fun `getWarehouseStockValue returns zero when null`() {
        whenever(ledgerRepository.calculateWarehouseValueOnDate("WHR-1", ledgerDate)).thenReturn(null)
        assertEquals(BigDecimal.ZERO, service.getWarehouseStockValue("WHR-1", ledgerDate))

        whenever(ledgerRepository.calculateWarehouseValueOnDate("WHR-1", ledgerDate)).thenReturn(BigDecimal("500"))
        assertEquals(BigDecimal("500"), service.getWarehouseStockValue("WHR-1", ledgerDate))
    }

    @Test
    fun `getWarehouseStockQuantity returns zero when null`() {
        whenever(ledgerRepository.calculateWarehouseStockOnDate("WHR-1", ledgerDate)).thenReturn(null)
        assertEquals(BigDecimal.ZERO, service.getWarehouseStockQuantity("WHR-1", ledgerDate))
    }

    @Test
    fun `query delegations pass through`() {
        whenever(ledgerRepository.findByDate(ledgerDate)).thenReturn(listOf(InventoryLedger()))
        whenever(ledgerRepository.findEntriesWithMovementOnDate(ledgerDate)).thenReturn(listOf(InventoryLedger()))
        assertEquals(1, service.getDailyLedger(ledgerDate).size)
        assertEquals(1, service.getItemsWithMovement(ledgerDate).size)
    }
}
