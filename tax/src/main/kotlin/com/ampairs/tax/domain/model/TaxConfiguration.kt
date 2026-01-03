package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "tax_configuration",
    indexes = [
        Index(name = "idx_tax_config", columnList = "id"),
        Index(name = "idx_tax_config_country", columnList = "country_code"),
        Index(name = "idx_tax_config_updated", columnList = "updated_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tax_config",
            columnNames = ["owner_id"]
        )
    ]
)
class TaxConfiguration : OwnableBaseDomain() {

    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    @Column(name = "tax_strategy", nullable = false, length = 50)
    var taxStrategy: String = "" // INDIA_GST, USA_SALES_TAX, UK_VAT, etc.

    @Column(name = "default_tax_code_system", nullable = false, length = 50)
    var defaultTaxCodeSystem: String = "" // HSN_CODE, SAC_CODE, TAX_CATEGORY

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_jurisdictions", columnDefinition = "jsonb")
    var taxJurisdictions: List<String> = emptyList()

    @Column(name = "industry", length = 100)
    var industry: String? = null

    @Column(name = "auto_subscribe_new_codes", nullable = false)
    var autoSubscribeNewCodes: Boolean = true

    @Column(name = "synced_at", nullable = false)
    var syncedAt: Instant = Instant.now()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, String> = emptyMap()

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_CONFIG_PREFIX
    }

    override fun toString(): String {
        return "TaxConfiguration(ownerId='$ownerId', countryCode='$countryCode', taxStrategy='$taxStrategy')"
    }
}
