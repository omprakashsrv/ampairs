package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.product.config.Constants
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.domain.model.UnitConversion
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity(name = "product")
@Table(
    indexes = [
        Index(name = "idx_product_uid", columnList = "uid", unique = true)
    ]
)
class Product : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "code", nullable = false, length = 255)
    var code: String = ""

    @Column(name = "sku", nullable = false, length = 100, unique = true)
    var sku: String = ""

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"

    @Column(name = "tax_code_id", length = 36)
    var taxCodeId: String? = null

    @Column(name = "tax_code", length = 20)
    var taxCode: String = ""

    @Column(name = "unit_id", length = 36)
    var unitId: String? = null

    @Column(name = "base_price", nullable = false)
    var basePrice: Double = 0.0

    @Column(name = "cost_price", nullable = false)
    var costPrice: Double = 0.0

    @Column(name = "group_id", length = 200)
    var groupId: String? = null

    @Column(name = "brand_id", length = 200)
    var brandId: String? = null

    @Column(name = "category_id", length = 200)
    var categoryId: String? = null

    @Column(name = "sub_category_id", length = 200)
    var subCategoryId: String? = null

    @Column(name = "base_unit_id", length = 200)
    var baseUnitId: String? = null

    @Column(name = "mrp", nullable = false)
    var mrp: Double = 0.0

    @Column(name = "dp", nullable = false)
    var dp: Double = 0.0

    @Column(name = "selling_price", nullable = false)
    var sellingPrice: Double = 0.0

    @OneToOne()
    @JoinColumn(name = "group_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var group: ProductGroup? = null

    @OneToOne()
    @JoinColumn(name = "brand_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var brand: ProductBrand? = null

    @OneToOne()
    @JoinColumn(name = "category_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var category: ProductCategory? = null

    @OneToOne()
    @JoinColumn(name = "sub_category_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var subCategory: ProductSubCategory? = null

    @OneToOne()
    @JoinColumn(name = "base_unit_id", referencedColumnName = "uid", updatable = false, insertable = false)
    var baseUnit: Unit? = null

    @Column(name = "index_no", nullable = false)
    var index: Int = 0

    /**
     * Retail business-specific attributes stored as JSON
     * Examples:
     * - JEWELRY: weight_grams, purity, metal_type, stone_type, certification
     * - KIRANA: pack_size, brand, expiry_tracking, bulk_discount_threshold
     * - HARDWARE: material, dimensions, warranty_months, safety_rating
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any> = emptyMap()

    @OneToMany()
    @JoinColumn(
        name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var images: MutableList<ProductImage> = mutableListOf()

    @OneToMany()
    @JoinColumn(
        name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var unitConversions: MutableList<UnitConversion> = mutableListOf()

    @OneToMany
    @JoinColumn(
        name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false, nullable = false
    )
    var inventory: MutableList<Inventory> = mutableListOf()


    override fun obtainSeqIdPrefix(): String {
        return Constants.PRODUCT_PREFIX
    }
}
