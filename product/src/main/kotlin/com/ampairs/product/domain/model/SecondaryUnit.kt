package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "product_unit")
class SecondaryUnit : OwnableBaseDomain() {

    @Column(name = "base_unit_id", length = 200)
    var baseUnitId: String = ""

    @Column(name = "product_id", length = 200)
    var productId: String = ""

    @Column(name = "multiplier")
    var multiplier: Double = 1.0

    @OneToOne()
    @JoinColumn(name = "base_unit_id", referencedColumnName = "id", updatable = false, insertable = false)
    var unit: Unit? = null

    @OneToOne()
    @JoinColumn(name = "product_id", referencedColumnName = "id", updatable = false, insertable = false)
    var product: Product? = null

    override fun obtainIdPrefix(): String? {
        return Constants.UNIT_PREFIX
    }

}