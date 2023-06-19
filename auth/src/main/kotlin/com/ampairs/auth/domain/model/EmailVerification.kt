package com.ampairs.auth.domain.model

import com.ampairs.auth.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import lombok.Getter
import lombok.Setter

@Entity()
@Getter
@Setter
class EmailVerification : AbstractIdVerification() {
    @Column(name = "email")
    var email: String? = null
    override fun obtainIdPrefix(): String {
        return Constants.EMAIL_VERIFICATION_PREFIX
    }
}
