package com.ampairs.auth.domain.model

import jakarta.persistence.*
import lombok.NoArgsConstructor

@NoArgsConstructor
@Entity()
class Token : com.ampairs.auth.domain.model.BaseDomain() {

    @Column(name = "token", nullable = false)
    var token: String = ""

    @Column(name = "expired",nullable = false)
    var expired: Boolean = false

    @Column(name = "revoked",nullable = false)
    var revoked: Boolean = false

    @Column(name = "user_id",nullable = false)
    var userId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    var tokenType = com.ampairs.auth.domain.enums.TokenType.BEARER

    override fun obtainIdPrefix(): String {
        return com.ampairs.auth.config.Constants.SMS_VERIFICATION_PREFIX
    }
}
