package com.ampairs.account.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Scheduled job to permanently delete accounts past their grace period
 * Runs daily at 2 AM
 */
@Service
class AccountDeletionScheduler @Autowired constructor(
    private val accountDeletionService: AccountDeletionService
) {

    private val logger = LoggerFactory.getLogger(AccountDeletionScheduler::class.java)

    /**
     * Run daily at 2 AM to permanently delete accounts past grace period
     * Cron: second, minute, hour, day of month, month, day(s) of week
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun executeScheduledAccountDeletions() {
        logger.info("Starting scheduled account deletion job")

        try {
            val accountsToDelete = accountDeletionService.findAccountsReadyForDeletion()

            if (accountsToDelete.isEmpty()) {
                logger.info("No accounts ready for permanent deletion")
                return
            }

            logger.info("Found ${accountsToDelete.size} accounts ready for permanent deletion")

            var successCount = 0
            var failureCount = 0

            accountsToDelete.forEach { user ->
                try {
                    accountDeletionService.executePermanentDeletion(user.uid)
                    successCount++
                } catch (e: Exception) {
                    logger.error("Failed to permanently delete account ${user.uid}", e)
                    failureCount++
                }
            }

            logger.info("Scheduled account deletion completed: $successCount succeeded, $failureCount failed")
        } catch (e: Exception) {
            logger.error("Error during scheduled account deletion job", e)
        }
    }
}
