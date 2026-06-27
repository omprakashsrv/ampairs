package com.ampairs.communication.config

/**
 * Module constants. UID prefixes are used only for server-minted rows; client (mobile) rows keep
 * their own app-generated uid on the `/sync` path.
 */
interface Constants {
    companion object {
        const val BASE_PATH = "/communication/v1"

        const val TEMPLATE_PREFIX = "CTPL"
        const val TEMPLATE_VARIANT_PREFIX = "CTPV"
        const val REQUEST_PREFIX = "CREQ"
        const val LOG_PREFIX = "CLOG"
        const val SCHEDULE_PREFIX = "CSCH"
        const val OCCURRENCE_PREFIX = "COCC"
        const val CAMPAIGN_PREFIX = "CCMP"
        const val PREFERENCE_PREFIX = "CPRF"
        const val SUPPRESSION_PREFIX = "CSUP"
        const val CONFIG_PREFIX = "CCFG"
        const val BINDING_PREFIX = "CETB"
        const val USAGE_PREFIX = "CUSG"

        const val SOURCE_MODULE = "communication"
    }
}
