package com.ampairs.event.domain.kafka

import com.ampairs.core.domain.model.Address
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class EcomKafkaEventsTest {

    @Test
    fun `catalog event holds product attributes and event type`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val event = EcomCatalogEvent(
            eventType = CatalogEventType.PRICE_UPDATED,
            workspaceId = "ws-1",
            storefrontId = "sf-1",
            managementProductId = "PRD-1",
            name = "Widget",
            price = BigDecimal("19.99"),
            mrp = BigDecimal("24.99"),
            stockQuantity = 12,
            imageUrls = listOf("a"),
            publishedAt = now,
        )

        assertEquals(CatalogEventType.PRICE_UPDATED, event.eventType)
        assertEquals("PRD-1", event.managementProductId)
        assertEquals(BigDecimal("19.99"), event.price)
        assertEquals(now, event.publishedAt)
        assertNull(event.description)
    }

    @Test
    fun `catalog event data-class equality and copy`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val base = EcomCatalogEvent(
            eventType = CatalogEventType.PRODUCT_LISTED,
            workspaceId = "ws-1",
            storefrontId = "sf-1",
            managementProductId = "PRD-1",
            publishedAt = now,
        )
        assertEquals(base, base.copy())
        val changed = base.copy(stockQuantity = 5)
        assertNotEquals(base, changed)
        assertEquals(5, changed.stockQuantity)
    }

    @Test
    fun `catalog event type enum has all five variants`() {
        assertEquals(5, CatalogEventType.entries.size)
        assertTrue(CatalogEventType.entries.contains(CatalogEventType.STOCK_UPDATED))
    }

    @Test
    fun `order placed event carries line items and address`() {
        val now = Instant.parse("2026-02-01T00:00:00Z")
        val line = EcomOrderLineItemPayload(
            listedProductId = "LP-1",
            managementProductId = "PRD-1",
            productName = "Widget",
            unitPrice = BigDecimal("10.00"),
            quantityOrdered = 2,
            lineTotal = BigDecimal("20.00"),
        )
        val event = EcomOrderPlacedEvent(
            ecomOrderRef = "ECO-1",
            workspaceId = "ws-1",
            storefrontId = "sf-1",
            customerId = "CUS-1",
            customerName = "Acme",
            customerEmail = "a@b.com",
            deliveryAddress = Address(city = "Pune"),
            lineItems = listOf(line),
            subtotal = BigDecimal("20.00"),
            totalAmount = BigDecimal("22.00"),
            placedAt = now,
        )

        assertEquals("ECO-1", event.ecomOrderRef)
        assertEquals(1, event.lineItems.size)
        assertEquals("Widget", event.lineItems.first().productName)
        assertEquals(BigDecimal("20.00"), event.lineItems.first().lineTotal)
        assertEquals("Pune", event.deliveryAddress.city)
        assertNull(event.customerPhone)
        assertEquals(event, event.copy())
    }

    @Test
    fun `order status event carries confirmed line items`() {
        val now = Instant.parse("2026-03-01T00:00:00Z")
        val confirmed = ConfirmedLineItemPayload(
            managementProductId = "PRD-1",
            quantityConfirmed = 3,
            status = "CONFIRMED",
        )
        val event = EcomOrderStatusEvent(
            ecomOrderRef = "ECO-1",
            workspaceId = "ws-1",
            newStatus = "CONFIRMED",
            managementOrderRef = "ORD-9",
            confirmedLineItems = listOf(confirmed),
            updatedAt = now,
        )

        assertEquals("CONFIRMED", event.newStatus)
        assertEquals("ORD-9", event.managementOrderRef)
        assertEquals(3, event.confirmedLineItems?.first()?.quantityConfirmed)
        assertEquals(now, event.updatedAt)
        assertEquals(event, event.copy())
    }
}
