package com.ampairs.connector.domain.catalogue

import org.springframework.stereotype.Component

/**
 * Tally — the first connector. CLIENT_SIDE: push/pull executes in the Ampairs desktop app talking to
 * a local Tally instance; the backend stores config/mapping/checkpoints/runs and receives mapped data
 * via the connector sparse-upsert endpoint. INBOUND only in the first release.
 */
@Component
class TallyConnectorProvider : ConnectorCatalogueProvider {
    override fun connector(): CatalogueConnector = CatalogueConnector(
        type = "tally",
        displayName = "Tally",
        description = "Sync customers, products, units and groups from a local Tally ERP instance.",
        hostingType = HostingType.CLIENT_SIDE,
        supportedEntities = listOf(
            "customer", "customer_group", "product", "product_catalog", "unit", "stock_balance",
        ),
        supportedDirections = listOf(SyncDirection.INBOUND),
        connectionSchema = listOf(
            ConnectionField(key = "host", label = "Tally Host", secret = false, required = true),
            ConnectionField(key = "port", label = "Tally Port", secret = false, required = true),
        ),
        defaultMapping = listOf(
            DefaultEntityMapping(
                entityType = "customer",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("ledgerPhone", "phone", transform = "sanitize_phone"),
                    DefaultMappingRule("partyGstin", "gstin", transform = "validate_gstin"),
                ),
            ),
            DefaultEntityMapping(
                entityType = "product",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("hsnCode", "hsnCode"),
                ),
            ),
        ),
        requiredModule = null,
        // Tally entitlement reuses the existing subscription addon; tier gating wired in a later task.
        requiredTier = null,
        multipleInstancesAllowed = false,
    )
}
