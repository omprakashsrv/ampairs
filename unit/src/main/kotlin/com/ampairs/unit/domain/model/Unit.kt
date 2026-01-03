package com.ampairs.unit.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.unit.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table

@Entity(name = "unit")
@NamedEntityGraph(
    name = "Unit.basic"
)
@Table(
    indexes = [
        Index(name = "unit_idx", columnList = "name"),
        Index(name = "idx_unit_uid", columnList = "uid", unique = true)
    ]
)
class Unit : OwnableBaseDomain() {

    @Column(name = "name", length = 100, nullable = false)
    var name: String = ""

    @Column(name = "short_name", length = 20, nullable = false)
    var shortName: String = ""

    @Column(name = "decimal_places", nullable = false)
    var decimalPlaces: Int = 2

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "category", length = 50)
    var category: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.UNIT_PREFIX
    }
}
