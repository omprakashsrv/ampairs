package com.ampairs.auth.model

import com.ampairs.core.domain.enums.VerificationStatus
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.util.*

private const val LOGIN_SESSION_ID = "LSQ"

@Entity
@Table(name = "login_session")
class LoginSession : BaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "user_agent", nullable = false, length = 255)
    var userAgent: String = ""

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_time", nullable = false)
    var expiresAt: Date? = null

    @Column(name = "code")
    var code: String = ""

    @Column(name = "attempts")
    var attempts = 0

    @Column(name = "verified")
    var verified: Boolean = false

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "verified_at")
    var verifiedAt: Date? = null

    @Column(name = "expired")
    var expired: Boolean = false

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    var status = VerificationStatus.NEW

    override fun obtainSeqIdPrefix(): String {
        return LOGIN_SESSION_ID
    }

    fun isExpired(): Boolean {
        return expiresAt?.before(Date()) ?: true
    }

    fun userName(): String {
        return countryCode.toString() + phone
    }
}
