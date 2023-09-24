package com.ampairs.core.domain.model

class AppClient : BaseDomain() {
    private val clientId: String? = null
    private val clientSecret: String? = null
    private val app: com.ampairs.core.domain.enums.App? = null
    private val clientType: com.ampairs.core.domain.enums.ClientType? = null
    private val whiteListings: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.APP_CLIENT_PREFIX
    }
}
