package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

/**
 * Workspace-specific customer types for business categorization.
 * Each workspace can define their own customer types with custom attributes.
 */
@Entity
@Table(
    name = "customer_types",
    indexes = [
        Index(name = "idx_customer_type_code_workspace", columnList = "type_code,workspace_id", unique = true),
        Index(name = "idx_customer_type_name", columnList = "name"),
        Index(name = "idx_customer_type_active", columnList = "active"),
        Index(name = "idx_customer_type_workspace", columnList = "workspace_id")
    ]
)
class CustomerType : OwnableBaseDomain() {

    /**
     * Unique identifier code for the customer type within workspace (e.g., "RETAIL", "WHOLESALE")
     */
    @Column(name = "type_code", nullable = false, length = 20)
    var typeCode: String = ""

    /**
     * Full name of the customer type
     */
    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    /**
     * Short description of the customer type
     */
    @Column(name = "description", length = 255)
    var description: String? = null

    /**
     * Display order for UI sorting
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Whether this customer type is active and available for selection
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Default credit limit for this customer type
     */
    @Column(name = "default_credit_limit", nullable = false)
    var defaultCreditLimit: Double = 0.0

    /**
     * Default credit days for this customer type
     */
    @Column(name = "default_credit_days", nullable = false)
    var defaultCreditDays: Int = 0

    /**
     * Additional metadata in JSON format
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_TYPE_PREFIX
    }

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return name
    }

    /**
     * Check if this type allows credit
     */
    fun allowsCredit(): Boolean {
        return defaultCreditLimit > 0
    }
}