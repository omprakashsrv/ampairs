package com.ampairs.order.service

import com.ampairs.form.domain.model.EntityType
import com.ampairs.form.domain.model.FieldDataType
import com.ampairs.form.domain.model.OptionSource
import com.ampairs.form.domain.service.registry.StandardFieldProvider
import com.ampairs.form.domain.service.registry.StandardFieldSpec
import com.ampairs.form.domain.service.registry.StandardSectionSpec
import org.springframework.stereotype.Component

/**
 * Declares the order entity's built-in (STANDARD) fields and sections (FR-020). Field keys mirror
 * `Order` entity property names. Computed monetary totals and line items are NOT form fields —
 * they're derived from the order's items, so only user-editable header fields are declared here.
 */
@Component
class OrderStandardFieldProvider : StandardFieldProvider {

    override fun entityType(): EntityType = EntityType.ORDER

    override fun sections(): List<StandardSectionSpec> = listOf(
        StandardSectionSpec(name = "Order Details", order = 0),
        StandardSectionSpec(name = "Parties", order = 1),
        StandardSectionSpec(name = "Notes", order = 2),
    )

    override fun standardFields(): List<StandardFieldSpec> = listOf(
        // --- Order Details ---
        StandardFieldSpec(
            key = "orderType", label = "Order Type", dataType = FieldDataType.CHOICE, section = "Order Details",
            order = 0, optionSource = OptionSource.STATIC, enumValues = listOf("REGULAR", "RETURN"), defaultValue = "REGULAR",
        ),
        StandardFieldSpec(
            key = "paymentMethod", label = "Payment Method", dataType = FieldDataType.CHOICE, section = "Order Details",
            order = 1, optionSource = OptionSource.STATIC, enumValues = listOf("CASH", "CARD", "UPI", "CREDIT"), defaultValue = "CASH",
        ),
        StandardFieldSpec(key = "orderDate", label = "Order Date", dataType = FieldDataType.DATE, section = "Order Details", order = 2),
        StandardFieldSpec(key = "deliveryDate", label = "Delivery Date", dataType = FieldDataType.DATE, section = "Order Details", order = 3),
        StandardFieldSpec(key = "placeOfSupply", label = "Place of Supply", dataType = FieldDataType.TEXT, section = "Order Details", order = 4),
        // --- Parties ---
        StandardFieldSpec(
            key = "customerId", label = "Customer", dataType = FieldDataType.CHOICE, section = "Parties",
            order = 0, optionSource = OptionSource.DYNAMIC, dynamicSourceKey = "customers",
        ),
        StandardFieldSpec(key = "isWalkIn", label = "Walk-in", dataType = FieldDataType.BOOLEAN, section = "Parties", order = 1),
        // --- Notes ---
        StandardFieldSpec(key = "notes", label = "Notes", dataType = FieldDataType.TEXTAREA, section = "Notes", order = 0),
        StandardFieldSpec(key = "internalNotes", label = "Internal Notes", dataType = FieldDataType.TEXTAREA, section = "Notes", order = 1),
    )
}
