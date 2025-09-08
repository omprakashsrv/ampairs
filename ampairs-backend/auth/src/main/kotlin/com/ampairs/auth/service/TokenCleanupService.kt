package com.ampairs.auth.service

import com.ampairs.auth.model.Token
import com.ampairs.auth.repository.TokenRepository
import com.ampairs.core.config.ApplicationProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*

/**
 * Service for cleaning up expired and revoked tokens from the database.
 * Since we now use a blacklist-only approach, we need to periodically clean up
 * expired tokens to prevent database bloat.
 */
@Service
@ConditionalOnProperty(
    name = ["application.security.token-cleanup.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TokenCleanupService(
    private val tokenRepository: TokenRepository,
    private val applicationProperties: ApplicationProperties,
    private val transactionTemplate: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(TokenCleanupService::class.java)

    /**
     * Optimized scheduled task to clean up expired tokens.
     * Uses token-specific expiration times for efficient cleanup:
     * 1. Access tokens: Cleaned up after 24 hours + buffer
     * 2. Refresh tokens: Cleaned up after 180 days + buffer
     * 3. Explicitly revoked/expired tokens: Immediate cleanup
     */
    @Scheduled(cron = "\${application.security.token-cleanup.cron:0 0 2 * * ?}") // Daily at 2 AM
    fun cleanupExpiredTokens() {
        logger.info("Starting optimized token cleanup...")

        try {
            var totalDeletedCount = 0

            // Step 1: Clean up explicitly marked expired/revoked tokens immediately
            val explicitlyRevokedCount = cleanupExplicitlyRevokedTokens()
            totalDeletedCount += explicitlyRevokedCount
            logger.info("Cleaned up {} explicitly revoked/expired tokens", explicitlyRevokedCount)

            // Step 2: Clean up access tokens based on their shorter expiration
            val expiredAccessTokensCount = cleanupExpiredAccessTokens()
            totalDeletedCount += expiredAccessTokensCount
            logger.info("Cleaned up {} expired access tokens", expiredAccessTokensCount)

            // Step 3: Clean up refresh tokens based on their longer expiration
            val expiredRefreshTokensCount = cleanupExpiredRefreshTokens()
            totalDeletedCount += expiredRefreshTokensCount
            logger.info("Cleaned up {} expired refresh tokens", expiredRefreshTokensCount)

            logger.info("Token cleanup completed. Total deleted: {} tokens", totalDeletedCount)

        } catch (e: Exception) {
            logger.error("Error during token cleanup", e)
        }
    }

    /**
     * Manual cleanup method that can be called programmatically
     * Uses the same optimized logic as the scheduled task
     */
    fun performManualCleanup(): Int {
        logger.info("Performing manual optimized token cleanup...")

        var totalDeletedCount = 0

        // Clean up explicitly revoked tokens
        totalDeletedCount += cleanupExplicitlyRevokedTokens()

        // Clean up expired access tokens
        totalDeletedCount += cleanupExpiredAccessTokens()

        // Clean up expired refresh tokens  
        totalDeletedCount += cleanupExpiredRefreshTokens()

        logger.info("Manual token cleanup completed. Deleted {} tokens", totalDeletedCount)
        return totalDeletedCount
    }

    /**
     * Clean up tokens that are explicitly marked as expired or revoked
     */
    private fun cleanupExplicitlyRevokedTokens(): Int {
        return try {
            // Use a far future date to only match explicitly expired/revoked tokens
            val farFutureDate = LocalDateTime.now().plusYears(100)
            tokenRepository.deleteExpiredOrRevokedTokens(farFutureDate)
        } catch (e: Exception) {
            logger.warn("Error cleaning up explicitly revoked tokens", e)
            0
        }
    }

    /**
     * Clean up access tokens that have exceeded their expiration time
     * Uses paginated fetching and batch processing to avoid memory and transaction issues
     */
    private fun cleanupExpiredAccessTokens(): Int {
        return try {
            val accessTokenCutoff = LocalDateTime.now()
                .minus(applicationProperties.security.jwt.expiration)
                .minusHours(2) // Small buffer for access tokens

            processBatchedTokenCleanupWithPagination(accessTokenCutoff) { tokenEntity ->
                isAccessTokenExpired(tokenEntity.token)
            }
        } catch (e: Exception) {
            logger.warn("Error cleaning up expired access tokens", e)
            0
        }
    }

    /**
     * Clean up refresh tokens that have exceeded their expiration time
     * Uses paginated fetching and batch processing to avoid memory and transaction issues
     */
    private fun cleanupExpiredRefreshTokens(): Int {
        return try {
            val refreshTokenCutoff = LocalDateTime.now()
                .minus(applicationProperties.security.jwt.refreshToken.expiration)
                .minusHours(24) // Larger buffer for long-lived refresh tokens

            processBatchedTokenCleanupWithPagination(refreshTokenCutoff) { tokenEntity ->
                isRefreshTokenExpired(tokenEntity.token)
            }
        } catch (e: Exception) {
            logger.warn("Error cleaning up expired refresh tokens", e)
            0
        }
    }

    /**
     * Process token cleanup using paginated fetching to avoid loading millions of tokens into memory
     * @param cutoffDate Date before which tokens should be considered for cleanup
     * @param shouldDelete Function to determine if a token should be deleted
     * @return Total number of tokens deleted
     */
    private fun processBatchedTokenCleanupWithPagination(
        cutoffDate: LocalDateTime,
        shouldDelete: (Token) -> Boolean,
    ): Int {
        val batchSize = applicationProperties.security.tokenCleanup.batchSize
        var totalDeletedCount = 0
        var currentPage = 0
        var hasMoreTokens = true

        while (hasMoreTokens) {
            try {
                // Fetch tokens in pages to avoid memory issues
                val pageable = PageRequest.of(currentPage, batchSize)
                val tokenBatch = tokenRepository.findExpiredOrRevokedTokensPaged(cutoffDate, pageable)

                if (tokenBatch.isEmpty()) {
                    hasMoreTokens = false
                    break
                }

                logger.debug("Processing page {} with {} tokens", currentPage, tokenBatch.size)

                // Filter tokens that should be deleted
                val tokensToDelete = tokenBatch.filter { tokenEntity ->
                    try {
                        shouldDelete(tokenEntity)
                    } catch (e: Exception) {
                        logger.debug("Error evaluating token for deletion: {}", tokenEntity.id, e)
                        // If we can't evaluate, err on side of caution and don't delete
                        false
                    }
                }

                // Delete the filtered tokens in a separate transaction
                if (tokensToDelete.isNotEmpty()) {
                    val deletedCount = deleteTokensInSeparateTransaction(tokensToDelete)
                    totalDeletedCount += deletedCount
                    logger.debug("Deleted {} tokens from page {}", deletedCount, currentPage)
                }

                currentPage++

                // Small delay between pages to reduce database load
                Thread.sleep(50) // 50ms delay between pages

                // If we got less than the batch size, we've reached the end
                if (tokenBatch.size < batchSize) {
                    hasMoreTokens = false
                }

            } catch (e: Exception) {
                logger.warn("Error processing token cleanup page {}", currentPage, e)
                // Continue with next page even if current page fails
                currentPage++

                // Prevent infinite loop on persistent errors
                if (currentPage > 1000) { // Safety limit
                    logger.error("Too many pages processed, stopping cleanup to prevent infinite loop")
                    break
                }
            }
        }

        logger.debug(
            "Completed paginated token cleanup. Total pages: {}, Total deleted: {}",
            currentPage,
            totalDeletedCount
        )
        return totalDeletedCount
    }

    /**
     * Delete a batch of tokens in a separate transaction to avoid long-running transactions
     * Uses TransactionTemplate with REQUIRES_NEW to ensure each batch runs in its own transaction
     * @param tokensToDelete List of tokens to delete in this batch
     * @return Number of tokens actually deleted
     */
    private fun deleteTokensInSeparateTransaction(tokensToDelete: List<Token>): Int {
        // Create a new transaction template with REQUIRES_NEW propagation
        val newTransactionTemplate = TransactionTemplate(transactionTemplate.transactionManager!!).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

        return try {
            newTransactionTemplate.execute {
                tokenRepository.deleteAll(tokensToDelete)
                tokensToDelete.size
            } ?: 0
        } catch (e: Exception) {
            logger.warn("Error deleting batch of {} tokens", tokensToDelete.size, e)
            // Return 0 if deletion failed, so the count stays accurate
            0
        }
    }

    /**
     * Check if a token is an expired access token
     */
    private fun isAccessTokenExpired(tokenString: String): Boolean {
        return try {
            val tokenType = extractTokenType(tokenString)
            val isExpired = isTokenExpired(tokenString)

            (tokenType == JwtService.ACCESS_TOKEN_TYPE || tokenType == null) && isExpired
        } catch (e: Exception) {
            // If we can't parse the token, it's likely malformed/expired, so clean it up
            true
        }
    }

    /**
     * Check if a token is an expired refresh token
     */
    private fun isRefreshTokenExpired(tokenString: String): Boolean {
        return try {
            val tokenType = extractTokenType(tokenString)
            val isExpired = isTokenExpired(tokenString)

            tokenType == JwtService.REFRESH_TOKEN_TYPE && isExpired
        } catch (e: Exception) {
            // If we can't parse the token, it's likely malformed/expired, so clean it up
            true
        }
    }

    /**
     * Extract token type from JWT claims
     */
    private fun extractTokenType(tokenString: String): String? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(tokenString)
                .body

            claims[JwtService.TOKEN_TYPE_CLAIM] as? String
        } catch (e: ExpiredJwtException) {
            // Token is expired but we can still read the claims
            e.claims[JwtService.TOKEN_TYPE_CLAIM] as? String
        } catch (e: Exception) {
            logger.debug("Could not extract token type from token", e)
            null
        }
    }

    /**
     * Check if JWT token is expired
     */
    private fun isTokenExpired(tokenString: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(tokenString)
                .body

            claims.expiration?.before(Date()) ?: true
        } catch (e: ExpiredJwtException) {
            true // Token is expired
        } catch (e: Exception) {
            true // Any other error means token is invalid/expired
        }
    }

    /**
     * Get JWT signing key
     */
    private fun getSigningKey(): java.security.Key {
        val keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(applicationProperties.security.jwt.secretKey)
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes)
    }

    /**
     * Get count of tokens that would be cleaned up (for monitoring/metrics)
     * Uses count queries to avoid loading tokens into memory
     */
    fun getExpiredTokenCount(): Long {
        return try {
            val accessTokenCutoff = LocalDateTime.now()
                .minus(applicationProperties.security.jwt.expiration)
                .minusHours(2)

            val refreshTokenCutoff = LocalDateTime.now()
                .minus(applicationProperties.security.jwt.refreshToken.expiration)
                .minusHours(24)

            // Use count queries instead of loading all tokens
            val accessTokenCount = tokenRepository.countExpiredOrRevokedTokens(accessTokenCutoff)
            val refreshTokenCount = tokenRepository.countExpiredOrRevokedTokens(refreshTokenCutoff)

            // Return the sum, but avoid double counting by taking the larger count
            // (some tokens might be in both categories)
            maxOf(accessTokenCount, refreshTokenCount)
        } catch (e: Exception) {
            logger.warn("Error counting expired tokens", e)
            0L
        }
    }
}