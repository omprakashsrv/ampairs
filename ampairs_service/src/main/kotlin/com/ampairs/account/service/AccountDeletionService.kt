package com.ampairs.account.service

import com.ampairs.account.dto.AccountDeletionRequest
import com.ampairs.account.dto.AccountDeletionResponse
import com.ampairs.account.dto.AccountDeletionStatusResponse
import com.ampairs.account.dto.WorkspaceOwnershipInfo
import com.ampairs.auth.repository.TokenRepository
import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import com.ampairs.user.service.UserService
import com.ampairs.workspace.service.WorkspaceMemberService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * Application-layer service coordinating user account deletion across the auth and workspace modules.
 * Cross-module repository injection is intentionally avoided — workspace interactions go through
 * WorkspaceMemberService's public interface per the module boundary rule.
 */
@Service
class AccountDeletionService(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val tokenRepository: TokenRepository,
    private val workspaceMemberService: WorkspaceMemberService,
) {

    private val logger = LoggerFactory.getLogger(AccountDeletionService::class.java)

    /**
     * Request account deletion with validation
     * Checks for sole workspace ownership and blocks if found
     */
    @Transactional
    fun requestAccountDeletion(request: AccountDeletionRequest): AccountDeletionResponse {
        if (!request.confirmed) {
            throw IllegalArgumentException("Account deletion must be confirmed")
        }

        val user = userService.getSessionUser()

        // Check if already deleted
        if (user.isDeleted()) {
            return buildDeletionResponse(user, "Account is already marked for deletion")
        }

        // Check for sole workspace ownership
        val blockingWorkspaces = findWorkspacesWhereSoleOwner(user.uid)
        if (blockingWorkspaces.isNotEmpty()) {
            return AccountDeletionResponse(
                userId = user.uid,
                deletionRequested = false,
                deletedAt = null,
                deletionScheduledFor = null,
                daysUntilPermanentDeletion = null,
                message = "Cannot delete account: You are the sole owner of ${blockingWorkspaces.size} workspace(s). " +
                        "Please transfer ownership or delete these workspaces first.",
                blockingWorkspaces = blockingWorkspaces,
                canRestore = false
            )
        }

        // Mark user for deletion
        user.markForDeletion(request.reason)

        // Note: We do NOT anonymize data or revoke tokens immediately
        // This allows users to cancel deletion during the 30-day grace period
        // Data anonymization and token revocation happen during permanent deletion

        userRepository.save(user)

        // Note: Workspace memberships remain active during grace period
        // This allows users to continue using the app until permanent deletion

        logger.info("Account deletion requested for user ${user.uid}. Permanent deletion scheduled for ${user.deletionScheduledFor}")

        return buildDeletionResponse(user, "Account deletion requested successfully. Your data will be permanently deleted in 30 days.")
    }

    /**
     * Cancel account deletion and restore account (only within grace period)
     */
    @Transactional
    fun cancelAccountDeletion(): AccountDeletionResponse {
        val user = userService.getSessionUser()

        if (!user.isDeleted()) {
            throw IllegalArgumentException("Account is not marked for deletion")
        }

        if (!user.canRestoreAccount()) {
            throw IllegalStateException("Grace period has expired. Account cannot be restored.")
        }

        user.restoreAccount()
        userRepository.save(user)

        logger.info("Account deletion cancelled for user ${user.uid}")

        return AccountDeletionResponse(
            userId = user.uid,
            deletionRequested = false,
            deletedAt = null,
            deletionScheduledFor = null,
            daysUntilPermanentDeletion = null,
            message = "Account restoration successful. Your account has been reactivated.",
            canRestore = false
        )
    }

    /**
     * Get account deletion status
     */
    fun getAccountDeletionStatus(): AccountDeletionStatusResponse {
        val user = userService.getSessionUser()

        val daysRemaining = user.deletionScheduledFor?.let {
            Duration.between(Instant.now(), it).toDays()
        }

        val statusMessage = when {
            !user.isDeleted() -> "Your account is active"
            user.canRestoreAccount() -> "Your account is scheduled for deletion in $daysRemaining days"
            user.isReadyForPermanentDeletion() -> "Your account is scheduled for permanent deletion"
            else -> "Unknown status"
        }

        return AccountDeletionStatusResponse(
            isDeleted = user.isDeleted(),
            deletedAt = user.deletedAt,
            deletionScheduledFor = user.deletionScheduledFor,
            daysRemaining = daysRemaining,
            canRestore = user.canRestoreAccount(),
            deletionReason = user.deletionReason,
            statusMessage = statusMessage
        )
    }

    /**
     * Execute permanent account deletion for accounts past grace period
     * Called by scheduled job
     */
    @Transactional
    fun executePermanentDeletion(userId: String) {
        val user = userRepository.findByUid(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        if (!user.isReadyForPermanentDeletion()) {
            logger.warn("Attempted to permanently delete user $userId but grace period not expired")
            return
        }

        // Anonymize user data before deletion for audit trail
        user.anonymize()
        user.active = false
        userRepository.save(user)

        // Revoke all authentication tokens
        val tokens = tokenRepository.findAllValidTokenByUser(userId)
        tokens.forEach { token ->
            token.revoked = true
            token.expired = true
        }
        tokenRepository.saveAll(tokens)

        // Revoke and delete all workspace memberships (deactivate then delete)
        workspaceMemberService.revokeAndDeleteMembershipsForUser(userId)

        // Delete all tokens
        tokenRepository.deleteAll(tokens)

        // Delete user account
        userRepository.delete(user)

        logger.info("Permanently deleted user account: $userId")
    }

    /**
     * Find all accounts ready for permanent deletion
     */
    fun findAccountsReadyForDeletion(): List<User> {
        return userRepository.findByDeletedTrueAndDeletionScheduledForBefore(Instant.now())
    }

    private fun findWorkspacesWhereSoleOwner(userId: String): List<WorkspaceOwnershipInfo> {
        return workspaceMemberService.findBlockingWorkspacesForDeletion(userId).map { info ->
            WorkspaceOwnershipInfo(
                workspaceId = info.workspaceId,
                workspaceName = info.workspaceName,
                workspaceSlug = info.workspaceSlug,
                memberCount = info.memberCount.toInt(),
            )
        }
    }

    /**
     * Build deletion response from user entity
     */
    private fun buildDeletionResponse(user: User, message: String): AccountDeletionResponse {
        val daysUntilDeletion = user.deletionScheduledFor?.let {
            Duration.between(Instant.now(), it).toDays()
        }

        return AccountDeletionResponse(
            userId = user.uid,
            deletionRequested = user.isDeleted(),
            deletedAt = user.deletedAt,
            deletionScheduledFor = user.deletionScheduledFor,
            daysUntilPermanentDeletion = daysUntilDeletion,
            message = message,
            canRestore = user.canRestoreAccount()
        )
    }
}
