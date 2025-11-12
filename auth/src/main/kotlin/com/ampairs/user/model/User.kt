package com.ampairs.user.model

import com.ampairs.auth.service.UserDetailsWithId
import com.ampairs.auth.service.UserDetailsWithRoles
import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import com.ampairs.core.domain.User as CoreUser
import java.time.Instant


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

    @Column(name = "firebase_uid", length = 128)
    var firebaseUid: String? = null

    /**
     * Whether this user account has been marked for deletion
     */
    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

    /**
     * When the account deletion was requested
     */
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    /**
     * When the account will be permanently deleted (30 days after deletion request)
     */
    @Column(name = "deletion_scheduled_for")
    var deletionScheduledFor: Instant? = null

    /**
     * Reason provided for account deletion
     */
    @Column(name = "deletion_reason", length = 500)
    var deletionReason: String? = null

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

    /**
     * Check if account is marked for deletion
     */
    fun isDeleted(): Boolean = deleted

    /**
     * Check if account is within deletion grace period (can be restored)
     */
    fun canRestoreAccount(): Boolean {
        return deleted && deletionScheduledFor?.isAfter(Instant.now()) == true
    }

    /**
     * Check if account is past grace period and ready for permanent deletion
     */
    fun isReadyForPermanentDeletion(): Boolean {
        return deleted && deletionScheduledFor?.isBefore(Instant.now()) == true
    }

    /**
     * Mark account for deletion with 30-day grace period
     * Note: Account remains active during grace period so user can cancel deletion
     */
    fun markForDeletion(reason: String? = null) {
        deleted = true
        deletedAt = Instant.now()
        deletionScheduledFor = Instant.now().plusSeconds(30L * 24 * 60 * 60) // 30 days
        deletionReason = reason
        // Keep active=true during grace period so user can cancel
        // Will be set to false during permanent deletion
    }

    /**
     * Restore account from deletion (only during grace period)
     */
    fun restoreAccount() {
        if (canRestoreAccount()) {
            deleted = false
            deletedAt = null
            deletionScheduledFor = null
            deletionReason = null
            active = true
        } else {
            throw IllegalStateException("Account cannot be restored - grace period expired")
        }
    }

    /**
     * Anonymize user data for soft delete
     */
    fun anonymize() {
        firstName = "Deleted"
        lastName = "User"
        email = null
        phone = "0000000000"
        userName = "deleted_${uid}"
        userPassword = null
        firebaseUid = null
    }
}
