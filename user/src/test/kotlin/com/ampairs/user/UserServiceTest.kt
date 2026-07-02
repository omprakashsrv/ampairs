package com.ampairs.user

import com.ampairs.user.model.User
import com.ampairs.user.model.dto.UserUpdateRequest
import com.ampairs.user.repository.UserRepository
import com.ampairs.user.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var service: UserService

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

    @BeforeEach
    fun setUp() {
        service = UserService(userRepository)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticateAs(principal: Any) {
        val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `createUser saves when username is new`() {
        val u = user()
        whenever(userRepository.existsByUserName("alice")).thenReturn(false)

        val result = service.createUser(u)

        assertSame(u, result)
        verify(userRepository).save(u)
    }

    @Test
    fun `createUser rejects a duplicate username`() {
        val u = user()
        whenever(userRepository.existsByUserName("alice")).thenReturn(true)

        val ex = assertThrows(IllegalArgumentException::class.java) { service.createUser(u) }
        assertTrue(ex.message!!.contains("alice"))
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `updateUser applies new name from session user`() {
        val sessionUser = user()
        authenticateAs(sessionUser)

        val result = service.updateUser(UserUpdateRequest(firstName = "Alicia", lastName = "Jones"))

        assertEquals("Alicia", result.firstName)
        assertEquals("Jones", result.lastName)
        verify(userRepository).save(sessionUser)
    }

    @Test
    fun `getSessionUser returns the User principal`() {
        val sessionUser = user()
        authenticateAs(sessionUser)

        assertSame(sessionUser, service.getSessionUser())
    }

    @Test
    fun `getSessionUser throws when no authentication present`() {
        SecurityContextHolder.clearContext()
        assertThrows(IllegalStateException::class.java) { service.getSessionUser() }
    }

    @Test
    fun `getSessionUser throws when principal is not a User`() {
        authenticateAs("not-a-user")
        assertThrows(IllegalStateException::class.java) { service.getSessionUser() }
    }

    @Test
    fun `getUser returns the found user`() {
        val u = user()
        whenever(userRepository.findByUid("USR-1")).thenReturn(Optional.of(u))

        assertSame(u, service.getUser("USR-1"))
    }

    @Test
    fun `getUser throws when not found`() {
        whenever(userRepository.findByUid("missing")).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) { service.getUser("missing") }
    }

    @Test
    fun `getUsers returns empty list for empty input without hitting repo`() {
        val result = service.getUsers(emptyList())
        assertTrue(result.isEmpty())
        verify(userRepository, never()).findByUidIn(any())
    }

    @Test
    fun `getUsers delegates to repository for non-empty ids`() {
        val u = user()
        whenever(userRepository.findByUidIn(listOf("USR-1"))).thenReturn(listOf(u))

        val result = service.getUsers(listOf("USR-1"))

        assertEquals(1, result.size)
        assertSame(u, result.first())
    }
}
