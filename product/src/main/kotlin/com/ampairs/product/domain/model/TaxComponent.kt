package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Tax component entity representing individual tax elements (CGST, SGST, IGST, etc.)
 * that make up a complete tax calculation for GST compliance
 */
@Entity(name = "tax_component")
@Table(
    indexes = [
        Index(name = "idx_tax_component_code", columnList = "component_code"),
        Index(name = "idx_tax_component_type", columnList = "component_type"),
        Index(name = "idx_tax_component_active", columnList = "is_active")
    ]
)
class TaxComponent : OwnableBaseDomain() {

    @Column(name = "component_code", nullable = false, length = 10, unique = true)
    var componentCode: String = ""

    @Column(name = "component_name", nullable = false, length = 100)
    var componentName: String = ""

    @Column(name = "component_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var componentType: TaxComponentType = TaxComponentType.IGST

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "default_rate", nullable = false)
    var defaultRate: Double = 0.0

    @Column(name = "is_percentage", nullable = false)
    var isPercentage: Boolean = true

    @Column(name = "is_compound", nullable = false)
    var isCompound: Boolean = false

    @Column(name = "calculation_order", nullable = false)
    var calculationOrder: Int = 1

    @Column(name = "applicable_from")
    var applicableFrom: LocalDateTime? = null

    @Column(name = "applicable_to")
    var applicableTo: LocalDateTime? = null


    @Column(name = "state_codes", length = 500)
    var stateCodes: String? = null // Comma-separated state codes where applicable

    @Column(name = "business_types", length = 500)
    var businessTypes: String? = null // Comma-separated business types

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_COMPONENT_PREFIX
    }

    /**
     * Calculate tax amount based on base amount
     */
    fun calculateAmount(baseAmount: Double): Double {
        return if (isPercentage) {
            baseAmount * defaultRate / 100
        } else {
            defaultRate // Fixed amount
        }
    }

    /**
     * Check if component is applicable for given state
     */
    fun isApplicableForState(stateCode: String): Boolean {
        return stateCodes?.split(",")?.map { it.trim() }?.contains(stateCode) ?: true
    }

    /**
     * Check if component is applicable for given business type
     */
    fun isApplicableForBusinessType(businessType: String): Boolean {
        return businessTypes?.split(",")?.map { it.trim() }?.contains(businessType) ?: true
    }

    /**
     * Check if component is currently active and valid
     */
    fun isCurrentlyValid(): Boolean {
        val now = LocalDateTime.now()
        return active &&
               (applicableFrom == null || now.isAfter(applicableFrom) || now.isEqual(applicableFrom)) &&
               (applicableTo == null || now.isBefore(applicableTo))
    }

    companion object {
        /**
         * Create standard GST components
         */
        fun createStandardGSTComponents(): List<TaxComponent> {
            return listOf(
                TaxComponent().apply {
                    componentCode = "CGST"
                    componentName = "Central Goods and Services Tax"
                    componentType = TaxComponentType.CGST
                    description = "Central GST component for intrastate transactions"
                    defaultRate = 9.0 // This will be overridden by tax codes
                    calculationOrder = 1
                    active = true
                },
                TaxComponent().apply {
                    componentCode = "SGST"
                    componentName = "State Goods and Services Tax"
                    componentType = TaxComponentType.SGST
                    description = "State GST component for intrastate transactions"
                    defaultRate = 9.0 // This will be overridden by tax codes
                    calculationOrder = 2
                    active = true
                },
                TaxComponent().apply {
                    componentCode = "IGST"
                    componentName = "Integrated Goods and Services Tax"
                    componentType = TaxComponentType.IGST
                    description = "Integrated GST for interstate transactions"
                    defaultRate = 18.0 // This will be overridden by tax codes
                    calculationOrder = 1
                    active = true
                },
                TaxComponent().apply {
                    componentCode = "UTGST"
                    componentName = "Union Territory Goods and Services Tax"
                    componentType = TaxComponentType.UTGST
                    description = "UT GST component for Union Territory transactions"
                    defaultRate = 9.0
                    calculationOrder = 2
                    stateCodes = "AN,CH,DN,DD,DL,JK,LA,LD,PY" // Union Territory codes
                    active = true
                },
                TaxComponent().apply {
                    componentCode = "CESS"
                    componentName = "Cess"
                    componentType = TaxComponentType.CESS
                    description = "Additional cess on specific goods"
                    defaultRate = 0.0
                    calculationOrder = 3
                    active = true
                }
            )
        }
    }
}