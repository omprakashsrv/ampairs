package com.ampairs.tally.model.domain.user

import com.ampairs.tally.model.domain.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity


@Entity(name = "app_user")
class User : BaseDomain() {
    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = ""

    @Column(name = "email", length = 255)
    var email: String = ""

    @Column(name = "user_name", nullable = false, length = 200)
    var userName: String = ""

    @Column(name = "user_password")
    var userPassword: String = ""

    @Column(name = "first_name", nullable = false)
    var firstName = ""

    @Column(name = "last_name", nullable = false)
    var lastName = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    val fullName: String
        get() = "$firstName $lastName"
}
