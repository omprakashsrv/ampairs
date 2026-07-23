package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "tax_component",
    indexes = [
        Index(name = "idx_tax_comp", columnList = "owner_id"),
        Index(name = "idx_tax_comp_type", columnList = "component_type_id"),
        Index(name = "idx_tax_comp_jurisdiction", columnList = "jurisdiction"),
        Index(name = "idx_tax_comp_updated", columnList = "updated_at"),
        Index(name = "idx_tax_comp_active", columnList = "is_active")
    ],
    // uid is unique PER WORKSPACE, not globally: workspace tax components deliberately reuse the
    // master component uid (e.g. COMP_CGST_9) across tenants. Overriding the inherited global
    // unique keeps Hibernate's schema view aligned with migration V1.0.122.
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tax_component_owner_uid", columnNames = ["owner_id", "uid"])
    ]
)
@AttributeOverride(
    name = "uid",
    column = Column(name = "uid", length = 200, updatable = false, nullable = false)
)
class TaxComponent : OwnableBaseDomain() {

    @Column(name = "component_type_id", nullable = false, length = 255)
    var componentTypeId: String = ""

    @Column(name = "component_name", nullable = false, length = 100)
    var componentName: String = "" // CGST, SGST, IGST, VAT, etc.

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

    @Column(name = "is_compound", nullable = false)
    var isCompound: Boolean = false

    @Column(name = "calculation_method", nullable = false, length = 50)
    var calculationMethod: String = "PERCENTAGE" // PERCENTAGE, FLAT

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_COMPONENT_PREFIX
    }

    override fun toString(): String {
        return "TaxComponent(ownerId='$ownerId', componentName='$componentName', jurisdiction='$jurisdiction', rate=$ratePercentage)"
    }
}
