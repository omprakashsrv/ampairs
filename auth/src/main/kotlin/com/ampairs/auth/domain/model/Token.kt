package com.ampairs.auth.domain.model

import com.ampairs.auth.config.Constants
import com.ampairs.auth.domain.enums.TokenType
import jakarta.persistence.*
import lombok.NoArgsConstructor

@NoArgsConstructor
@Entity()
class Token : BaseDomain() {

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
    var tokenType = TokenType.BEARER

    override fun obtainIdPrefix(): String {
        return Constants.SMS_VERIFICATION_PREFIX
    }
}
