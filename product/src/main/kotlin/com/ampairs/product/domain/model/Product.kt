package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import jakarta.persistence.*

@Entity(name = "product")
class Product : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "code", nullable = false, length = 255)
    var code: String = ""

    @Column(name = "tax_code", length = 20)
    var taxCode: String = ""

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
    @JoinColumn(name = "group_id", referencedColumnName = "id", updatable = false, insertable = false)
    var group: ProductGroup? = null

    @OneToOne()
    @JoinColumn(name = "brand_id", referencedColumnName = "id", updatable = false, insertable = false)
    var brand: ProductBrand? = null

    @OneToOne()
    @JoinColumn(name = "category_id", referencedColumnName = "id", updatable = false, insertable = false)
    var category: ProductCategory? = null

    @OneToOne()
    @JoinColumn(name = "sub_category_id", referencedColumnName = "id", updatable = false, insertable = false)
    var subCategory: ProductSubCategory? = null

    @OneToOne()
    @JoinColumn(name = "base_unit_id", referencedColumnName = "id", updatable = false, insertable = false)
    var baseUnit: Unit? = null

    @Column(name = "index_no", nullable = false)
    var index: Int = 0

    @OneToMany()
    @JoinColumn(
        name = "code", referencedColumnName = "tax_code", insertable = false, updatable = false, nullable = false
    )
    var taxCodes: MutableList<TaxCode> = mutableListOf()

    @OneToMany()
    @JoinColumn(
        name = "product_id", referencedColumnName = "id", insertable = false, updatable = false, nullable = false
    )
    var images: MutableList<ProductImage> = mutableListOf()

    @OneToMany()
    @JoinColumn(
        name = "product_id", referencedColumnName = "id", insertable = false, updatable = false, nullable = false
    )
    var unitConversions: MutableList<UnitConversion> = mutableListOf()

    override fun obtainIdPrefix(): String {
        return Constants.PRODUCT_PREFIX
    }
}