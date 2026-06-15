package com.ampairs.connector.domain.catalogue

import com.ampairs.core.exception.NotFoundException
import org.springframework.stereotype.Component

/**
 * Aggregates all [ConnectorCatalogueProvider] beans into a single, type-keyed catalogue. Spring
 * injects every provider on the classpath, so adding a new connector is just a new provider bean.
 */
@Component
class ConnectorCatalogueRegistry(
    providers: List<ConnectorCatalogueProvider>,
) {
    private val byType: Map<String, CatalogueConnector> =
        providers.associate { it.connector().type to it.connector() }

    fun all(): List<CatalogueConnector> = byType.values.sortedBy { it.displayName }

    fun find(type: String): CatalogueConnector? = byType[type]

    fun require(type: String): CatalogueConnector =
        byType[type] ?: throw NotFoundException("Unknown connector type: $type")
}
