package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "master_tax_codes",
    indexes = [
        Index(name = "idx_master_tax_country", columnList = "country_code"),
        Index(name = "idx_master_tax_code_type", columnList = "code_type"),
        Index(name = "idx_master_tax_code", columnList = "code"),
        Index(name = "idx_master_tax_active", columnList = "is_active"),
        Index(name = "idx_master_tax_updated", columnList = "updated_at"),
        Index(name = "idx_master_tax_lookup", columnList = "country_code, code_type, is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_master_tax_code",
            columnNames = ["country_code", "code_type", "code"]
        )
    ]
)
class MasterTaxCode : BaseDomain() {

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    @Column(name = "code_type", nullable = false, length = 50)
    var codeType: String = "" // HSN_CODE, SAC_CODE, TAX_CATEGORY, etc.

    @Column(name = "code", nullable = false, length = 100)
    var code: String = ""

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = ""

    @Column(name = "short_description", length = 500)
    var shortDescription: String = ""

    @Column(name = "chapter", length = 10)
    var chapter: String? = null

    @Column(name = "heading", length = 10)
    var heading: String? = null

    @Column(name = "sub_heading", length = 20)
    var subHeading: String? = null

    @Column(name = "category", length = 100)
    var category: String? = null

    @Column(name = "default_tax_rate")
    var defaultTaxRate: Double? = null

    @Column(name = "default_tax_slab_id", length = 255)
    var defaultTaxSlabId: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, String> = emptyMap()

    override fun obtainSeqIdPrefix(): String {
        return Constants.MASTER_TAX_CODE_PREFIX
    }

    override fun toString(): String {
        return "MasterTaxCode(countryCode='$countryCode', codeType='$codeType', code='$code', description='$description')"
    }
}
