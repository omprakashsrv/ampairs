package com.ampairs.connector.exception

import com.ampairs.core.exception.BusinessException

/**
 * Connector domain errors. `BusinessException` is final in core, so these are factory functions that
 * return a typed [BusinessException] (with a stable error code) for the global handler to map.
 */
object ConnectorErrors {
    /** A connector of this type is already installed (and multiple instances are not allowed). */
    fun alreadyInstalled(connectorType: String) =
        BusinessException("CONNECTOR_ALREADY_INSTALLED", "Connector '$connectorType' is already installed for this workspace")

    /** A mapping rule targets a non-existent Ampairs field or violates type expectations. */
    fun invalidMapping(message: String) =
        BusinessException("CONNECTOR_INVALID_MAPPING", message)

    /** Connection configuration failed validation. */
    fun configInvalid(message: String) =
        BusinessException("CONNECTOR_CONFIG_INVALID", message)
}
