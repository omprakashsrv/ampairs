package com.ampairs.auth.model

import com.ampairs.core.domain.enums.VerificationStatus
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDateTime

private const val LOGIN_SESSION_ID = "LGS"

@Entity(name = "login_session")
class LoginSession : BaseDomain() {

    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "user_agent", nullable = false, length = 255)
    var userAgent: String = ""

    @Column(name = "expiry_time", nullable = false)
    val expiryTime: LocalDateTime = LocalDateTime.now().plusMinutes(15)

    @Column(name = "code")
    var code: String = ""

    @Column(name = "attempts")
    var attempts = 0

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    var status = VerificationStatus.NEW

    override fun obtainSeqIdPrefix(): String {
        return LOGIN_SESSION_ID
    }

    fun expired(): Boolean {
        return LocalDateTime.now().isBefore(expiryTime)
    }

    fun userName(): String {
        return countryCode.toString() + phone
    }
}
