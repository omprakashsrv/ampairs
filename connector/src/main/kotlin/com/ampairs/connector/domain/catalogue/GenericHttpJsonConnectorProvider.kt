package com.ampairs.connector.domain.catalogue

import org.springframework.stereotype.Component

/**
 * Generic HTTP/JSON — the first SERVER-SIDE connector. The sync engine runs on the backend
 * ([com.ampairs.connector.service.GenericHttpJsonExecutor]): on a schedule it GETs a configurable REST
 * endpoint per entity, applies the installation's mapping, and writes via the connector sparse-upsert
 * path. It needs no vendor SDK and is the template every future vendor provider (Zoho/Shopify/
 * QuickBooks) copies — those add OAuth + pagination on top of this same shape.
 *
 * Config keys the executor reads (set through the standard config UI): base_url, api_key (secret),
 * auth_header, auth_scheme, path.{entity}, records_path.{entity}, since_param.{entity}.
 */
@Component
class GenericHttpJsonConnectorProvider : ConnectorCatalogueProvider {
    override fun connector(): CatalogueConnector = CatalogueConnector(
        type = "generic_http_json",
        displayName = "Generic HTTP / JSON",
        description = "Server-side connector that pulls customers and products from any REST/JSON endpoint using an API key. A template for building vendor-specific connectors.",
        hostingType = HostingType.SERVER_SIDE,
        supportedEntities = listOf("customer", "product"),
        supportedDirections = listOf(SyncDirection.INBOUND),
        connectionSchema = listOf(
            ConnectionField(key = "base_url", label = "Base URL", secret = false, required = true),
            ConnectionField(key = "api_key", label = "API Key", secret = true, required = true),
            ConnectionField(key = "auth_header", label = "Auth Header Name", secret = false, required = false),
            ConnectionField(key = "auth_scheme", label = "Auth Scheme (e.g. Bearer)", secret = false, required = false),
            ConnectionField(key = "path.customer", label = "Customers Path", secret = false, required = false),
            ConnectionField(key = "path.product", label = "Products Path", secret = false, required = false),
        ),
        defaultMapping = listOf(
            DefaultEntityMapping(
                entityType = "customer",
                rules = listOf(
                    DefaultMappingRule("id", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("email", "email"),
                    DefaultMappingRule("phone", "phone"),
                ),
            ),
            DefaultEntityMapping(
                entityType = "product",
                rules = listOf(
                    DefaultMappingRule("id", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("sku", "sku"),
                ),
            ),
        ),
        requiredModule = null,
        requiredTier = null,
        multipleInstancesAllowed = true,
    )
}
