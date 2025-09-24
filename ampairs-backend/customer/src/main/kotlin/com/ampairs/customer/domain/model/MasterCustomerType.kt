package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

/**
 * Master registry of customer types available in the Ampairs system.
 * This serves as the central catalog from which workspaces can import customer types.
 */
@Entity
@Table(
    name = "master_customer_types",
    indexes = [
        Index(name = "idx_master_customer_type_code", columnList = "type_code", unique = true),
        Index(name = "idx_master_customer_type_name", columnList = "name"),
        Index(name = "idx_master_customer_type_active", columnList = "active")
    ]
)
class MasterCustomerType : BaseDomain() {

    /**
     * Unique identifier code for the customer type (e.g., "RETAIL", "WHOLESALE")
     */
    @Column(name = "type_code", nullable = false, unique = true, length = 20)
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
        return Constants.MASTER_CUSTOMER_TYPE_PREFIX
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