package com.ampairs.connector.domain.catalogue

import org.springframework.stereotype.Component

/**
 * Tally — the first connector. CLIENT_SIDE: push/pull executes in the Ampairs desktop app talking to
 * a local Tally instance; the backend stores config/mapping/checkpoints/runs and receives mapped data
 * via the connector sparse-upsert endpoint. INBOUND only in the first release.
 *
 * The [defaultMapping] mirrors today's client-side `TallyCustomerMapper` / `TallyProductMapper`
 * (ampairs-app): external Tally fields → Ampairs entity property names. These are seeded as editable
 * mapping rows when the connector is installed (FR-011). `transform` names are hints applied on the
 * client when reading Tally, before the mapped value is pushed.
 */
@Component
class TallyConnectorProvider : ConnectorCatalogueProvider {
    override fun connector(): CatalogueConnector = CatalogueConnector(
        type = "tally",
        displayName = "Tally",
        description = "Sync customers, customer groups, products, product catalogue, units and stock balances from a local Tally ERP instance.",
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
            // Ledger → Customer (TallyCustomerMapper)
            DefaultEntityMapping(
                entityType = "customer",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("ledgerMobile", "phone", transform = "extract_mobile"),
                    DefaultMappingRule("ledgerPhone", "landline", transform = "clean_landline"),
                    DefaultMappingRule("gstin", "gstNumber", transform = "validate_gstin"),
                    DefaultMappingRule("mailingAddress", "address"),
                    DefaultMappingRule("addressLine1", "street"),
                    DefaultMappingRule("addressCity", "city"),
                    DefaultMappingRule("stateName", "state"),
                    DefaultMappingRule("pinCode", "pincode", transform = "validate_pincode"),
                    DefaultMappingRule("parent", "customerGroup"),
                ),
            ),
            // Account Group → CustomerGroup (TallyCustomerMapper)
            DefaultEntityMapping(
                entityType = "customer_group",
                rules = listOf(
                    DefaultMappingRule("groupName", "refId"),
                    DefaultMappingRule("groupName", "name"),
                    DefaultMappingRule("parent", "description"),
                ),
            ),
            // StockItem → Product (TallyProductMapper)
            DefaultEntityMapping(
                entityType = "product",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("standardPrice", "sellingPrice", transform = "parse_price"),
                    DefaultMappingRule("standardPrice", "mrp", transform = "parse_price"),
                    DefaultMappingRule("standardCost", "costPrice", transform = "parse_price"),
                    DefaultMappingRule("hsnCode", "taxCode", transform = "extract_hsn"),
                    DefaultMappingRule("baseUnits", "baseUnit"),
                    DefaultMappingRule("category", "category"),
                    DefaultMappingRule("parent", "group"),
                ),
            ),
            // StockGroup / StockCategory → product catalogue node (TallyProductMapper)
            DefaultEntityMapping(
                entityType = "product_catalog",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                ),
            ),
            // Tally Unit → Unit (TallyProductMapper)
            DefaultEntityMapping(
                entityType = "unit",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("name", "name"),
                    DefaultMappingRule("symbol", "shortName"),
                ),
            ),
            // Closing balance → Product inventory quantity
            DefaultEntityMapping(
                entityType = "stock_balance",
                rules = listOf(
                    DefaultMappingRule("guid", "refId"),
                    DefaultMappingRule("closingBalance", "inventory", transform = "parse_quantity"),
                ),
            ),
        ),
        requiredModule = null,
        // Tally entitlement reuses the existing subscription addon; tier gating wired in a later task.
        requiredTier = null,
        multipleInstancesAllowed = false,
    )
}
