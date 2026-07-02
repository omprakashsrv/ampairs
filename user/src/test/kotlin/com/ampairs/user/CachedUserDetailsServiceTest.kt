package com.ampairs.user

import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import com.ampairs.user.service.CachedUserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachedUserDetailsServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var service: CachedUserDetailsService

    @BeforeEach
    fun setUp() {
        service = CachedUserDetailsService(userRepository)
    }

    @Test
    fun `loadUserByUsername returns the user details`() {
        val u = User().apply { uid = "USR-1"; userName = "alice"; firstName = "Alice" }
        whenever(userRepository.findByUserName("alice")).thenReturn(Optional.of(u))

        val result = service.loadUserByUsername("alice")

        assertEquals("alice", result.username)
    }

    @Test
    fun `loadUserByUsername throws when user is missing`() {
        whenever(userRepository.findByUserName("ghost")).thenReturn(Optional.empty())

        assertThrows(UsernameNotFoundException::class.java) { service.loadUserByUsername("ghost") }
    }
}
