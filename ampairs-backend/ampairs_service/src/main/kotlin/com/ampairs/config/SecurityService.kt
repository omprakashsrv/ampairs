package com.ampairs.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service


@Service
class SecurityService {
    fun username(): String? {
        val name = SecurityContextHolder.getContext().authentication.name
        if (name == "anonymousUser") {
            return null
        }
        return name
    }

    fun notSignedIn(): Boolean {
        return SecurityContextHolder.getContext().authentication?.principal == null
    }
}