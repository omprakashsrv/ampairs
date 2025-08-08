package com.ampairs.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties(prefix = "application")
data class ApplicationProperties(
    val security: SecurityProperties = SecurityProperties(),
    val cache: CacheProperties = CacheProperties(),
    val integration: IntegrationProperties = IntegrationProperties(),
) {
    data class SecurityProperties(
        val jwt: JwtProperties = JwtProperties(),
        val cors: CorsProperties = CorsProperties(),
        val tokenCleanup: TokenCleanupProperties = TokenCleanupProperties(),
        val rateLimiting: RateLimitingProperties = RateLimitingProperties(),
    ) {
        data class JwtProperties(
            val secretKey: String = "",
            val expiration: Duration = Duration.ofDays(1),
            val refreshToken: RefreshTokenProperties = RefreshTokenProperties(),
        ) {
            data class RefreshTokenProperties(
                val expiration: Duration = Duration.ofDays(180),
            )
        }

        data class CorsProperties(
            val allowedOrigins: List<String> = listOf("http://localhost:3000"),
            val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
            val allowedHeaders: List<String> = listOf("*"),
            val allowCredentials: Boolean = true,
            val maxAge: Duration = Duration.ofHours(1),
        )

        data class TokenCleanupProperties(
            val enabled: Boolean = true,
            val cron: String = "0 0 2 * * ?", // Daily at 2 AM
            val batchSize: Int = 100, // Process tokens in batches to avoid memory/transaction issues
        )

        data class RateLimitingProperties(
            val enabled: Boolean = true,
            val auth: AuthRateLimitProperties = AuthRateLimitProperties(),
            val api: ApiRateLimitProperties = ApiRateLimitProperties(),
            val global: GlobalRateLimitProperties = GlobalRateLimitProperties(),
        ) {
            data class AuthRateLimitProperties(
                val init: RateLimitConfig = RateLimitConfig(capacity = 3, windowMinutes = 5, burstCapacity = 1),
                val verify: RateLimitConfig = RateLimitConfig(capacity = 5, windowMinutes = 5, burstCapacity = 2),
                val refreshToken: RateLimitConfig = RateLimitConfig(
                    capacity = 10,
                    windowMinutes = 1,
                    burstCapacity = 5
                ),
                val logout: RateLimitConfig = RateLimitConfig(capacity = 5, windowMinutes = 1, burstCapacity = 3),
                val deviceManagement: RateLimitConfig = RateLimitConfig(
                    capacity = 10,
                    windowMinutes = 1,
                    burstCapacity = 5
                ),
            )

            data class ApiRateLimitProperties(
                val authenticated: RateLimitConfig = RateLimitConfig(
                    capacity = 100,
                    windowMinutes = 1,
                    burstCapacity = 20
                ),
                val public: RateLimitConfig = RateLimitConfig(capacity = 50, windowMinutes = 1, burstCapacity = 10),
            )

            data class GlobalRateLimitProperties(
                val perIp: RateLimitConfig = RateLimitConfig(capacity = 200, windowMinutes = 1, burstCapacity = 50),
                val perUser: RateLimitConfig = RateLimitConfig(capacity = 150, windowMinutes = 1, burstCapacity = 30),
            )

            data class RateLimitConfig(
                val capacity: Int = 10,
                val windowMinutes: Int = 1,
                val burstCapacity: Int = 5,
            )
        }
    }

    data class CacheProperties(
        val defaultTtl: Duration = Duration.ofMinutes(30),
        val maxSize: Long = 10000,
        val rateLimitCache: RateLimitCacheProperties = RateLimitCacheProperties(),
    ) {
        data class RateLimitCacheProperties(
            val expireAfterAccess: Duration = Duration.ofMinutes(15),
            val maximumSize: Long = 10000,
        )
    }

    data class IntegrationProperties(
        val tally: TallyProperties = TallyProperties(),
        val notifications: NotificationProperties = NotificationProperties(),
    ) {
        data class TallyProperties(
            val baseUrl: String = "http://localhost:9000",
            val connectionTimeout: Duration = Duration.ofSeconds(30),
            val readTimeout: Duration = Duration.ofMinutes(2),
            val retryAttempts: Int = 3,
            val retryDelay: Duration = Duration.ofSeconds(5),
            val sync: SyncProperties = SyncProperties(),
        ) {
            data class SyncProperties(
                val customers: SyncTaskProperties = SyncTaskProperties(),
                val products: SyncTaskProperties = SyncTaskProperties(),
            ) {
                data class SyncTaskProperties(
                    val enabled: Boolean = true,
                    val cronExpression: String = "0 0 * * * *", // Every hour
                    val batchSize: Int = 100,
                )
            }
        }

        data class NotificationProperties(
            val email: EmailProperties = EmailProperties(),
            val sms: SmsProperties = SmsProperties(),
        ) {
            data class EmailProperties(
                val enabled: Boolean = true,
                val from: String = "noreply@ampairs.com",
            )

            data class SmsProperties(
                val enabled: Boolean = true,
                val provider: String = "aws-sns",
            )
        }
    }
}

@Configuration
@EnableConfigurationProperties(ApplicationProperties::class)
class ApplicationConfiguration