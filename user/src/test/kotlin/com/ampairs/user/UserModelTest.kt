package com.ampairs.user

import com.ampairs.user.model.User
import com.ampairs.user.model.dto.toUserResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserModelTest {

    private fun user(block: User.() -> Unit = {}): User =
        User().apply {
            uid = "USR-1"
            userName = "alice"
            firstName = "Alice"
            lastName = "Smith"
            phone = "9999999999"
            countryCode = 91
            email = "alice@example.com"
            active = true
            block()
        }

    @Test
    fun `display name combines first and last name`() {
        assertEquals("Alice Smith", user().getDisplayName())
        assertEquals("Alice Smith", user().getFullName())
    }

    @Test
    fun `display name falls back through first name, last name, email then uid`() {
        assertEquals("Alice", user { lastName = null }.getDisplayName())
        assertEquals("Smith", user { firstName = "" }.getDisplayName())
        assertEquals("alice@example.com", user { firstName = ""; lastName = null }.getDisplayName())
        assertEquals("USR-1", user { firstName = ""; lastName = null; email = null }.getDisplayName())
    }

    @Test
    fun `full name returns empty when no names set`() {
        assertEquals("", user { firstName = ""; lastName = null }.getFullName())
    }

    @Test
    fun `UserDetails contract reflects active flag`() {
        val u = user()
        assertEquals("alice", u.username)
        assertTrue(u.isEnabled)
        assertTrue(u.isAccountNonExpired)
        assertTrue(u.isAccountNonLocked)
        assertTrue(u.isCredentialsNonExpired)
        assertTrue(u.authorities.isEmpty())

        val inactive = user { active = false }
        assertFalse(inactive.isEnabled)
        assertFalse(inactive.isActive)
    }

    @Test
    fun `markForDeletion sets grace period and keeps account restorable`() {
        val u = user()
        u.markForDeletion("not needed anymore")

        assertTrue(u.isDeleted())
        assertTrue(u.canRestoreAccount())
        assertFalse(u.isReadyForPermanentDeletion())
        assertEquals("not needed anymore", u.deletionReason)
        assertTrue(u.deletionScheduledFor!!.isAfter(Instant.now()))
    }

    @Test
    fun `account ready for permanent deletion once grace period elapsed`() {
        val u = user {
            deleted = true
            deletionScheduledFor = Instant.now().minusSeconds(60)
        }
        assertTrue(u.isReadyForPermanentDeletion())
        assertFalse(u.canRestoreAccount())
    }

    @Test
    fun `restoreAccount clears deletion state during grace period`() {
        val u = user()
        u.markForDeletion()
        u.restoreAccount()

        assertFalse(u.isDeleted())
        assertNull(u.deletedAt)
        assertNull(u.deletionScheduledFor)
        assertNull(u.deletionReason)
        assertTrue(u.active)
    }

    @Test
    fun `restoreAccount throws once grace period expired`() {
        val u = user {
            deleted = true
            deletionScheduledFor = Instant.now().minusSeconds(60)
        }
        assertThrows(IllegalStateException::class.java) { u.restoreAccount() }
    }

    @Test
    fun `anonymize scrubs identifying fields`() {
        val u = user()
        u.anonymize()

        assertEquals("Deleted", u.firstName)
        assertEquals("User", u.lastName)
        assertNull(u.email)
        assertEquals("0000000000", u.phone)
        assertEquals("deleted_USR-1", u.userName)
        assertNull(u.userPassword)
        assertNull(u.firebaseUid)
    }

    @Test
    fun `toUserResponse maps fields and full name`() {
        val u = user {
            profilePictureUrlField = "key/full.jpg"
            profilePictureThumbnailUrl = "key/thumb.jpg"
        }
        val resp = u.toUserResponse()

        assertEquals("USR-1", resp.id)
        assertEquals("Alice", resp.firstName)
        assertEquals("Smith", resp.lastName)
        assertEquals("alice", resp.userName)
        assertEquals(91, resp.countryCode)
        assertEquals("9999999999", resp.phone)
        assertEquals("alice@example.com", resp.email)
        assertEquals("Alice Smith", resp.fullName)
        assertTrue(resp.active)
        assertEquals("key/full.jpg", resp.profilePictureUrl)
        assertEquals("key/thumb.jpg", resp.profilePictureThumbnailUrl)
    }
}
