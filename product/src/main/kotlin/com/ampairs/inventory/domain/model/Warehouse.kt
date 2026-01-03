package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.Address
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Warehouse Entity
 *
 * Represents a physical warehouse, store, godown, or other storage location
 * where inventory is stored. Supports multi-location inventory management.
 *
 * Key Features:
 * - Multi-tenant aware via @TenantId
 * - Flexible warehouse types (WAREHOUSE, STORE, GODOWN, SHOWROOM, FACTORY)
 * - Address embedding for location details
 * - Default warehouse designation per tenant
 * - Extensible attributes via JSON storage
 */
@Entity(name = "warehouse")
@Table(
    name = "warehouse",
    indexes = [
        Index(name = "idx_warehouse_uid", columnList = "uid"),
        Index(name = "idx_warehouse_code", columnList = "code"),
        Index(name = "idx_warehouse_owner_id", columnList = "owner_id"),
        Index(name = "idx_warehouse_is_active", columnList = "is_active"),
        Index(name = "idx_warehouse_is_default", columnList = "is_default")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_warehouse_code_owner", columnNames = ["code", "owner_id"])
    ]
)
@NamedEntityGraph(
    name = "Warehouse.full",
    attributeNodes = []  // Address is embedded, no need to fetch separately
)
class Warehouse : OwnableBaseDomain() {

    /**
     * Warehouse name (e.g., "Main Warehouse", "Downtown Store")
     */
    @Column(name = "name", nullable = false, length = 200)
    var name: String = ""

    /**
     * Unique warehouse code within tenant (e.g., "WH-001", "STORE-MAIN")
     */
    @Column(name = "code", nullable = false, length = 50)
    var code: String = ""

    /**
     * Warehouse type categorization
     * Values: WAREHOUSE, STORE, GODOWN, SHOWROOM, FACTORY
     */
    @Column(name = "warehouse_type", nullable = false, length = 50)
    var warehouseType: String = Constants.WAREHOUSE_TYPE_WAREHOUSE

    /**
     * Active status flag
     * Inactive warehouses cannot receive new inventory transactions
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    /**
     * Default warehouse flag
     * Only one warehouse per tenant should be marked as default
     */
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    /**
     * Contact phone number
     */
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /**
     * Contact email address
     */
    @Column(name = "email", length = 100)
    var email: String? = null

    /**
     * Warehouse manager or contact person name
     */
    @Column(name = "manager_name", length = 100)
    var managerName: String? = null

    /**
     * Physical address of the warehouse
     * Embedded value object containing street, city, state, pincode, etc.
     */
    @Embedded
    var address: Address = Address()

    /**
     * Additional flexible attributes stored as JSON
     * Can include capacity, area, operating hours, etc.
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: Map<String, Any>? = null

    /**
     * Description or notes about the warehouse
     */
    @Column(name = "description", length = 500)
    var description: String? = null

    /**
     * Obtain the sequence ID prefix for UID generation
     */
    override fun obtainSeqIdPrefix(): String {
        return Constants.WAREHOUSE_PREFIX
    }

    override fun toString(): String {
        return "Warehouse(uid='$uid', code='$code', name='$name', type='$warehouseType', isActive=$isActive, isDefault=$isDefault)"
    }
}
