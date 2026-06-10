package com.ampairs.workspace.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.model.validation.ValidationRule
import com.ampairs.form.domain.model.validation.ValidationRuleType
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import org.springframework.stereotype.Component

/**
 * Declares the business (workspace) profile's built-in (STANDARD) fields and sections for
 * `EntityType.BUSINESS` (FR-020). Lives in the `workspace` module — there is no separate `business`
 * backend module. Field keys mirror `Workspace` entity property names.
 */
@Component
class BusinessStandardFieldProvider : StandardFieldProvider {

    override fun entityType(): EntityType = EntityType.BUSINESS

    override fun sections(): List<StandardSectionSpec> = listOf(
        StandardSectionSpec(name = "Business Profile", order = 0),
        StandardSectionSpec(name = "Contact", order = 1),
        StandardSectionSpec(name = "Address", order = 2),
        StandardSectionSpec(name = "Tax & Registration", order = 3),
        StandardSectionSpec(name = "Localization", order = 4),
    )

    override fun standardFields(): List<StandardFieldSpec> = listOf(
        // --- Business Profile ---
        StandardFieldSpec(
            key = "name", label = "Business Name", dataType = FieldDataType.TEXT, section = "Business Profile",
            order = 0, essential = true, mandatory = true,
            validationRules = listOf(ValidationRule(ValidationRuleType.REQUIRED)),
        ),
        StandardFieldSpec(key = "description", label = "Description", dataType = FieldDataType.TEXTAREA, section = "Business Profile", order = 1),
        StandardFieldSpec(key = "website", label = "Website", dataType = FieldDataType.TEXT, section = "Business Profile", order = 2),
        // --- Contact ---
        StandardFieldSpec(key = "phone", label = "Phone", dataType = FieldDataType.TEXT, section = "Contact", order = 0),
        StandardFieldSpec(key = "email", label = "Email", dataType = FieldDataType.TEXT, section = "Contact", order = 1),
        // --- Address ---
        StandardFieldSpec(key = "city", label = "City", dataType = FieldDataType.TEXT, section = "Address", order = 0),
        StandardFieldSpec(key = "state", label = "State", dataType = FieldDataType.TEXT, section = "Address", order = 1),
        StandardFieldSpec(key = "postalCode", label = "Postal Code", dataType = FieldDataType.TEXT, section = "Address", order = 2),
        StandardFieldSpec(key = "country", label = "Country", dataType = FieldDataType.TEXT, section = "Address", order = 3),
        // --- Tax & Registration ---
        StandardFieldSpec(key = "taxId", label = "Tax ID", dataType = FieldDataType.TEXT, section = "Tax & Registration", order = 0),
        StandardFieldSpec(key = "registrationNumber", label = "Registration Number", dataType = FieldDataType.TEXT, section = "Tax & Registration", order = 1),
        // --- Localization ---
        StandardFieldSpec(
            key = "currency", label = "Currency", dataType = FieldDataType.CHOICE, section = "Localization",
            order = 0, optionSource = OptionSource.STATIC, enumValues = listOf("INR", "USD", "EUR", "GBP", "AED"), defaultValue = "INR",
        ),
        StandardFieldSpec(key = "timezone", label = "Timezone", dataType = FieldDataType.TEXT, section = "Localization", order = 1, defaultValue = "UTC"),
    )
}
