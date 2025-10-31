package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.domain.enums.BusinessType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "business_types",
    indexes = [
        Index(name = "idx_business_type_code", columnList = "business_type", unique = true),
        Index(name = "idx_business_type_active", columnList = "active")
    ]
)
class BusinessTypeEntity : OwnableBaseDomain() {

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, unique = true, length = 30)
    var businessType: BusinessType = BusinessType.B2B

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "display_name", nullable = false, length = 100)
    var displayName: String = ""

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "default_gst_rate", precision = 8, scale = 4)
    var defaultGstRate: java.math.BigDecimal? = null

    @Column(name = "composition_scheme_rate", precision = 8, scale = 4)
    var compositionSchemeRate: java.math.BigDecimal? = null

    @Column(name = "turnover_threshold", precision = 15, scale = 2)
    var turnoverThreshold: java.math.BigDecimal? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_rules")
    var specialRules: Map<String, Any> = emptyMap()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "compliance_requirements")
    var complianceRequirements: Map<String, Any> = emptyMap()

    @OneToMany(mappedBy = "businessTypeEntity", fetch = FetchType.LAZY)
    var taxConfigurations: MutableList<TaxConfiguration> = mutableListOf()

    override fun obtainSeqIdPrefix(): String {
        return "BT"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BusinessTypeEntity) return false
        return businessType == other.businessType
    }

    override fun hashCode(): Int {
        return businessType.hashCode()
    }

    override fun toString(): String {
        return "BusinessTypeEntity(businessType=$businessType, displayName='$displayName')"
    }
}