package com.ampairs.tax.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.tax.config.Constants
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "tax_codes",
    indexes = [
        Index(name = "idxtax_code_workspace", columnList = "owner_id"),
        Index(name = "idx_tax_code_master", columnList = "master_tax_code_id"),
        Index(name = "idx_tax_code_updated", columnList = "updated_at"),
        Index(name = "idx_tax_code_favorite", columnList = "is_favorite"),
        Index(name = "idx_tax_code_active", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tax_code",
            columnNames = ["owner_id", "master_tax_code_id"]
        )
    ]
)
class TaxCode : OwnableBaseDomain() {

    @Column(name = "master_tax_code_id", nullable = false, length = 255)
    var masterTaxCodeId: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_tax_code_id", referencedColumnName = "uid", insertable = false, updatable = false)
    var masterTaxCode: MasterTaxCode? = null

    // Cached master data for offline access
    @Column(name = "code", nullable = false, length = 100)
    var code: String = ""

    @Column(name = "code_type", nullable = false, length = 50)
    var codeType: String = ""

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = ""

    @Column(name = "short_description", length = 500)
    var shortDescription: String = ""

    // Workspace-specific configuration
    @Column(name = "custom_name", length = 255)
    var customName: String? = null

    @Column(name = "custom_tax_rule_id", length = 255)
    var customTaxRuleId: String? = null

    @Column(name = "usage_count", nullable = false)
    var usageCount: Int = 0

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null

    @Column(name = "is_favorite", nullable = false)
    var isFavorite: Boolean = false

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "added_at", nullable = false)
    var addedAt: Instant = Instant.now()

    @Column(name = "sync_status", nullable = false, length = 50)
    var syncStatus: String = "SYNCED" // SYNCED, PENDING

    override fun obtainSeqIdPrefix(): String {
        return Constants.TAX_CODE_PREFIX
    }

    override fun toString(): String {
        return "WorkspaceTaxCode(ownerId='$ownerId', code='$code', codeType='$codeType')"
    }
}
