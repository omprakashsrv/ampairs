package com.ampairs.core.connector

/**
 * Applies a sparse map of `propertyName → value` onto a mutable entity via JVM setter reflection
 * (`var name` → `setName`). Used by [ConnectorEntityWriter] implementations to write only the columns
 * present in a connector row. Returns the property names actually applied.
 *
 * Handles flat scalar fields only (String/Int/Long/Double/Boolean + null). Complex/nested targets
 * (collections, embedded objects) are handled by per-writer custom appliers — see [DslEntityWriter].
 */
object SparsePropertyApplier {

    fun apply(target: Any, columns: Map<String, Any?>): List<String> {
        val applied = mutableListOf<String>()
        for ((name, value) in columns) {
            if (applyOne(target, name, value)) applied += name
        }
        return applied
    }

    /** Apply a single column via its setter; returns true if it was applied. */
    fun applyOne(target: Any, name: String, value: Any?): Boolean {
        val setter = target.javaClass.methods.firstOrNull {
            it.name == setterName(name) && it.parameterCount == 1
        } ?: return false
        val paramType = setter.parameterTypes[0]
        val coerced = runCatching { coerce(value, paramType) }.getOrNull()
        if (value != null && coerced == null && !paramType.isAssignableFrom(value.javaClass)) {
            return false // could not coerce — skip rather than corrupt
        }
        return runCatching { setter.invoke(target, coerced); true }.getOrDefault(false)
    }

    private fun setterName(prop: String): String =
        "set" + prop.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun coerce(value: Any?, type: Class<*>): Any? {
        if (value == null) return null
        return when {
            type.isInstance(value) -> value
            type == String::class.java -> value.toString()
            type == Int::class.javaPrimitiveType || type == Integer::class.java ->
                (value as? Number)?.toInt() ?: value.toString().trim().toInt()
            type == Long::class.javaPrimitiveType || type == java.lang.Long::class.java ->
                (value as? Number)?.toLong() ?: value.toString().trim().toLong()
            type == Double::class.javaPrimitiveType || type == java.lang.Double::class.java ->
                (value as? Number)?.toDouble() ?: value.toString().trim().toDouble()
            type == Boolean::class.javaPrimitiveType || type == java.lang.Boolean::class.java ->
                (value as? Boolean) ?: value.toString().trim().toBoolean()
            else -> value
        }
    }
}
