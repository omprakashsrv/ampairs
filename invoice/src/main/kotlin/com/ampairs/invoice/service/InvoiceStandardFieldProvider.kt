package com.ampairs.invoice.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import org.springframework.stereotype.Component

/**
 * Declares the invoice entity's built-in (STANDARD) fields and sections (FR-020). Field keys mirror
 * `Invoice` entity property names. Monetary totals and line items are derived, not form fields, so
 * only user-editable header fields are declared.
 */
@Component
class InvoiceStandardFieldProvider : StandardFieldProvider {

    override fun entityType(): EntityType = EntityType.INVOICE

    override fun sections(): List<StandardSectionSpec> = listOf(
        StandardSectionSpec(name = "Invoice Details", order = 0),
        StandardSectionSpec(name = "Parties", order = 1),
    )

    override fun standardFields(): List<StandardFieldSpec> = listOf(
        // --- Invoice Details ---
        StandardFieldSpec(key = "series", label = "Series", dataType = FieldDataType.TEXT, section = "Invoice Details", order = 0, defaultValue = "INV"),
        StandardFieldSpec(key = "invoiceDate", label = "Invoice Date", dataType = FieldDataType.DATE, section = "Invoice Details", order = 1),
        StandardFieldSpec(key = "placeOfSupply", label = "Place of Supply", dataType = FieldDataType.TEXT, section = "Invoice Details", order = 2),
        StandardFieldSpec(
            key = "priceMode", label = "Price Mode", dataType = FieldDataType.CHOICE, section = "Invoice Details",
            order = 3, optionSource = OptionSource.STATIC, enumValues = listOf("TAX_EXCLUSIVE", "TAX_INCLUSIVE"), defaultValue = "TAX_EXCLUSIVE",
        ),
        // --- Parties ---
        StandardFieldSpec(
            key = "toCustomerId", label = "Customer", dataType = FieldDataType.CHOICE, section = "Parties",
            order = 0, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "customers",
        ),
        StandardFieldSpec(key = "toCustomerGst", label = "Customer GST", dataType = FieldDataType.TEXT, section = "Parties", order = 1),
    )
}
