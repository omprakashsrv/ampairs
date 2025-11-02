package com.ampairs.auth.model

import com.ampairs.auth.model.enums.TokenType
import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*

@Entity
@Table(name = "auth_token")
class Token : BaseDomain() {

    @Column(name = "token", nullable = false)
    var token: String = ""

    @Column(name = "expired", nullable = false)
    var expired: Boolean = false

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false

    @Column(name = "user_id", nullable = false)
    var userId: String = ""

    @Column(name = "device_id")
    var deviceId: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    var tokenType = TokenType.BEARER

    override fun obtainSeqIdPrefix(): String {
        return Constants.TOKEN_PREFIX
    }
}
