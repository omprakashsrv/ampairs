package com.ampairs.unit.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.unit.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity(name = "unit_conversion")
@Table(
    indexes = [
        Index(name = "idx_unit_conversion_base", columnList = "base_unit_id"),
        Index(name = "idx_unit_conversion_derived", columnList = "derived_unit_id"),
        Index(name = "idx_unit_conversion_entity", columnList = "entity_id")
    ]
)
@NamedEntityGraph(
    name = "UnitConversion.withUnits",
    attributeNodes = [
        NamedAttributeNode("baseUnit"),
        NamedAttributeNode("derivedUnit")
    ]
)
class UnitConversion : OwnableBaseDomain() {

    @Column(name = "base_unit_id", length = 200, nullable = false)
    var baseUnitId: String = ""

    @Column(name = "derived_unit_id", length = 200, nullable = false)
    var derivedUnitId: String = ""

    @Column(name = "entity_id", length = 200)
    var entityId: String? = null

    @Column(name = "multiplier", precision = 20, scale = 6, nullable = false)
    var multiplier: BigDecimal = BigDecimal.ONE

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var baseUnit: Unit? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derived_unit_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var derivedUnit: Unit? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.UNIT_CONVERSION_PREFIX
    }
}
