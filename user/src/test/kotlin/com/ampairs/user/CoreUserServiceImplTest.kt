package com.ampairs.user

import com.ampairs.user.model.User
import com.ampairs.user.service.CoreUserServiceImpl
import com.ampairs.user.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreUserServiceImplTest {

    @Mock
    private lateinit var authUserService: UserService

    private lateinit var service: CoreUserServiceImpl

    private fun user(uid: String = "USR-1"): User =
        User().apply {
            this.uid = uid
            userName = "alice"
            firstName = "Alice"
            lastName = "Smith"
            phone = "9999999999"
        }

    @BeforeEach
    fun setUp() {
        service = CoreUserServiceImpl(authUserService)
    }

    @Test
    fun `getUserById returns the user`() {
        val u = user()
        whenever(authUserService.getUser("USR-1")).thenReturn(u)
        assertSame(u, service.getUserById("USR-1"))
    }

    @Test
    fun `getUserById returns null when lookup fails`() {
        whenever(authUserService.getUser("missing")).thenThrow(IllegalArgumentException("nope"))
        assertNull(service.getUserById("missing"))
    }

    @Test
    fun `getUsersByIds skips ids that cannot be resolved`() {
        val u1 = user("USR-1")
        whenever(authUserService.getUser("USR-1")).thenReturn(u1)
        whenever(authUserService.getUser("USR-2")).thenThrow(IllegalArgumentException("nope"))

        val result = service.getUsersByIds(listOf("USR-1", "USR-2"))

        assertEquals(1, result.size)
        assertSame(u1, result["USR-1"])
    }

    @Test
    fun `getCurrentUser returns the session user`() {
        val u = user()
        whenever(authUserService.getSessionUser()).thenReturn(u)
        assertSame(u, service.getCurrentUser())
    }

    @Test
    fun `getCurrentUser returns null on error`() {
        whenever(authUserService.getSessionUser()).thenThrow(IllegalStateException("no auth"))
        assertNull(service.getCurrentUser())
    }

    @Test
    fun `userExists is true when user is found`() {
        whenever(authUserService.getUser("USR-1")).thenReturn(user())
        assertTrue(service.userExists("USR-1"))
    }

    @Test
    fun `userExists is false when lookup fails`() {
        whenever(authUserService.getUser("missing")).thenThrow(IllegalArgumentException("nope"))
        assertFalse(service.userExists("missing"))
    }

    @Test
    fun `getCurrentUserId returns the session user uid`() {
        whenever(authUserService.getSessionUser()).thenReturn(user("USR-9"))
        assertEquals("USR-9", service.getCurrentUserId())
    }

    @Test
    fun `getCurrentUserId returns null on error`() {
        whenever(authUserService.getSessionUser()).thenThrow(IllegalStateException("no auth"))
        assertNull(service.getCurrentUserId())
    }
}
