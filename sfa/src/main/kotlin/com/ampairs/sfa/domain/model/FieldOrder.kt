package com.ampairs.sfa.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.sfa.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * A counter order taken during a [Visit]. The actual order is authored through the `order` module's
 * own `/sync` (no parallel order entity here); this row only references the resulting `orderUid`
 * as a secondary-sales pointer the DMS layer later attributes.
 */
@Entity
@Table(
    name = "field_orders",
    indexes = [
        Index(name = "idx_field_order_owner", columnList = "owner_id"),
        Index(name = "idx_field_order_visit", columnList = "visit_uid"),
        Index(name = "idx_field_order_order", columnList = "order_uid"),
        Index(name = "idx_field_order_updated_at", columnList = "updated_at"),
    ],
)
class FieldOrder : OwnableBaseDomain() {

    @Column(name = "visit_uid", length = 40)
    var visitUid: String? = null

    @Column(name = "customer_uid", nullable = false, length = 40)
    var customerUid: String = ""

    @Column(name = "rep_member_uid", nullable = false, length = 40)
    var repMemberUid: String = ""

    /** The `order` module order this counter order produced. */
    @Column(name = "order_uid", length = 40)
    var orderUid: String? = null

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.FIELD_ORDER_PREFIX
}
