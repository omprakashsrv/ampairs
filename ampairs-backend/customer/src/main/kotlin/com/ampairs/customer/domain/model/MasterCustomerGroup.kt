package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

/**
 * Master registry of customer groups available in the Ampairs system.
 * This serves as the central catalog from which workspaces can import customer groups.
 */
@Entity
@Table(
    name = "master_customer_groups",
    indexes = [
        Index(name = "idx_master_customer_group_code", columnList = "group_code", unique = true),
        Index(name = "idx_master_customer_group_name", columnList = "name"),
        Index(name = "idx_master_customer_group_active", columnList = "active")
    ]
)
class MasterCustomerGroup : BaseDomain() {

    /**
     * Unique identifier code for the customer group (e.g., "VIP", "REGULAR", "BULK_BUYER")
     */
    @Column(name = "group_code", nullable = false, unique = true, length = 20)
    var groupCode: String = ""

    /**
     * Full name of the customer group
     */
    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    /**
     * Short description of the customer group
     */
    @Column(name = "description", length = 255)
    var description: String? = null

    /**
     * Display order for UI sorting
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Whether this customer group is active and available for selection
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Default discount percentage for this group
     */
    @Column(name = "default_discount_percentage", nullable = false)
    var defaultDiscountPercentage: Double = 0.0

    /**
     * Priority level for this group (higher number = higher priority)
     */
    @Column(name = "priority_level", nullable = false)
    var priorityLevel: Int = 0

    /**
     * Additional metadata in JSON format
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.MASTER_CUSTOMER_GROUP_PREFIX
    }

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return name
    }

    /**
     * Check if this group has discount benefits
     */
    fun hasDiscount(): Boolean {
        return defaultDiscountPercentage > 0
    }
}