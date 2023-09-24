package com.ampairs.core.domain.model

import com.ampairs.core.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity(name = "token")
class Token : BaseDomain() {

    @Column(name = "token", nullable = false)
    var token: String = ""

    @Column(name = "expired", nullable = false)
    var expired: Boolean = false

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false

    @Column(name = "user_id", nullable = false)
    var userId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    var tokenType = com.ampairs.core.domain.enums.TokenType.BEARER

    override fun obtainIdPrefix(): String {
        return Constants.TOKEN_PREFIX
    }
}
