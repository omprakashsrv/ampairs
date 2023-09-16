package com.ampairs.order.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.order.config.Constants
import com.ampairs.order.domain.enums.ItemStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "order_item")
class OrderItem : BaseDomain() {

    @Column(name = "order_id", nullable = false, length = 255)
    var orderId: String = ""

    @Column(name = "description", nullable = false, length = 255)
    var description: String = ""

    @Column(name = "product_id", nullable = false, length = 255)
    var productId: String = ""

    @Column(name = "status", nullable = false, length = 10)
    var status: ItemStatus = ItemStatus.ACTIVE

    @Column(name = "qunatity", nullable = false)
    var qunatity: Double = 0.0

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "dp", nullable = false)
    var dp: Double = 0.0

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "total_items", nullable = false)
    var totalItems: Int = 0

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Double = 0.0

    override fun obtainIdPrefix(): String {
        return Constants.ORDER_ITEM_PREFIX
    }
}