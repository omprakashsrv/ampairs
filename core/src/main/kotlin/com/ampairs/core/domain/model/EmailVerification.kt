package com.ampairs.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "email_verification")
class EmailVerification : AbstractIdVerification() {
    @Column(name = "email")
    var email: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.EMAIL_VERIFICATION_PREFIX
    }
}
