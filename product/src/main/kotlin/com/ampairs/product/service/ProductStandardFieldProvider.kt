package com.ampairs.product.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import org.springframework.stereotype.Component

/**
 * Declares the product entity's built-in (STANDARD) fields and sections — the single source of
 * truth for the product form (FR-020). Field keys mirror `Product` entity property names so the
 * client binds standard values to columns; unit/tax/category are dynamic choices.
 */
@Component
class ProductStandardFieldProvider : StandardFieldProvider {

    override fun entityType(): EntityType = EntityType.PRODUCT

    override fun sections(): List<StandardSectionSpec> = listOf(
        StandardSectionSpec(name = "Basic Info", order = 0),
        StandardSectionSpec(name = "Classification", order = 1),
        StandardSectionSpec(name = "Pricing", order = 2),
        StandardSectionSpec(name = "Tax & Units", order = 3),
    )

    override fun standardFields(): List<StandardFieldSpec> = listOf(
        // --- Basic Info ---
        StandardFieldSpec(
            key = "name", label = "Name", dataType = FieldDataType.TEXT, section = "Basic Info",
            order = 0, essential = true, mandatory = true,
            validationRules = listOf(com.ampairs.form.domain.model.validation.ValidationRule(com.ampairs.form.domain.model.validation.ValidationRuleType.REQUIRED)),
        ),
        StandardFieldSpec(key = "code", label = "Code", dataType = FieldDataType.TEXT, section = "Basic Info", order = 1),
        StandardFieldSpec(key = "sku", label = "SKU", dataType = FieldDataType.TEXT, section = "Basic Info", order = 2),
        StandardFieldSpec(key = "description", label = "Description", dataType = FieldDataType.TEXTAREA, section = "Basic Info", order = 3),
        StandardFieldSpec(
            key = "status", label = "Status", dataType = FieldDataType.CHOICE, section = "Basic Info",
            order = 4, optionSource = OptionSource.STATIC, enumValues = listOf("ACTIVE", "INACTIVE"), defaultValue = "ACTIVE",
        ),
        // --- Classification ---
        StandardFieldSpec(
            key = "groupId", label = "Group", dataType = FieldDataType.CHOICE, section = "Classification",
            order = 0, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "product_groups",
        ),
        StandardFieldSpec(
            key = "categoryId", label = "Category", dataType = FieldDataType.CHOICE, section = "Classification",
            order = 1, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "product_categories",
        ),
        StandardFieldSpec(
            key = "subCategoryId", label = "Sub-category", dataType = FieldDataType.CHOICE, section = "Classification",
            order = 2, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "product_sub_categories",
        ),
        StandardFieldSpec(
            key = "brandId", label = "Brand", dataType = FieldDataType.CHOICE, section = "Classification",
            order = 3, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "product_brands",
        ),
        // --- Pricing ---
        StandardFieldSpec(key = "basePrice", label = "Base Price", dataType = FieldDataType.NUMBER, section = "Pricing", order = 0),
        StandardFieldSpec(key = "costPrice", label = "Cost Price", dataType = FieldDataType.NUMBER, section = "Pricing", order = 1),
        StandardFieldSpec(key = "mrp", label = "MRP", dataType = FieldDataType.NUMBER, section = "Pricing", order = 2),
        StandardFieldSpec(key = "sellingPrice", label = "Selling Price", dataType = FieldDataType.NUMBER, section = "Pricing", order = 3),
        // --- Tax & Units ---
        StandardFieldSpec(
            key = "taxCodeId", label = "Tax Code", dataType = FieldDataType.CHOICE, section = "Tax & Units",
            order = 0, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "tax_codes",
        ),
        StandardFieldSpec(
            key = "baseUnitId", label = "Unit", dataType = FieldDataType.CHOICE, section = "Tax & Units",
            order = 1, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "units",
        ),
    )
}
