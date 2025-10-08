package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TaxComponentType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "tax_rates",
    indexes = [
        Index(name = "idx_tax_rate_hsn", columnList = "hsn_code_id"),
        Index(name = "idx_tax_rate_component", columnList = "tax_component_type"),
        Index(name = "idx_tax_rate_business", columnList = "business_type"),
        Index(name = "idx_tax_rate_zone", columnList = "geographical_zone"),
        Index(name = "idx_tax_rate_effective", columnList = "effective_from, effective_to"),
        Index(name = "idx_tax_rate_active", columnList = "active"),
        Index(name = "idx_tax_rate_lookup", columnList = "hsn_code_id, business_type, effective_from, active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tax_rates",
            columnNames = ["hsn_code_id", "tax_component_type", "business_type", "geographical_zone", "effective_from"]
        )
    ]
)
class TaxRate : OwnableBaseDomain() {

    @Column(name = "hsn_code_id", nullable = false)
    var hsnCodeId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hsn_code_id", referencedColumnName = "id", insertable = false, updatable = false)
    var hsnCode: HsnCode? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_component_type", nullable = false, length = 30)
    var taxComponentType: TaxComponentType = TaxComponentType.IGST

    @Column(name = "rate_percentage", nullable = false, precision = 8, scale = 4)
    var ratePercentage: BigDecimal = BigDecimal.ZERO

    @Column(name = "fixed_amount_per_unit", precision = 12, scale = 4)
    var fixedAmountPerUnit: BigDecimal? = null

    @Column(name = "minimum_amount", precision = 12, scale = 4)
    var minimumAmount: BigDecimal? = null

    @Column(name = "maximum_amount", precision = 12, scale = 4)
    var maximumAmount: BigDecimal? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    var businessType: BusinessType = BusinessType.B2B

    @Enumerated(EnumType.STRING)
    @Column(name = "geographical_zone", length = 30)
    var geographicalZone: GeographicalZone? = null

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate = LocalDate.now()

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @Column(name = "version_number", nullable = false)
    var versionNumber: Int = 1

    @Column(name = "notification_number", length = 100)
    var notificationNumber: String? = null

    @Column(name = "notification_date")
    var notificationDate: LocalDate? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "JSON")
    var conditions: Map<String, Any> = emptyMap()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exemption_rules", columnDefinition = "JSON")
    var exemptionRules: Map<String, Any> = emptyMap()

    @Column(name = "is_reverse_charge_applicable", nullable = false)
    var isReverseChargeApplicable: Boolean = false

    @Column(name = "is_composition_scheme_applicable", nullable = false)
    var isCompositionSchemeApplicable: Boolean = true

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "source_reference", length = 255)
    var sourceReference: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_RATE_PREFIX
    }

    fun isValidForDate(date: LocalDateTime = LocalDateTime.now()): Boolean {
        val checkDate = date.toLocalDate()
        return (checkDate.isAfter(effectiveFrom) || checkDate.isEqual(effectiveFrom)) &&
                (effectiveTo == null || checkDate.isBefore(effectiveTo))
    }

    fun calculateTaxAmount(baseAmount: BigDecimal, quantity: Int = 1): BigDecimal {
        var taxAmount = baseAmount.multiply(ratePercentage).divide(BigDecimal(100), 4, BigDecimal.ROUND_HALF_UP)

        fixedAmountPerUnit?.let { fixedAmount ->
            taxAmount = taxAmount.add(fixedAmount.multiply(BigDecimal(quantity)))
        }

        minimumAmount?.let { minAmount ->
            if (taxAmount < minAmount) taxAmount = minAmount
        }

        maximumAmount?.let { maxAmount ->
            if (taxAmount > maxAmount) taxAmount = maxAmount
        }

        return taxAmount
    }

    fun isApplicableForBusinessType(businessType: BusinessType): Boolean {
        return this.businessType == businessType ||
                this.businessType == BusinessType.B2B && businessType == BusinessType.B2C
    }

    fun isApplicableForZone(stateCode: String?): Boolean {
        if (geographicalZone == null || geographicalZone == GeographicalZone.ALL_INDIA) return true
        if (stateCode == null) return false

        return geographicalZone?.stateCodes?.contains(stateCode.uppercase()) == true
    }

    fun hasExemptionFor(exemptionType: String): Boolean {
        return exemptionRules.containsKey(exemptionType) &&
                exemptionRules[exemptionType] as? Boolean == true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaxRate) return false

        return hsnCodeId == other.hsnCodeId &&
                taxComponentType == other.taxComponentType &&
                businessType == other.businessType &&
                geographicalZone == other.geographicalZone &&
                effectiveFrom == other.effectiveFrom
    }

    override fun hashCode(): Int {
        var result = hsnCodeId.hashCode()
        result = 31 * result + taxComponentType.hashCode()
        result = 31 * result + businessType.hashCode()
        result = 31 * result + (geographicalZone?.hashCode() ?: 0)
        result = 31 * result + effectiveFrom.hashCode()
        return result
    }

    override fun toString(): String {
        return "TaxRate(hsnCodeId=$hsnCodeId, taxComponentType=$taxComponentType, ratePercentage=$ratePercentage, businessType=$businessType)"
    }
}