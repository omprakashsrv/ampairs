package com.ampairs.customer.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.model.validation.FormatKind
import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.model.validation.ValidationRuleType
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import org.springframework.stereotype.Component

/**
 * Declares the customer entity's built-in (STANDARD) fields and sections — the single source of
 * truth for the customer form (FR-020). Field keys mirror `Customer` entity property names so the
 * client binds standard values to columns; `customer_type` / `customer_group` are dynamic choices.
 */
@Component
class CustomerStandardFieldProvider : StandardFieldProvider {

    override fun entityType(): EntityType = EntityType.CUSTOMER

    override fun sections(): List<StandardSectionSpec> = listOf(
        StandardSectionSpec(name = "Basic Info", order = 0),
        StandardSectionSpec(name = "Contact", order = 1),
        StandardSectionSpec(name = "Tax & Credit", order = 2),
        StandardSectionSpec(name = "Address", order = 3),
    )

    override fun standardFields(): List<StandardFieldSpec> = listOf(
        // --- Basic Info ---
        StandardFieldSpec(
            key = "name", label = "Name", dataType = FieldDataType.TEXT, section = "Basic Info",
            order = 0, essential = true, mandatory = true,
            validationRules = listOf(ValidationRule(ValidationRuleType.REQUIRED)),
        ),
        StandardFieldSpec(
            key = "customerType", label = "Customer Type", dataType = FieldDataType.CHOICE, section = "Basic Info",
            order = 1, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "customer_types",
        ),
        StandardFieldSpec(
            key = "customerGroup", label = "Customer Group", dataType = FieldDataType.CHOICE, section = "Basic Info",
            order = 2, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "customer_groups",
        ),
        StandardFieldSpec(
            key = "status", label = "Status", dataType = FieldDataType.CHOICE, section = "Basic Info",
            order = 3, optionSource = OptionSource.STATIC, enumValues = listOf("ACTIVE", "INACTIVE"),
            defaultValue = "ACTIVE",
        ),
        // --- Contact ---
        StandardFieldSpec(
            key = "phone", label = "Phone", dataType = FieldDataType.TEXT, section = "Contact", order = 0,
            validationRules = listOf(ValidationRule(ValidationRuleType.FORMAT, kind = FormatKind.PHONE)),
        ),
        StandardFieldSpec(key = "landline", label = "Landline", dataType = FieldDataType.TEXT, section = "Contact", order = 1),
        StandardFieldSpec(
            key = "email", label = "Email", dataType = FieldDataType.TEXT, section = "Contact", order = 2,
            validationRules = listOf(ValidationRule(ValidationRuleType.FORMAT, kind = FormatKind.EMAIL)),
        ),
        // --- Tax & Credit ---
        StandardFieldSpec(
            key = "gstNumber", label = "GST Number", dataType = FieldDataType.TEXT, section = "Tax & Credit", order = 0,
            validationRules = listOf(ValidationRule(ValidationRuleType.FORMAT, kind = FormatKind.GSTIN)),
        ),
        StandardFieldSpec(
            key = "panNumber", label = "PAN Number", dataType = FieldDataType.TEXT, section = "Tax & Credit", order = 1,
            validationRules = listOf(ValidationRule(ValidationRuleType.FORMAT, kind = FormatKind.PAN)),
        ),
        StandardFieldSpec(key = "creditLimit", label = "Credit Limit", dataType = FieldDataType.NUMBER, section = "Tax & Credit", order = 2),
        StandardFieldSpec(key = "creditDays", label = "Credit Days", dataType = FieldDataType.NUMBER, section = "Tax & Credit", order = 3),
        // --- Address ---
        StandardFieldSpec(key = "address", label = "Address", dataType = FieldDataType.TEXTAREA, section = "Address", order = 0),
        StandardFieldSpec(key = "street", label = "Street", dataType = FieldDataType.TEXT, section = "Address", order = 1),
        StandardFieldSpec(key = "city", label = "City", dataType = FieldDataType.TEXT, section = "Address", order = 2),
        StandardFieldSpec(key = "pincode", label = "Pincode", dataType = FieldDataType.TEXT, section = "Address", order = 3),
        StandardFieldSpec(key = "state", label = "State", dataType = FieldDataType.TEXT, section = "Address", order = 4),
        StandardFieldSpec(key = "country", label = "Country", dataType = FieldDataType.TEXT, section = "Address", order = 5, defaultValue = "India"),
    )
}
