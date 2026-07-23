package com.ampairs.pricing.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Small Jackson helper for the JSON-string columns (tiers, attribute predicates, geo-zone members).
 * Kept self-contained so entity ↔ DTO mapping never depends on a Hibernate JSON type.
 */
object PricingJson {
    val mapper: ObjectMapper = jacksonObjectMapper()

    inline fun <reified T> read(json: String?, default: T): T =
        if (json.isNullOrBlank()) default else mapper.readValue(json)

    fun write(value: Any?): String? = value?.let { mapper.writeValueAsString(it) }
}
