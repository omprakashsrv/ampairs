package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import com.ampairs.unit.domain.model.Unit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity(name = "inventory_unit_conversion")
class InventoryUnitConversion : OwnableBaseDomain() {

    @Column(name = "base_unit_id", length = 200)
    var baseUnitId: String = ""

    @Column(name = "derived_unit_id", length = 200)
    var derivedUnitId: String = ""

    @Column(name = "inventory_id", length = 200)
    var inventoryId: String = ""

    @Column(name = "multiplier")
    var multiplier: Double = 1.0

    @OneToOne()
    @JoinColumn(name = "base_unit_id", referencedColumnName = "uid", updatable = false, insertable = false)
    lateinit var baseUnit: Unit

    @OneToOne()
    @JoinColumn(name = "derived_unit_id", referencedColumnName = "uid", updatable = false, insertable = false)
    lateinit var derivedUnit: Unit

    @OneToOne()
    @JoinColumn(name = "inventory_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var inventory: Inventory? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.UNIT_PREFIX
    }

}
