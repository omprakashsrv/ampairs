package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*

/**
 * Workspace-specific customer groups for business segmentation.
 * Each workspace can define their own customer groups with custom attributes.
 */
@Entity
@Table(
    name = "customer_groups",
    indexes = [
        Index(name = "idx_customer_group_code_workspace", columnList = "group_code,workspace_id", unique = true),
        Index(name = "idx_customer_group_name", columnList = "name"),
        Index(name = "idx_customer_group_active", columnList = "active"),
        Index(name = "idx_customer_group_workspace", columnList = "workspace_id")
    ]
)
class CustomerGroup : OwnableBaseDomain() {

    /**
     * Unique identifier code for the customer group within workspace (e.g., "VIP", "REGULAR", "BULK_BUYER")
     */
    @Column(name = "group_code", nullable = false, length = 20)
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
        return Constants.CUSTOMER_GROUP_PREFIX
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