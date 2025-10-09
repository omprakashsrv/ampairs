package com.ampairs.user.service

import com.ampairs.user.repository.UserRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Cached implementation of UserDetailsService to improve authentication performance.
 * User details are cached by username with a TTL to reduce database lookups.
 */
@Service
class CachedUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    @Cacheable(value = ["userDetails"], key = "#username")
    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUserName(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }
    }
}