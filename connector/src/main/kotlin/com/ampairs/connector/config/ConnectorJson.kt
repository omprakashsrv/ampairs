package com.ampairs.connector.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Self-contained Jackson mapper for the connector module's INTERNAL JSON columns (config values,
 * mapping rules). Deliberately NOT the Spring-managed `ObjectMapper` bean: some sliced test contexts
 * scan connector @Service beans without Jackson auto-configuration, so injecting the bean made those
 * contexts fail to load. This mapper is read/write-consistent for storage, which is all that matters
 * here; API DTOs are still serialized by Spring's global (snake_case) mapper at the controller layer.
 */
object ConnectorJson {
    val mapper: ObjectMapper = jacksonObjectMapper()
}
