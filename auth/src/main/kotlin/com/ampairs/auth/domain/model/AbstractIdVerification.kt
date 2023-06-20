package com.ampairs.auth.domain.model

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import java.sql.Timestamp

@MappedSuperclass
abstract class AbstractIdVerification : com.ampairs.auth.domain.model.BaseDomain() {
    @Column(name = "code")
    var code: String = ""

    @Column(name = "valid_till")
    var validTill: Timestamp? = null

    @Column(name = "attempts")
    var attempts = 0

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    var status = com.ampairs.auth.domain.enums.VerificationStatus.NEW

    @Column(name = "user_id")
    var userId: String = ""


}
