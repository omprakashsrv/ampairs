package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Add-on module purchased for a subscription.
 * Tracks additional features/modules added to base subscription.
 */
@Entity
@Table(
    name = "subscription_addons",
    indexes = [
        Index(name = "idx_addon_uid", columnList = "uid", unique = true),
        Index(name = "idx_addon_subscription", columnList = "subscription_id"),
        Index(name = "idx_addon_workspace", columnList = "workspace_id"),
        Index(name = "idx_addon_code", columnList = "addon_code"),
        Index(name = "idx_addon_status", columnList = "status")
    ]
)
class SubscriptionAddon : BaseDomain() {

    /**
     * Parent subscription
     */
    @Column(name = "subscription_id", nullable = false, length = 200)
    var subscriptionId: String = ""

    /**
     * Workspace ID (denormalized for quick queries)
     */
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    /**
     * Add-on module code
     */
    @Column(name = "addon_code", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var addonCode: AddonModuleCode = AddonModuleCode.TALLY_INTEGRATION

    /**
     * Status of the add-on
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE

    /**
     * Price at time of purchase
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO

    /**
     * Currency
     */
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "INR"

    /**
     * When the add-on was activated
     */
    @Column(name = "activated_at")
    var activatedAt: Instant? = null

    /**
     * When the add-on expires/renews
     */
    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    /**
     * When cancellation was requested
     */
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null

    /**
     * External subscription item ID from payment provider
     */
    @Column(name = "external_item_id", length = 255)
    var externalItemId: String? = null

    override fun obtainSeqIdPrefix(): String {
        return "ADDON"
    }

    fun isActive(): Boolean {
        return status == SubscriptionStatus.ACTIVE
    }
}
