package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "tax_rules",
    indexes = [
        Index(name = "idx_tax_rule", columnList = "owner_id"),
        Index(name = "idx_tax_rule_tax_code", columnList = "tax_code_id"),
        Index(name = "idx_tax_rule_tax_code", columnList = "tax_code"),
        Index(name = "idx_tax_rule_country", columnList = "country_code"),
        Index(name = "idx_tax_rule_jurisdiction", columnList = "jurisdiction"),
        Index(name = "idx_tax_rule_updated", columnList = "updated_at"),
        Index(name = "idx_tax_rule_active", columnList = "is_active")
    ]
)
class TaxRule : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    @Column(name = "tax_code_id", nullable = false, length = 255)
    var taxCodeId: String = ""

    @Column(name = "tax_code", nullable = false, length = 100)
    var taxCode: String = ""

    @Column(name = "tax_code_type", nullable = false, length = 50)
    var taxCodeType: String = ""

    @Column(name = "tax_code_description", columnDefinition = "TEXT")
    var taxCodeDescription: String? = null

    @Column(name = "jurisdiction", nullable = false, length = 100)
    var jurisdiction: String = ""

    @Column(name = "jurisdiction_level", nullable = false, length = 50)
    var jurisdictionLevel: String = "" // COUNTRY, STATE, COUNTY, CITY

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "component_composition", nullable = false, columnDefinition = "jsonb")
    var componentComposition: Map<String, ComponentComposition> = emptyMap()

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_RULE_PREFIX
    }

    override fun toString(): String {
        return "TaxRuleV2(ownerId='$ownerId', taxCode='$taxCode', jurisdiction='$jurisdiction')"
    }
}

data class ComponentComposition(
    val scenario: String,
    val components: List<ComponentReference>,
    val totalRate: Double
)

data class ComponentReference(
    val id: String,
    val name: String,
    val rate: Double,
    val order: Int
)
