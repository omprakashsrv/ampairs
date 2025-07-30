package com.ampairs.invoice.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.invoice.config.Constants
import com.ampairs.invoice.domain.dto.Discount
import com.ampairs.invoice.domain.dto.TaxInfo
import com.ampairs.invoice.domain.enums.InvoiceStatus
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.util.*


@Entity(name = "invoice")
@Table(indexes = arrayOf(Index(name = "invoice_ref_idx", columnList = "ref_id", unique = true)))
class Invoice : OwnableBaseDomain() {

    @Column(name = "invoice_number", nullable = false, length = 255)
    var invoiceNumber: String = ""

    @Column(name = "order_ref_id", nullable = true, length = 255)
    var orderRefId: String? = null

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invoice_date", nullable = false)
    var invoiceDate: Date = Date()

    @Column(name = "from_customer_id", nullable = false, length = 255)
    var fromCustomerId: String = ""

    @Column(name = "from_customer_name", nullable = false, length = 255)
    var fromCustomerName: String = ""

    @Column(name = "to_customer_id", nullable = false, length = 255)
    var toCustomerId: String = ""

    @Column(name = "to_customer_name", nullable = false, length = 255)
    var toCustomerName: String = ""

    @Column(name = "place_of_supply", nullable = false, length = 255)
    var placeOfSupply: String = ""

    @Column(name = "from_customer_gst", nullable = false, length = 30)
    var fromCustomerGst: String = ""

    @Column(name = "to_customer_gst", nullable = false, length = 30)
    var toCustomerGst: String = ""

    @Column(name = "total_cost", nullable = false)
    var totalCost: Double = 0.0

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "total_tax", nullable = false)
    var totalTax: Double = 0.0

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: InvoiceStatus = InvoiceStatus.DRAFT

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

    @Type(JsonType::class)
    @Column(name = "discount", nullable = true, columnDefinition = "json")
    var discount: List<Discount>? = null

    @Type(JsonType::class)
    @Column(name = "tax_info", length = 255, columnDefinition = "json")
    var taxInfos: List<TaxInfo> = listOf()

    @OneToMany()
    @JoinColumn(
        name = "invoice_id", referencedColumnName = "id", insertable = false, updatable = false, nullable = false
    )
    var invoiceItems: MutableList<InvoiceItem> = mutableListOf()


    override fun obtainSeqIdPrefix(): String {
        return Constants.INVOICE_PREFIX
    }
}