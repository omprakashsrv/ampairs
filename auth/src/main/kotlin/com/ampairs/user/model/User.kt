package com.ampairs.user.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


@Entity
@Table(name = "app_user")
class User : BaseDomain(), UserDetails {
    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "phone", nullable = false, length = 20)  // Increased for international numbers
    var phone: String = ""

    @Column(name = "email", length = 320)  // RFC compliant email length
    var email: String? = null  // Email should be nullable

    @Column(name = "user_name", nullable = false, length = 200, unique = true)  // Added unique constraint
    var userName: String = ""

    @Column(name = "user_password", nullable = true)  // Password should be nullable
    var userPassword: String? = null

    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(100) default ''")
    var firstName = ""

    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(100) default ''")
    var lastName: String? = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String {
        return Constants.USER_ID_PREFIX
    }

    val fullName: String
        get() = "$firstName $lastName"

    @ElementCollection
    override fun getAuthorities(): List<SimpleGrantedAuthority> {
        return listOf()
    }

    override fun getPassword(): String? {
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
