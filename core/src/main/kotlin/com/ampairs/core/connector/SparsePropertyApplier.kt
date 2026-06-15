package com.ampairs.core.connector

/**
 * Applies a sparse map of `propertyName → value` onto a mutable entity via JVM setter reflection
 * (`var name` → `setName`). Used by [ConnectorEntityWriter] implementations to write only the columns
 * present in a connector row. Returns the property names actually applied.
 *
 * - Only keys with a matching single-arg setter are applied; unknown keys are ignored by the caller's
 *   allowlist already, but extra safety here.
 * - Values arrive from JSON (String/Number/Boolean/null) and are coerced to the setter's parameter
 *   type. A null targeting a non-null Kotlin property is skipped (cannot clear a non-null column).
 */
object SparsePropertyApplier {

    fun apply(target: Any, columns: Map<String, Any?>): List<String> {
        val applied = mutableListOf<String>()
        for ((name, value) in columns) {
            val setter = target.javaClass.methods.firstOrNull {
                it.name == setterName(name) && it.parameterCount == 1
            } ?: continue
            val paramType = setter.parameterTypes[0]
            val coerced = runCatching { coerce(value, paramType) }.getOrNull()
            if (value != null && coerced == null && !paramType.isAssignableFrom(value.javaClass)) {
                // Could not coerce — skip rather than corrupt.
                continue
            }
            runCatching { setter.invoke(target, coerced) }.onSuccess { applied += name }
        }
        return applied
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
