package com.ampairs.user.service

import com.ampairs.core.service.UserService as CoreUserService
import org.springframework.stereotype.Service

@Service
class CoreUserServiceImpl(
    private val authUserService: UserService
) : CoreUserService {

    override fun getUserById(userId: String): com.ampairs.core.domain.User? {
        return try {
            authUserService.getUser(userId)
        } catch (_: Exception) {
            null
        }
    }

    override fun getUsersByIds(userIds: List<String>): Map<String, com.ampairs.core.domain.User> {
        return try {
            userIds.mapNotNull { id ->
                try {
                    val user = authUserService.getUser(id)
                    id to user
                } catch (_: Exception) {
                    null
                }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun getCurrentUser(): com.ampairs.core.domain.User? {
        return try {
            authUserService.getSessionUser()
        } catch (_: Exception) {
            null
        }
    }

    override fun userExists(userId: String): Boolean {
        return try {
            authUserService.getUser(userId)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun getCurrentUserId(): String? {
        return try {
            authUserService.getSessionUser().uid
        } catch (_: Exception) {
            null
        }
    }

    override fun getUserByPhone(phone: String): com.ampairs.core.domain.User? {
        return authUserService.getUserByPhone(phone)
    }
}
