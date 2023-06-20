package com.ampairs.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import lombok.Getter
import lombok.Setter

@Entity()
@Getter
@Setter
class EmailVerification : com.ampairs.core.domain.model.AbstractIdVerification() {
    @Column(name = "email")
    var email: String? = null
    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.EMAIL_VERIFICATION_PREFIX
    }
}
