package com.ampairs.auth.domain.model

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class AppClient : com.ampairs.auth.domain.model.BaseDomain() {
    private val clientId: String? = null
    private val clientSecret: String? = null
    private val app: com.ampairs.auth.domain.enums.App? = null
    private val clientType: com.ampairs.auth.domain.enums.ClientType? = null
    private val whiteListings: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.auth.config.Constants.APP_CLIENT_PREFIX
    }
}
