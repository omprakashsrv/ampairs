package com.ampairs.core.domain.model

import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


@Entity()
class User : BaseDomain(), UserDetails {
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

    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(100) default ''")
    var firstName = ""

    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(100) default ''")
    var lastName = ""

    @Column(name = "active", nullable = false)
    var active = true


    override fun obtainIdPrefix(): String {
        return com.ampairs.core.config.Constants.USER_ID_PREFIX
    }

    val fullName: String
        get() = "$firstName $lastName"

    @ElementCollection
    override fun getAuthorities(): List<SimpleGrantedAuthority> {
        return listOf(SimpleGrantedAuthority(com.ampairs.core.domain.enums.Role.ADMIN.name))
    }

    override fun getPassword(): String {
        return userPassword
    }

    override fun getUsername(): String {
        return userName
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}
