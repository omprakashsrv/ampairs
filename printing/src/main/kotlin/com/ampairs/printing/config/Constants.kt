package com.ampairs.printing.config

interface Constants {
    companion object {
        /** Seq-id prefix for server-minted print templates (client rows keep their own uid). */
        const val PRINT_TEMPLATE_PREFIX = "PTM"
    }
}
