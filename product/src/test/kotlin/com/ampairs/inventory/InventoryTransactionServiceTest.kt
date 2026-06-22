package com.ampairs.inventory

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.PhysicalCountRequest
import com.ampairs.inventory.domain.dto.StockAdjustmentRequest
import com.ampairs.inventory.domain.dto.StockInRequest
import com.ampairs.inventory.domain.dto.StockOutRequest
import com.ampairs.inventory.domain.dto.StockTransferRequest
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.InventoryTransactionRepository
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.InventorySerialService
import com.ampairs.inventory.service.InventoryTransactionService
import com.ampairs.inventory.service.WarehouseService
import com.ampairs.setting.service.SettingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryTransactionServiceTest {

    @Mock
    private lateinit var transactionRepository: InventoryTransactionRepository

    @Mock
    private lateinit var itemService: InventoryItemService

    @Mock
    private lateinit var warehouseService: WarehouseService

    @Mock
    private lateinit var settingService: SettingService

    @Mock
    private lateinit var serialService: InventorySerialService

    private lateinit var service: InventoryTransactionService

    private fun item(block: InventoryItem.() -> Unit = {}): InventoryItem =
        InventoryItem().apply {
            uid = "INV-1"
            sku = "SKU-1"
            warehouseId = "WHR-1"
            currentStock = BigDecimal("100")
            reservedStock = BigDecimal("0")
            availableStock = BigDecimal("100")
            costPrice = BigDecimal("5")
            block()
        }

    @BeforeEach
    fun setUp() {
        service = InventoryTransactionService(
            transactionRepository, itemService, warehouseService, settingService, serialService
        )
        whenever(transactionRepository.save(any<InventoryTransaction>())).thenAnswer { it.arguments[0] }
        whenever(warehouseService.getWarehouseByUid(any())).thenReturn(Warehouse().apply { uid = "WHR-1" })
        whenever(transactionRepository.countTransactionsToday(any())).thenReturn(0L)
        whenever(transactionRepository.existsByTransactionNumber(any())).thenReturn(false)
        whenever(settingService.getBoolean("inventory", "allow_negative_stock")).thenReturn(false)
    }

    @Test
    fun `stockIn adds stock and sets balance`() {
        val it = item()
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockInRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("50"),
            unitCost = BigDecimal("4"),
            reason = "PURCHASE"
        )

        val txn = service.stockIn(request)

        assertEquals(Constants.TXN_TYPE_STOCK_IN, txn.transactionType)
        assertEquals(BigDecimal("150"), txn.balanceAfter)
        assertEquals(BigDecimal("200"), txn.totalCost) // 50 * 4
        verify(itemService).updateStockQuantities(eq("INV-1"), eq(BigDecimal("150")), eq(BigDecimal("0")))
    }

    @Test
    fun `stockIn with serial numbers creates serials`() {
        val it = item()
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockInRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("2"),
            unitCost = BigDecimal("4"),
            serialNumbers = listOf("SN-1", "SN-2")
        )

        service.stockIn(request)

        verify(serialService).createBulkSerials(any())
    }

    @Test
    fun `stockOut reduces stock`() {
        val it = item { currentStock = BigDecimal("100"); reservedStock = BigDecimal("0") }
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockOutRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("30"),
            reason = "SALE"
        )

        val txn = service.stockOut(request)

        assertEquals(Constants.TXN_TYPE_STOCK_OUT, txn.transactionType)
        assertEquals(BigDecimal("70"), txn.balanceAfter)
        verify(itemService).updateStockQuantities(eq("INV-1"), eq(BigDecimal("70")), any())
    }

    @Test
    fun `stockOut throws on insufficient stock when negative not allowed`() {
        val it = item { currentStock = BigDecimal("10"); reservedStock = BigDecimal("0") }
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        whenever(settingService.getBoolean("inventory", "allow_negative_stock")).thenReturn(false)
        val request = StockOutRequest(inventoryItemId = "INV-1", warehouseId = "WHR-1", quantity = BigDecimal("50"))

        assertThrows(IllegalStateException::class.java) { service.stockOut(request) }
        verify(transactionRepository, never()).save(any<InventoryTransaction>())
    }

    @Test
    fun `stockOut allows negative when configured`() {
        val it = item { currentStock = BigDecimal("10"); reservedStock = BigDecimal("0") }
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        whenever(settingService.getBoolean("inventory", "allow_negative_stock")).thenReturn(true)
        val request = StockOutRequest(inventoryItemId = "INV-1", warehouseId = "WHR-1", quantity = BigDecimal("50"))

        val txn = service.stockOut(request)

        assertEquals(BigDecimal("-40"), txn.balanceAfter)
    }

    @Test
    fun `stockOut with serials and reference marks them sold`() {
        val it = item()
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockOutRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("2"),
            serialNumbers = listOf("SN-1", "SN-2"),
            referenceType = "INVOICE",
            referenceId = "INV-DOC"
        )

        service.stockOut(request)

        verify(serialService).markSerialsAsSold(any(), eq("INVOICE"), eq("INV-DOC"), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `adjustStock positive increases stock`() {
        val it = item()
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockAdjustmentRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            adjustmentQuantity = BigDecimal("25"),
            reason = "FOUND"
        )

        val txn = service.adjustStock(request)

        assertEquals(Constants.TXN_TYPE_ADJUSTMENT, txn.transactionType)
        assertEquals(BigDecimal("125"), txn.balanceAfter)
        assertEquals(BigDecimal("25"), txn.quantity) // stored positive
        assertEquals(BigDecimal("5"), txn.unitCost) // positive uses cost price
    }

    @Test
    fun `adjustStock negative decreases stock and stores absolute quantity`() {
        val it = item()
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = StockAdjustmentRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            adjustmentQuantity = BigDecimal("-30"),
            reason = "DAMAGE"
        )

        val txn = service.adjustStock(request)

        assertEquals(BigDecimal("70"), txn.balanceAfter)
        assertEquals(BigDecimal("30"), txn.quantity)
        assertEquals(BigDecimal.ZERO, txn.unitCost) // negative uses zero cost
    }

    @Test
    fun `physicalCount reconciles to counted quantity`() {
        val it = item { currentStock = BigDecimal("100") }
        whenever(itemService.getInventoryItemByUid("INV-1")).thenReturn(it)
        val request = PhysicalCountRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            countedQuantity = BigDecimal("90")
        )

        val txn = service.physicalCount(request)

        assertEquals(Constants.TXN_TYPE_COUNT, txn.transactionType)
        assertEquals(BigDecimal("90"), txn.balanceAfter)
        assertEquals(BigDecimal("10"), txn.quantity) // abs difference
        verify(itemService).updateStockQuantities(eq("INV-1"), eq(BigDecimal("90")), any())
    }

    @Test
    fun `transferStock rejects same warehouse`() {
        val request = StockTransferRequest(
            inventoryItemId = "INV-1",
            fromWarehouseId = "WHR-1",
            toWarehouseId = "WHR-1",
            quantity = BigDecimal("5")
        )
        assertThrows(IllegalArgumentException::class.java) { service.transferStock(request) }
    }

    @Test
    fun `transferStock creates out and in transactions`() {
        val fromItem = item { uid = "INV-FROM"; warehouseId = "WHR-1"; currentStock = BigDecimal("100"); availableStock = BigDecimal("100") }
        val toItem = item { uid = "INV-TO"; warehouseId = "WHR-2"; currentStock = BigDecimal("20"); availableStock = BigDecimal("20") }
        whenever(itemService.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-1")).thenReturn(fromItem)
        whenever(itemService.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-2")).thenReturn(toItem)
        val request = StockTransferRequest(
            inventoryItemId = "INV-1",
            fromWarehouseId = "WHR-1",
            toWarehouseId = "WHR-2",
            quantity = BigDecimal("40")
        )

        val txns = service.transferStock(request)

        assertEquals(2, txns.size)
        assertTrue(txns.all { it.transactionType == Constants.TXN_TYPE_TRANSFER })
        assertEquals(BigDecimal("60"), txns[0].balanceAfter) // source 100-40
        assertEquals(BigDecimal("60"), txns[1].balanceAfter) // dest 20+40
    }

    @Test
    fun `transferStock throws when source item missing`() {
        whenever(itemService.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-1")).thenReturn(null)
        val request = StockTransferRequest(
            inventoryItemId = "INV-1",
            fromWarehouseId = "WHR-1",
            toWarehouseId = "WHR-2",
            quantity = BigDecimal("5")
        )
        assertThrows(IllegalArgumentException::class.java) { service.transferStock(request) }
    }

    @Test
    fun `transferStock throws on insufficient source stock`() {
        val fromItem = item { warehouseId = "WHR-1"; availableStock = BigDecimal("5") }
        val toItem = item { uid = "INV-TO"; warehouseId = "WHR-2" }
        whenever(itemService.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-1")).thenReturn(fromItem)
        whenever(itemService.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-2")).thenReturn(toItem)
        whenever(settingService.getBoolean("inventory", "allow_negative_stock")).thenReturn(false)
        val request = StockTransferRequest(
            inventoryItemId = "INV-1",
            fromWarehouseId = "WHR-1",
            toWarehouseId = "WHR-2",
            quantity = BigDecimal("50")
        )
        assertThrows(IllegalStateException::class.java) { service.transferStock(request) }
    }

    @Test
    fun `getTransactionByUid throws when missing`() {
        whenever(transactionRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getTransactionByUid("X") }
    }

    @Test
    fun `getTransactionByNumber throws when missing`() {
        whenever(transactionRepository.findByTransactionNumber("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getTransactionByNumber("X") }
    }

    @Test
    fun `getTransactionsByReference delegates`() {
        whenever(transactionRepository.findByReferenceTypeAndReferenceId("ORDER", "ORD-1"))
            .thenReturn(listOf(InventoryTransaction()))
        assertEquals(1, service.getTransactionsByReference("ORDER", "ORD-1").size)
    }
}
