package com.ampairs.auth.model

import com.ampairs.auth.model.enums.App
import com.ampairs.auth.model.enums.ClientType
import com.ampairs.core.domain.model.BaseDomain

class AppClient : BaseDomain() {
    private val clientId: String? = null
    private val clientSecret: String? = null
    private val app: App? = null
    private val clientType: ClientType? = null
    private val whiteListings: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.APP_CLIENT_PREFIX
    }
}
