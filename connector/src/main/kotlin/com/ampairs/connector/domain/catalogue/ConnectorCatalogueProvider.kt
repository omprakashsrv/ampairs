package com.ampairs.connector.domain.catalogue

/**
 * SPI implemented (one per connector type) to contribute a [CatalogueConnector] to the catalogue.
 * Mirrors the `setting` module's `SettingDefinitionProvider` pattern — connectors ship with code.
 */
interface ConnectorCatalogueProvider {
    fun connector(): CatalogueConnector
}
