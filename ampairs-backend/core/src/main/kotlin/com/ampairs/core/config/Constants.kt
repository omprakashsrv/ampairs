package com.ampairs.core.config

interface Constants {
    companion object {
        const val ID_LENGTH = 34

        const val USER_ID_PREFIX = "UID"
        const val SMS_VERIFICATION_PREFIX = "SVR"
        const val EMAIL_VERIFICATION_PREFIX = "EVR"
        const val APP_CLIENT_PREFIX = "APC"
        const val WORKSPACE_PREFIX = "WSP"
        const val USER_WORKSPACE_PREFIX = "UWS"
        const val WORKSPACE_MEMBER_PREFIX = "WMB"
        const val WORKSPACE_INVITATION_PREFIX = "WIV"
        const val WORKSPACE_SETTINGS_PREFIX = "WST"
        const val WORKSPACE_TEAM_PREFIX = "WTM"
        const val TOKEN_PREFIX = "JWT"
        const val DEVICE_SESSION_PREFIX = "DSS"
        const val FILE_ID_PREFIX = "FIL"

        // SeqId Prefixes
        const val USER_SEQ_PREFIX = "USQ"
        const val WORKSPACE_SEQ_PREFIX = "WSQ"
        const val USER_WORKSPACE_SEQ_PREFIX = "UWQ"
        const val TOKEN_SEQ_PREFIX = "JWQ"
        const val FILE_SEQ_PREFIX = "FLQ"
        const val CUSTOMER_SEQ_PREFIX = "CSQ"
        const val PRODUCT_SEQ_PREFIX = "PRQ"
        const val ORDER_SEQ_PREFIX = "ORQ"
        const val INVOICE_SEQ_PREFIX = "INQ"

    }
}
