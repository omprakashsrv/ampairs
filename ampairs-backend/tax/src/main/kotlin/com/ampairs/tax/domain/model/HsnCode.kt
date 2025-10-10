package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "hsn_codes",
    indexes = [
        Index(name = "idx_hsn_code", columnList = "hsn_code", unique = true),
        Index(name = "idx_hsn_chapter", columnList = "hsn_chapter"),
        Index(name = "idx_hsn_heading", columnList = "hsn_heading"),
        Index(name = "idx_hsn_parent", columnList = "parent_hsn_id"),
        Index(name = "idx_hsn_active", columnList = "active")
    ]
)
@NamedEntityGraph(
    name = "HsnCode.withTaxRates",
    attributeNodes = [NamedAttributeNode("taxRates")]
)
class HsnCode : OwnableBaseDomain() {

    @Column(name = "hsn_code", nullable = false, unique = true, length = 10)
    var hsnCode: String = ""

    @Column(name = "hsn_description", nullable = false, columnDefinition = "TEXT")
    var hsnDescription: String = ""

    @Column(name = "hsn_chapter", length = 2)
    var hsnChapter: String? = null

    @Column(name = "hsn_heading", length = 4)
    var hsnHeading: String? = null

    @Column(name = "parent_hsn_id")
    var parentHsnId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_hsn_id", referencedColumnName = "id", insertable = false, updatable = false)
    var parentHsn: HsnCode? = null

    @OneToMany(mappedBy = "parentHsn", fetch = FetchType.LAZY)
    var childHsnCodes: MutableList<HsnCode> = mutableListOf()

    @Column(name = "level", nullable = false)
    var level: Int = 1

    @Column(name = "unit_of_measurement", length = 50)
    var unitOfMeasurement: String? = null

    @Column(name = "exemption_available", nullable = false)
    var exemptionAvailable: Boolean = false

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_category_rules", columnDefinition = "JSON")
    var businessCategoryRules: Map<String, Any> = emptyMap()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any> = emptyMap()

    @OneToMany(mappedBy = "hsnCode", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var taxRates: MutableList<TaxRate> = mutableListOf()

    @Column(name = "effective_from")
    var effectiveFrom: Instant? = null

    @Column(name = "effective_to")
    var effectiveTo: Instant? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.HSN_CODE_PREFIX
    }

//    fun isValidForDate(date: LocalDateTime = LocalDateTime.now()): Boolean {
//        return active &&
//                (effectiveFrom == null || date.isAfter(effectiveFrom) || date.isEqual(effectiveFrom)) &&
//                (effectiveTo == null || date.isBefore(effectiveTo))
//    }

    fun getFullPath(): String {
        val path = mutableListOf<String>()
        var current: HsnCode? = this
        while (current != null) {
            path.add(0, current.hsnCode)
            current = current.parentHsn
        }
        return path.joinToString(" > ")
    }

    fun hasValidTaxRates(date: Instant = Instant.now()): Boolean {
        return taxRates.any { it.isValidForDate(date) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HsnCode) return false
        return hsnCode == other.hsnCode
    }

    override fun hashCode(): Int {
        return hsnCode.hashCode()
    }

    override fun toString(): String {
        return "HsnCode(hsnCode='$hsnCode', description='$hsnDescription')"
    }
}