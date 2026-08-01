package com.ampairs.event.domain.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Pure unit tests for the entity change events — verifies the `getChanges()` payload maps and the
 * common BaseEntityEvent identity fields. No Spring context required (ApplicationEvent only needs a
 * source object).
 */
class DomainEventsTest {

    private val src = Any()

    @Test
    fun `customer created exposes name and type`() {
        val e = CustomerCreatedEvent(src, "ws", "CUS-1", "u", "d", "Acme", "RETAIL")
        assertEquals("ws", e.workspaceId)
        assertEquals("CUS-1", e.entityId)
        assertEquals(
            mapOf("action" to "created", "name" to "Acme", "customerType" to "RETAIL"),
            e.getChanges(),
        )
    }

    @Test
    fun `customer updated wraps field changes`() {
        val changes = mapOf<String, Any>("phone" to "123")
        val e = CustomerUpdatedEvent(src, "ws", "CUS-1", "u", "d", changes)
        val result = e.getChanges()
        assertEquals("updated", result["action"])
        assertSame(changes, result["changes"])
    }

    @Test
    fun `customer deleted exposes name`() {
        val e = CustomerDeletedEvent(src, "ws", "CUS-1", "u", "d", "Acme")
        assertEquals(mapOf("action" to "deleted", "name" to "Acme"), e.getChanges())
    }

    @Test
    fun `product created exposes sku`() {
        val e = ProductCreatedEvent(src, "ws", "PRD-1", "u", "d", "Widget", "SKU-9")
        val c = e.getChanges()
        assertEquals("created", c["action"])
        assertEquals("Widget", c["name"])
        assertEquals("SKU-9", c["sku"])
    }

    @Test
    fun `product stock changed computes difference`() {
        val e = ProductStockChangedEvent(src, "ws", "PRD-1", "u", "d", "Widget", 10.0, 7.0)
        val c = e.getChanges()
        assertEquals("stock_changed", c["action"])
        assertEquals(10.0, c["oldStock"])
        assertEquals(7.0, c["newStock"])
        assertEquals(-3.0, c["difference"])
    }

    @Test
    fun `product deleted and updated`() {
        assertEquals("deleted", ProductDeletedEvent(src, "ws", "PRD-1", "u", "d", "Widget").getChanges()["action"])
        val upd = ProductUpdatedEvent(src, "ws", "PRD-1", "u", "d", mapOf("price" to 5))
        assertEquals("updated", upd.getChanges()["action"])
    }

    @Test
    fun `order created exposes total amount`() {
        val e = OrderCreatedEvent(src, "ws", "ORD-1", "u", "d", "ORD-001", "Acme", 250.0)
        val c = e.getChanges()
        assertEquals("created", c["action"])
        assertEquals("ORD-001", c["orderNumber"])
        assertEquals(250.0, c["totalAmount"])
    }

    @Test
    fun `order status changed exposes both statuses`() {
        val e = OrderStatusChangedEvent(src, "ws", "ORD-1", "u", "d", "ORD-001", "NEW", "SHIPPED")
        val c = e.getChanges()
        assertEquals("status_changed", c["action"])
        assertEquals("NEW", c["oldStatus"])
        assertEquals("SHIPPED", c["newStatus"])
    }

    @Test
    fun `order updated and deleted`() {
        assertEquals("updated", OrderUpdatedEvent(src, "ws", "ORD-1", "u", "d", mapOf("x" to 1)).getChanges()["action"])
        assertEquals("deleted", OrderDeletedEvent(src, "ws", "ORD-1", "u", "d", "ORD-001").getChanges()["action"])
    }

    @Test
    fun `invoice paid exposes amount and method`() {
        val e = InvoicePaidEvent(src, "ws", "INV-1", "u", "d", "INV-001", 99.0, "CARD")
        val c = e.getChanges()
        assertEquals("paid", c["action"])
        assertEquals(99.0, c["paidAmount"])
        assertEquals("CARD", c["paymentMethod"])
    }

    @Test
    fun `invoice finalized and cancelled expose customer uid`() {
        val fin = InvoiceFinalizedEvent(src, "ws", "INV-1", "u", "d", "INV-001", "CUS-1", 500.0, 123L)
        val finChanges = fin.getChanges()
        assertEquals("finalized", finChanges["action"])
        assertEquals("CUS-1", finChanges["customerUid"])
        assertEquals(500.0, finChanges["totalAmount"])

        val cancelled = InvoiceCancelledEvent(src, "ws", "INV-1", "u", "d", "INV-001", "CUS-1")
        val cancelChanges = cancelled.getChanges()
        assertEquals("cancelled", cancelChanges["action"])
        assertEquals("CUS-1", cancelChanges["customerUid"])
    }

    @Test
    fun `invoice created updated status-changed deleted`() {
        assertEquals("created", InvoiceCreatedEvent(src, "ws", "INV-1", "u", "d", "INV-001", "Acme", 10.0).getChanges()["action"])
        assertEquals("updated", InvoiceUpdatedEvent(src, "ws", "INV-1", "u", "d", mapOf("x" to 1)).getChanges()["action"])
        val st = InvoiceStatusChangedEvent(src, "ws", "INV-1", "u", "d", "INV-001", "DRAFT", "INVOICED")
        assertEquals("status_changed", st.getChanges()["action"])
        assertEquals("deleted", InvoiceDeletedEvent(src, "ws", "INV-1", "u", "d", "INV-001").getChanges()["action"])
    }

    @Test
    fun `member events expose member user id and role`() {
        val added = MemberAddedEvent(src, "ws", "MEM-1", "u", "d", "user-2", "ADMIN")
        val addedChanges = added.getChanges()
        assertEquals("added", addedChanges["action"])
        assertEquals("user-2", addedChanges["memberUserId"])
        assertEquals("ADMIN", addedChanges["role"])

        assertEquals("removed", MemberRemovedEvent(src, "ws", "MEM-1", "u", "d", "user-2").getChanges()["action"])
        assertEquals("activated", MemberActivatedEvent(src, "ws", "MEM-1", "u", "d", "user-2").getChanges()["action"])
        assertEquals("deactivated", MemberDeactivatedEvent(src, "ws", "MEM-1", "u", "d", "user-2").getChanges()["action"])
    }

    @Test
    fun `product catalog change holds catalog attributes`() {
        val change = ProductCatalogChange(
            managementProductId = "PRD-1",
            listed = true,
            name = "Widget",
            price = BigDecimal("19.99"),
            mrp = BigDecimal("24.99"),
            stockQuantity = 5,
            imageUrls = listOf("a", "b"),
        )
        val event = ProductCatalogChangedEvent(workspaceId = "ws", changes = listOf(change))
        assertEquals("ws", event.workspaceId)
        assertEquals(1, event.changes.size)
        assertEquals("Widget", event.changes.first().name)
        assertEquals(BigDecimal("19.99"), event.changes.first().price)
        assertEquals(true, event.changes.first().listed)
    }
}
