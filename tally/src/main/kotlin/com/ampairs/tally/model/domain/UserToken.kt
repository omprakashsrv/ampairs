package com.ampairs.tally.model.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "user_token")
class UserToken : BaseDomain() {
    @Column(name = "user_id", length = 200, updatable = false, nullable = false)
    var userId: String = ""

    @Column(name = "token", nullable = false)
    var token: String = ""
}