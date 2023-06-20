package com.ampairs.auth.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import lombok.Getter
import lombok.Setter

@Entity()
@Getter
@Setter
class EmailVerification : com.ampairs.auth.domain.model.AbstractIdVerification() {
    @Column(name = "email")
    var email: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.auth.config.Constants.EMAIL_VERIFICATION_PREFIX
    }
}
