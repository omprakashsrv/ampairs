package com.ampairs.user.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import com.ampairs.core.domain.User as CoreUser


@Entity
@Table(name = "app_user")
class User : BaseDomain(), UserDetails, CoreUser {
    @Column(name = "country_code", nullable = false)
    var countryCode: Int = 91

    @Column(name = "phone", nullable = false, length = 20)  // Increased for international numbers
    override var phone: String = ""

    @Column(name = "email", length = 320)  // RFC compliant email length
    override var email: String? = null  // Email should be nullable

    @Column(name = "user_name", nullable = false, length = 200, unique = true)  // Added unique constraint
    var userName: String = ""

    @Column(name = "user_password", nullable = true)  // Password should be nullable
    var userPassword: String? = null

    @Column(name = "first_name", nullable = false, length = 100)
    override var firstName: String = ""

    @Column(name = "last_name", nullable = true, length = 100)
    override var lastName: String? = ""

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    // Core User interface implementation
    override val isActive: Boolean
        get() = active

    override val profilePictureUrl: String?
        get() = null // Can be implemented when profile pictures are added

    // UserDetails implementation
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
        return active
    }

    /**
     * Get display name for the user
     */
    override fun getDisplayName(): String {
        return when {
            firstName.isNotBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName!!
            !email.isNullOrBlank() -> email!!
            else -> uid
        }
    }

    /**
     * Get full name
     */
    override fun getFullName(): String {
        return when {
            firstName.isNotBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName!!
            else -> ""
        }
    }

    override fun obtainSeqIdPrefix(): String {
        return Constants.USER_ID_PREFIX
    }
}
