package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*

@Entity
@Table(
    name = "master_tax_component",
    indexes = [
        Index(name = "idx_master_tax_comp_type", columnList = "component_type_id"),
        Index(name = "idx_master_tax_comp_rate", columnList = "rate_percentage")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_master_tax_component",
            columnNames = ["component_type_id", "rate_percentage"]
        )
    ]
)
class MasterTaxComponent : BaseDomain() {

    @Column(name = "component_type_id", nullable = false, length = 255)
    var componentTypeId: String = ""

    @Column(name = "component_name", nullable = false, length = 100)
    var componentName: String = "" // CGST, SGST, IGST, etc.

    @Column(name = "component_display_name", length = 200)
    var componentDisplayName: String = ""

    @Column(name = "tax_type", nullable = false, length = 50)
    var taxType: String = "" // GST, VAT, SALES_TAX

    @Column(name = "jurisdiction", nullable = false, length = 100)
    var jurisdiction: String = ""

    @Column(name = "jurisdiction_level", nullable = false, length = 50)
    var jurisdictionLevel: String = "" // COUNTRY, STATE, COUNTY, CITY

    @Column(name = "rate_percentage", nullable = false)
    var ratePercentage: Double = 0.0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.MASTER_TAX_COMPONENT_PREFIX
    }

    override fun toString(): String {
        return "MasterTaxComponent(componentName='$componentName', jurisdiction='$jurisdiction', rate=$ratePercentage)"
    }
}
