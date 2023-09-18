package com.ampairs.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "sms_verification")
class SmsVerification : AbstractIdVerification() {
    @Column(name = "country_code")
    var countryCode: Int? = null

    @Column(name = "phone")
    var phone: String? = null

    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.SMS_VERIFICATION_PREFIX
    }
}
