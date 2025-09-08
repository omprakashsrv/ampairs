package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.enums.TaxSpec
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.sql.Timestamp
import java.time.LocalDateTime

@Entity(name = "tax_code")
@Table(
    indexes = arrayOf(
        Index(
            name = "tax_code_idx",
            columnList = "code"
        )
    )
)
class TaxCode : OwnableBaseDomain() {

    @Column(name = "code", length = 20)
    var code: String = ""

    @Column(name = "effective_from")
    var effectiveFrom: Timestamp? = null

    @Column(name = "type", length = 10)
    @Enumerated(EnumType.STRING)
    var type: TaxType = TaxType.HSN

    @Column(name = "description", length = 255)
    var description: String = ""

    @Type(JsonType::class)
    @Column(name = "tax_info", length = 255, nullable = false, columnDefinition = "json")
    var taxInfos: List<TaxInfoModel> = listOf()

    @Column(name = "gst_rate", nullable = false)
    var gstRate: Double = 0.0

    @Column(name = "is_reverse_charge", nullable = false)
    var isReverseCharge: Boolean = false

    @Column(name = "is_composition_applicable", nullable = false)
    var isCompositionApplicable: Boolean = true

    @Column(name = "cess_rate", nullable = false)
    var cessRate: Double = 0.0

    @Column(name = "category", length = 100)
    var category: String? = null

    @Column(name = "business_type_rates", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var businessTypeRates: Map<String, Double> = emptyMap()

    @Column(name = "valid_from")
    var validFrom: LocalDateTime? = null

    @Column(name = "valid_to") 
    var validTo: LocalDateTime? = null


    override fun obtainSeqIdPrefix(): String {
        return Constants.HSN_CODE_PREFIX
    }

    /**
     * Calculate tax components based on amount and transaction type
     */
    fun calculateTaxComponents(
        baseAmount: Double,
        taxSpec: TaxSpec,
        buyerStateCode: String? = null,
        sellerStateCode: String? = null
    ): List<TaxInfoModel> {
        val components = mutableListOf<TaxInfoModel>()
        
        if (gstRate == 0.0) return components
        
        when (taxSpec) {
            TaxSpec.INTER -> {
                // Interstate transaction - IGST
                components.add(
                    TaxInfoModel(
                        refId = null,
                        name = "IGST",
                        percentage = gstRate,
                        componentType = TaxComponentType.IGST,
                        baseAmount = baseAmount,
                        calculatedAmount = baseAmount * gstRate / 100,
                        taxSpec = taxSpec
                    )
                )
            }
            TaxSpec.INTRA -> {
                // Intrastate transaction - CGST + SGST/UTGST
                val halfRate = gstRate / 2
                components.add(
                    TaxInfoModel(
                        refId = null,
                        name = "CGST",
                        percentage = halfRate,
                        componentType = TaxComponentType.CGST,
                        baseAmount = baseAmount,
                        calculatedAmount = baseAmount * halfRate / 100,
                        taxSpec = taxSpec
                    )
                )
                
                // Check if UT (Union Territory)
                val isUT = buyerStateCode in listOf("AN", "CH", "DN", "DD", "DL", "JK", "LA", "LD", "PY")
                val stateComponent = if (isUT) TaxComponentType.UTGST else TaxComponentType.SGST
                val stateName = if (isUT) "UTGST" else "SGST"
                
                components.add(
                    TaxInfoModel(
                        refId = null,
                        name = stateName,
                        percentage = halfRate,
                        componentType = stateComponent,
                        baseAmount = baseAmount,
                        calculatedAmount = baseAmount * halfRate / 100,
                        taxSpec = taxSpec
                    )
                )
            }
            TaxSpec.COMPOSITION -> {
                // Composition scheme - simplified tax
                val compositionRate = businessTypeRates["COMPOSITION"] ?: (gstRate * 0.6) // Typically 60% of normal rate
                components.add(
                    TaxInfoModel(
                        refId = null,
                        name = "GST (Composition)",
                        percentage = compositionRate,
                        componentType = TaxComponentType.IGST,
                        baseAmount = baseAmount,
                        calculatedAmount = baseAmount * compositionRate / 100,
                        taxSpec = taxSpec
                    )
                )
            }
            TaxSpec.EXPORT, TaxSpec.EXEMPT, TaxSpec.NIL -> {
                // No tax applicable
            }
        }
        
        // Add cess if applicable
        if (cessRate > 0) {
            components.add(
                TaxInfoModel(
                    refId = null,
                    name = "Cess",
                    percentage = cessRate,
                    componentType = TaxComponentType.CESS,
                    baseAmount = baseAmount,
                    calculatedAmount = baseAmount * cessRate / 100,
                    taxSpec = taxSpec
                )
            )
        }
        
        return components
    }

    /**
     * Get total tax percentage for display
     */
    fun getTotalTaxRate(): Double {
        return gstRate + cessRate
    }

    /**
     * Check if tax code is valid for given date
     */
    fun isValidForDate(date: LocalDateTime): Boolean {
        val now = date
        return active && 
               (validFrom == null || now.isAfter(validFrom) || now.isEqual(validFrom)) &&
               (validTo == null || now.isBefore(validTo))
    }

    /**
     * Get business type specific rate
     */
    fun getRateForBusinessType(businessType: String): Double {
        return businessTypeRates[businessType] ?: gstRate
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TaxCode

        return code == other.code
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + code.hashCode()
        return result
    }


}