package com.ampairs.auth.domain.model

import com.ampairs.auth.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import lombok.NoArgsConstructor

@Entity
@NoArgsConstructor
class SmsVerification : AbstractIdVerification() {
    @Column(name = "country_code")
    var countryCode: Int? = null

    @Column(name = "phone")
    var phone: String? = null

    override fun obtainIdPrefix(): String {
        return Constants.SMS_VERIFICATION_PREFIX
    }
}
