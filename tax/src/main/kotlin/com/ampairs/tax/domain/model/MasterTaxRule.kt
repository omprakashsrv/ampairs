package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "master_tax_rule",
    indexes = [
        Index(name = "idx_master_tax_rule_code", columnList = "master_tax_code_id"),
        Index(name = "idx_master_tax_rule_rate", columnList = "tax_rate"),
        Index(name = "idx_master_tax_rule_country", columnList = "country_code")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_master_tax_rule",
            columnNames = ["master_tax_code_id", "jurisdiction", "jurisdiction_level"]
        )
    ]
)
class MasterTaxRule : BaseDomain() {

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    @Column(name = "master_tax_code_id", nullable = false, length = 255)
    var masterTaxCodeId: String = ""

    @Column(name = "tax_code", nullable = false, length = 100)
    var taxCode: String = ""

    @Column(name = "tax_code_type", nullable = false, length = 50)
    var taxCodeType: String = ""

    @Column(name = "tax_rate", nullable = false)
    var taxRate: Double = 0.0

    @Column(name = "jurisdiction", nullable = false, length = 100)
    var jurisdiction: String = ""

    @Column(name = "jurisdiction_level", nullable = false, length = 50)
    var jurisdictionLevel: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "component_composition", nullable = false, columnDefinition = "jsonb")
    var componentComposition: Map<String, Any> = emptyMap()

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.MASTER_TAX_RULE_PREFIX
    }

    override fun toString(): String {
        return "MasterTaxRule(masterTaxCodeId='$masterTaxCodeId', taxCode='$taxCode', taxRate=$taxRate)"
    }
}
