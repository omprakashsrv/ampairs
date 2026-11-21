package com.ampairs.connector.config

object Constants {
    /** UID prefix for [com.ampairs.connector.domain.model.ConnectorInstallation]. */
    const val CONNECTOR_INSTALLATION_PREFIX = "CNI"
    const val CONNECTOR_CONFIG_PREFIX = "CNC"
    const val CONNECTOR_MAPPING_PREFIX = "CNM"
    const val CONNECTOR_CHECKPOINT_PREFIX = "CNK"
    const val CONNECTOR_RUN_PREFIX = "CNR"

    /** Mobile `SyncEntity` code used for real-time change broadcasts. */
    const val SYNC_ENTITY_CODE = "connector"

    /** Env var holding the symmetric key used to encrypt connector secrets at rest. */
    const val SECRET_KEY_ENV = "CONNECTOR_SECRET_KEY"
}
