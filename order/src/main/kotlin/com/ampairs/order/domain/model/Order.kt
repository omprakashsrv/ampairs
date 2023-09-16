package com.ampairs.order.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.order.config.Constants
import com.ampairs.order.domain.enums.OrderStatus
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.util.*


@Entity(name = "customer_order")
@Table(indexes = arrayOf(Index(name = "order_ref_idx", columnList = "ref_id", unique = true)))
class Order : OwnableBaseDomain() {

    @Column(name = "order_number", nullable = false, length = 255)
    var orderNumber: String = ""

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "order_date", nullable = false)
    var orderDate: Date = Date()

    @Column(name = "from_customer_id", nullable = false, length = 255)
    var fromCustomerId: String = ""

    @Column(name = "from_customer_name", nullable = false, length = 255)
    var fromCustomerName: String = ""

    @Column(name = "to_customer_id", nullable = false, length = 255)
    var toCustomerId: String = ""

    @Column(name = "to_customer_name", nullable = false, length = 255)
    var toCustomerName: String = ""

    @Column(name = "from_customer_gst", nullable = false, length = 30)
    var fromCustomerGst: String = ""

    @Column(name = "to_customer_gst", nullable = false, length = 30)
    var toCustomerGst: String = ""

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.DRAFT

    @Column(name = "total_items", nullable = false)
    var totalItems: Int = 0

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Double = 0.0

    @Type(JsonType::class)
    @Column(name = "billing_address", nullable = false, columnDefinition = "json")
    var billingAddress: Address = Address()

    @Type(JsonType::class)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "json")
    var shippingAddress: Address = Address()


    override fun obtainIdPrefix(): String {
        return Constants.ORDER_PREFIX
    }
}