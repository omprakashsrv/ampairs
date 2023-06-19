package com.ampairs.auth.domain.model

import com.ampairs.auth.config.Constants
import com.ampairs.auth.domain.enums.App
import com.ampairs.auth.domain.enums.ClientType
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class AppClient : BaseDomain() {
    private val clientId: String? = null
    private val clientSecret: String? = null
    private val app: App? = null
    private val clientType: ClientType? = null
    private val whiteListings: String? = null
    override fun obtainIdPrefix(): String? {
        return Constants.APP_CLIENT_PREFIX
    }
}
