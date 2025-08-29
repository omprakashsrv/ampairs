package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity(name = "unit")
@Table(
    indexes = [
        Index(name = "unit_idx", columnList = "name"),
        Index(name = "idx_unit_uid", columnList = "uid", unique = true)
    ]
)
class Unit : OwnableBaseDomain() {

    @Column(name = "name", length = 10)
    var name: String = ""

    @Column(name = "short_name", length = 10)
    var shortName: String = ""

    @Column(name = "decimal_places")
    var decimalPlaces: Int = 2

    override fun obtainSeqIdPrefix(): String {
        return Constants.UNIT_PREFIX
    }
}