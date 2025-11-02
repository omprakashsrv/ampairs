package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TransactionType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "tax_configurations",
    indexes = [
        Index(name = "idx_tax_config_business", columnList = "business_type_id"),
        Index(name = "idx_tax_config_hsn", columnList = "hsn_code_id"),
        Index(name = "idx_tax_config_zone", columnList = "geographical_zone"),
        Index(name = "idx_tax_config_effective", columnList = "effective_from, effective_to"),
        Index(name = "idx_tax_config_active", columnList = "active"),
        Index(name = "idx_tax_config_lookup", columnList = "business_type_id, hsn_code_id, effective_from, active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tax_configurations",
            columnNames = ["business_type_id", "hsn_code_id", "geographical_zone", "effective_from"]
        )
    ]
)
class TaxConfiguration : OwnableBaseDomain() {

    @Column(name = "business_type_id", nullable = false)
    var businessTypeId: Long = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_type_id", referencedColumnName = "id", insertable = false, updatable = false)
    var businessTypeEntity: BusinessTypeEntity? = null

    @Column(name = "hsn_code_id", nullable = false)
    var hsnCodeId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hsn_code_id", referencedColumnName = "id", insertable = false, updatable = false)
    var hsnCode: HsnCode? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "geographical_zone", length = 30)
    var geographicalZone: GeographicalZone? = null

    @Column(name = "total_gst_rate", nullable = false, precision = 8, scale = 4)
    var totalGstRate: BigDecimal = BigDecimal.ZERO

    @Column(name = "cgst_rate", precision = 8, scale = 4)
    var cgstRate: BigDecimal? = null

    @Column(name = "sgst_rate", precision = 8, scale = 4)
    var sgstRate: BigDecimal? = null

    @Column(name = "igst_rate", precision = 8, scale = 4)
    var igstRate: BigDecimal? = null

    @Column(name = "utgst_rate", precision = 8, scale = 4)
    var utgstRate: BigDecimal? = null

    @Column(name = "cess_rate", precision = 8, scale = 4)
    var cessRate: BigDecimal? = null

    @Column(name = "cess_amount_per_unit", precision = 12, scale = 4)
    var cessAmountPerUnit: BigDecimal? = null

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate = LocalDate.now()

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null

    @Column(name = "is_reverse_charge_applicable", nullable = false)
    var isReverseChargeApplicable: Boolean = false

    @Column(name = "is_composition_scheme_applicable", nullable = false)
    var isCompositionSchemeApplicable: Boolean = true

    @Column(name = "composition_rate", precision = 8, scale = 4)
    var compositionRate: BigDecimal? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_conditions")
    var specialConditions: Map<String, Any> = emptyMap()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exemption_criteria")
    var exemptionCriteria: Map<String, Any> = emptyMap()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "threshold_limits")
    var thresholdLimits: Map<String, Any> = emptyMap()

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "notification_reference", length = 255)
    var notificationReference: String? = null

    @Column(name = "last_updated_by", length = 200)
    var lastUpdatedBy: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_CONFIG_PREFIX
    }

    fun isValidForDate(date: LocalDateTime = LocalDateTime.now()): Boolean {
        val checkDate = date.toLocalDate()
        return (checkDate.isAfter(effectiveFrom) || checkDate.isEqual(effectiveFrom)) &&
                (effectiveTo == null || checkDate.isBefore(effectiveTo))
    }

    fun getTaxRateForTransaction(transactionType: TransactionType): BigDecimal {
        return when (transactionType) {
            TransactionType.INTRA_STATE -> (cgstRate ?: BigDecimal.ZERO) + (sgstRate ?: BigDecimal.ZERO)
            TransactionType.INTER_STATE -> igstRate ?: totalGstRate
            TransactionType.UNION_TERRITORY -> (cgstRate ?: BigDecimal.ZERO) + (utgstRate ?: BigDecimal.ZERO)
            TransactionType.EXPORT -> BigDecimal.ZERO
            else -> totalGstRate
        }
    }

    fun getEffectiveCessRate(): BigDecimal {
        return cessRate ?: BigDecimal.ZERO
    }

    fun getEffectiveCessAmountPerUnit(): BigDecimal {
        return cessAmountPerUnit ?: BigDecimal.ZERO
    }

    fun isApplicableForZone(stateCode: String?): Boolean {
        if (geographicalZone == null || geographicalZone == GeographicalZone.ALL_INDIA) return true
        if (stateCode == null) return false

        return geographicalZone?.stateCodes?.contains(stateCode.uppercase()) == true
    }

    fun hasThresholdLimit(limitType: String): Boolean {
        return thresholdLimits.containsKey(limitType)
    }

    fun getThresholdLimit(limitType: String): BigDecimal? {
        val limit = thresholdLimits[limitType]
        return when (limit) {
            is Number -> BigDecimal.valueOf(limit.toDouble())
            is String -> limit.toBigDecimalOrNull()
            else -> null
        }
    }

    fun isExemptionApplicable(exemptionType: String, value: Any? = null): Boolean {
        val criteria = exemptionCriteria[exemptionType] ?: return false

        return when (criteria) {
            is Boolean -> criteria
            is Map<*, *> -> {
                if (value == null) return false
                val condition = criteria["condition"] as? String
                val threshold = criteria["threshold"]

                when (condition) {
                    "GREATER_THAN" -> (value as? Number)?.toDouble()?.let { it > (threshold as Number).toDouble() }
                        ?: false

                    "LESS_THAN" -> (value as? Number)?.toDouble()?.let { it < (threshold as Number).toDouble() }
                        ?: false

                    "EQUALS" -> value == threshold
                    else -> false
                }
            }

            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaxConfiguration) return false

        return businessTypeId == other.businessTypeId &&
                hsnCodeId == other.hsnCodeId &&
                geographicalZone == other.geographicalZone &&
                effectiveFrom == other.effectiveFrom
    }

    override fun hashCode(): Int {
        var result = businessTypeId.hashCode()
        result = 31 * result + hsnCodeId.hashCode()
        result = 31 * result + (geographicalZone?.hashCode() ?: 0)
        result = 31 * result + effectiveFrom.hashCode()
        return result
    }

    override fun toString(): String {
        return "TaxConfiguration(businessTypeId=$businessTypeId, hsnCodeId=$hsnCodeId, totalGstRate=$totalGstRate, geographicalZone=$geographicalZone)"
    }
}