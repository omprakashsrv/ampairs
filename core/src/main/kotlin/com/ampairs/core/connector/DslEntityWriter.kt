package com.ampairs.core.connector

import com.ampairs.core.domain.model.OwnableBaseDomain

/**
 * A [ConnectorEntityWriter] with a small **mapping DSL** for fields whose target is a complex/nested
 * object (a collection, an embedded value, a related entity) that flat setter reflection can't handle.
 *
 * A writer declares only its complex fields via [defineFields]; every other present column falls back
 * to flat reflection ([SparsePropertyApplier]). This keeps the complex, entity-specific logic inside
 * the owning module (Constitution Principle IX) while staying type-safe.
 *
 * Example (mapping a scalar value into a related/child object owned by this module's service):
 * ```
 * override fun defineFields(f: FieldBinding<Product>) {
 *     f.field("stockQuantity") { product, value ->
 *         val qty = (value as? Number)?.toBigDecimal() ?: return@field
 *         // Delegate to the owning module's public service (Principle IX) — never reach into another
 *         // module's repositories. e.g. inventoryStockWriter.setOnHand(product.uid, qty)
 *         stockWriter.setOnHand(product.uid, qty)
 *     }
 *     // "name", "refId", ... not declared here → applied by reflection automatically
 * }
 * ```
 */
abstract class DslEntityWriter<T : OwnableBaseDomain> : AbstractRefIdEntityWriter<T>() {

    /** Builder collecting custom field appliers keyed by Ampairs field name. */
    class FieldBinding<T> {
        internal val custom = LinkedHashMap<String, (T, Any?) -> Unit>()

        /** Register a custom applier for a complex/nested target field. */
        fun field(name: String, apply: (T, Any?) -> Unit) {
            custom[name] = apply
        }
    }

    private val bindings: Map<String, (T, Any?) -> Unit> by lazy {
        FieldBinding<T>().also { defineFields(it) }.custom
    }

    /** Declare custom field appliers; fields not declared here are applied by flat reflection. */
    protected abstract fun defineFields(f: FieldBinding<T>)

    override fun applyColumns(entity: T, columns: Map<String, Any?>): List<String> {
        val applied = mutableListOf<String>()
        for ((name, value) in columns) {
            val custom = bindings[name]
            if (custom != null) {
                runCatching { custom(entity, value) }.onSuccess { applied += name }
            } else if (SparsePropertyApplier.applyOne(entity, name, value)) {
                applied += name
            }
        }
        return applied
    }
}
