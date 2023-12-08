package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import com.ampairs.product.domain.model.Unit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "inventory_transaction")
class InventoryTransaction : OwnableBaseDomain() {

    @Column(name = "description", nullable = false, length = 255)
    var description: String = ""

    @Column(name = "product_id", nullable = false, length = 255)
    var productId: String = ""

    @Column(name = "stock", nullable = false)
    var stock: Double = 0.0

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "dp", nullable = false)
    var dp: Double = 0.0

    @Column(name = "unit_id", length = 200)
    var unitId: String? = null

    @OneToOne()
    @JoinColumn(name = "unit_id", referencedColumnName = "id", updatable = false, insertable = false)
    var unit: Unit? = null

    override fun obtainIdPrefix(): String {
        return Constants.INVENTORY_ITEM_PREFIX
    }
}