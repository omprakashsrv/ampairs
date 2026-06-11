package com.ampairs.order.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pure-arithmetic tests for the order/line-item money math. These ran with no coverage before;
 * the totals feed invoicing, so a silent regression here is a billing bug.
 *
 * Previously `Order.calculateTotals()` subtracted each item's discount TWICE — once inside
 * `OrderItem.calculateLineTotal()` (which nets the discount into `lineTotal`) and again via an
 * `itemDiscounts` term — so a $100 item with a $10 line discount produced 80 instead of 90. That
 * double-count was removed; `applies each line discount exactly once` locks in the fix.
 */
class OrderCalculationTest {

    private fun item(
        qty: Double,
        unitPrice: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
    ) = OrderItem().apply {
        quantity = qty
        this.unitPrice = unitPrice
        discountAmount = discount
        totalTax = tax
    }

    @Test
    fun `calculateLineTotal is quantity times price minus item discount`() {
        val i = item(qty = 2.0, unitPrice = 100.0, discount = 20.0)
        i.calculateLineTotal()
        assertEquals(180.0, i.lineTotal, 1e-9)
    }

    @Test
    fun `calculateLineTotalWithTax adds tax on top of the net line total`() {
        val i = item(qty = 2.0, unitPrice = 100.0, discount = 20.0, tax = 18.0)
        i.calculateLineTotalWithTax()
        assertEquals(198.0, i.lineTotal, 1e-9)
    }

    @Test
    fun `getEffectiveUnitPrice divides line total by quantity, falling back to unit price`() {
        val i = item(qty = 2.0, unitPrice = 100.0, discount = 20.0)
        i.calculateLineTotal() // lineTotal = 180
        assertEquals(90.0, i.getEffectiveUnitPrice(), 1e-9)

        val zeroQty = item(qty = 0.0, unitPrice = 55.0)
        assertEquals(55.0, zeroQty.getEffectiveUnitPrice(), 1e-9)
    }

    @Test
    fun `order totals aggregate subtotal, tax and total across items`() {
        val order = Order()
        val a = item(qty = 1.0, unitPrice = 100.0, tax = 18.0).also { it.calculateLineTotal() }
        val b = item(qty = 2.0, unitPrice = 50.0, tax = 18.0).also { it.calculateLineTotal() }
        order.addItem(a) // addItem() recomputes totals
        order.addItem(b)

        assertEquals(2, order.totalItems)
        assertEquals(3.0, order.totalQuantity, 1e-9)
        assertEquals(200.0, order.subtotal, 1e-9)   // 100 + 100
        assertEquals(36.0, order.taxAmount, 1e-9)   // 18 + 18
        assertEquals(236.0, order.totalAmount, 1e-9) // no discounts: subtotal + tax
    }

    @Test
    fun `order-level discount reduces the total`() {
        val order = Order().apply { discountAmount = 30.0 }
        val a = item(qty = 1.0, unitPrice = 100.0, tax = 0.0).also { it.calculateLineTotal() }
        order.addItem(a)
        // subtotal 100 - orderDiscount 30 + tax 0
        assertEquals(70.0, order.totalAmount, 1e-9)
    }

    @Test
    fun `applies each line discount exactly once`() {
        val order = Order()
        // A single $100 item with a $10 line discount and no order-level discount or tax.
        val i = item(qty = 1.0, unitPrice = 100.0, discount = 10.0).also { it.calculateLineTotal() }
        assertEquals(90.0, i.lineTotal, 1e-9) // discount already netted into the line total

        order.addItem(i)
        // calculateTotals(): subtotal(90, already net) - orderDiscount 0 + tax 0 = 90.
        // The $10 line discount is reflected once (in lineTotal), not subtracted a second time.
        assertEquals(90.0, order.totalAmount, 1e-9)
    }

    @Test
    fun `line and order-level discounts both apply, each once`() {
        val order = Order().apply { discountAmount = 15.0 } // order-level discount on top
        val i = item(qty = 1.0, unitPrice = 100.0, discount = 10.0).also { it.calculateLineTotal() }
        order.addItem(i)
        // subtotal 90 (net of the 10 line discount) - 15 order discount + 0 tax = 75
        assertEquals(75.0, order.totalAmount, 1e-9)
    }
}
