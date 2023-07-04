package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity(name = "unit")
@Table(
    indexes = arrayOf(
        Index(
            name = "unit_idx",
            columnList = "name"
        )
    )
)
class Unit : OwnableBaseDomain() {

    @Column(name = "name", length = 10)
    var name: String = ""

    @Column(name = "short_name", length = 10)
    var shortName: String = ""

    @Column(name = "decimal_places")
    var decimalPlaces: Int = 2

    override fun obtainIdPrefix(): String {
        return Constants.UNIT_PREFIX
    }
}