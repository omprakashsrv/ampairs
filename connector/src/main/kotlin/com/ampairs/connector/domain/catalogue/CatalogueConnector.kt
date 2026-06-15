package com.ampairs.connector.domain.catalogue

/** Where a connector's push/pull executes. */
enum class HostingType { CLIENT_SIDE, SERVER_SIDE }

/** Sync direction a connector supports. First release ships INBOUND only. */
enum class SyncDirection { INBOUND, OUTBOUND }

/** A single connection-detail field the install must supply (e.g. host, port, api_key). */
data class ConnectionField(
    val key: String,
    val label: String,
    val secret: Boolean = false,
    val required: Boolean = true,
)

/** One default field-mapping rule for an entity (external field → Ampairs column). */
data class DefaultMappingRule(
    val externalField: String,
    val ampairsField: String,
    val transform: String? = null,
)

/** Default mapping template for one entity type, shipped with the connector. */
data class DefaultEntityMapping(
    val entityType: String,
    val rules: List<DefaultMappingRule>,
)

/**
 * Code-defined catalogue definition of an installable connector (Tally, Zoho, ...). This is NOT a
 * tenant table — connectors ship with the build via [ConnectorCatalogueProvider]. Only per-workspace
 * state (installations/config/mappings/checkpoints/runs) is persisted.
 */
data class CatalogueConnector(
    val type: String,
    val displayName: String,
    val description: String,
    val hostingType: HostingType,
    val supportedEntities: List<String>,
    val supportedDirections: List<SyncDirection> = listOf(SyncDirection.INBOUND),
    val connectionSchema: List<ConnectionField> = emptyList(),
    val defaultMapping: List<DefaultEntityMapping> = emptyList(),
    /** Workspace module code that must be installed for this connector to appear (null = always). */
    val requiredModule: String? = null,
    /** Subscription tier required, by tier code; null = available to all tiers. */
    val requiredTier: String? = null,
    val multipleInstancesAllowed: Boolean = false,
)
