package com.ampairs.connector.domain.dto

import com.ampairs.connector.domain.catalogue.CatalogueConnector

/** Read-only catalogue entry shown to the workspace admin (FR-001). */
data class CatalogueConnectorResponse(
    val type: String,
    val displayName: String,
    val description: String,
    val hostingType: String,
    val supportedEntities: List<String>,
    val supportedDirections: List<String>,
    val connectionSchema: List<ConnectionFieldResponse>,
    val multipleInstancesAllowed: Boolean,
    /** Whether the current workspace already has an active installation of this connector. */
    val installed: Boolean,
)

data class ConnectionFieldResponse(
    val key: String,
    val label: String,
    val secret: Boolean,
    val required: Boolean,
)

fun CatalogueConnector.asResponse(installed: Boolean): CatalogueConnectorResponse =
    CatalogueConnectorResponse(
        type = type,
        displayName = displayName,
        description = description,
        hostingType = hostingType.name,
        supportedEntities = supportedEntities,
        supportedDirections = supportedDirections.map { it.name },
        connectionSchema = connectionSchema.map {
            ConnectionFieldResponse(it.key, it.label, it.secret, it.required)
        },
        multipleInstancesAllowed = multipleInstancesAllowed,
        installed = installed,
    )
