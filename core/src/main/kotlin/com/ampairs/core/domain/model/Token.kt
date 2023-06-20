package com.ampairs.core.domain.model

import jakarta.persistence.*
import lombok.NoArgsConstructor

@NoArgsConstructor
@Entity()
class Token : com.ampairs.core.domain.model.BaseDomain() {

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
    var tokenType = com.ampairs.core.domain.enums.TokenType.BEARER

    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.SMS_VERIFICATION_PREFIX
    }
}
