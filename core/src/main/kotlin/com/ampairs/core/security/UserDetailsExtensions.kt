package com.ampairs.core.security

interface UserDetailsWithId {
    fun getId(): String
}

interface UserDetailsWithRoles {
    fun getRoles(): List<String>
}
