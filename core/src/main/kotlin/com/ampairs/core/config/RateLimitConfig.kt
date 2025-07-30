package com.ampairs.core.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.grid.jcache.JCacheProxyManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration
import java.util.function.Supplier
import javax.cache.CacheManager as JCacheManager

@Configuration
@ConditionalOnProperty(name = ["bucket4j.enabled"], havingValue = "true", matchIfMissing = false)
class RateLimitConfig(
    private val jCacheManager: JCacheManager,
) {

    @Bean
    fun bucketProxyManager(): ProxyManager<String> {
        return JCacheProxyManager(
            jCacheManager.getCache(
                "rate-limit-bucket",
                String::class.java,
                ByteArray::class.java
            )
        )
    }

    @Bean
    fun rateLimitInterceptor(proxyManager: ProxyManager<String>): RateLimitInterceptor {
        return RateLimitInterceptor(proxyManager)
    }

    @Bean
    fun rateLimitWebMvcConfigurer(rateLimitInterceptor: RateLimitInterceptor): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/auth/v1/init") // Strict rate limit for auth init
                    .addPathPatterns("/api/**") // General API rate limit
            }
        }
    }
}

@Component
@ConditionalOnProperty(name = ["bucket4j.enabled"], havingValue = "true", matchIfMissing = false)
class RateLimitInterceptor(
    private val proxyManager: ProxyManager<String>,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(RateLimitInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val clientIp = getClientIp(request)
        val requestPath = request.requestURI

        val bucket = getBucketForClient(clientIp, requestPath)

        return if (bucket.tryConsume(1)) {
            true
        } else {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write(
                """
                {
                    "success": false,
                    "error": {
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "Too many requests. Please try again later.",
                        "details": "Rate limit exceeded for your IP address"
                    },
                    "timestamp": "${java.time.LocalDateTime.now()}",
                    "path": "$requestPath"
                }
                """.trimIndent()
            )
            false
        }
    }

    private fun getBucketForClient(clientIp: String, requestPath: String): Bucket {
        val bucketKey = "$clientIp:$requestPath"

        return when {
            requestPath.startsWith("/auth/v1/init") -> {
                // Strict rate limit for auth initialization: 1 request per 20 seconds
                proxyManager.builder()
                    .build(bucketKey, createStrictAuthLimitSupplier())
            }

            requestPath.startsWith("/api/") -> {
                // General API rate limit: 20 requests per minute
                proxyManager.builder()
                    .build(bucketKey, createGeneralApiLimitSupplier())
            }

            else -> {
                // Default rate limit: 60 requests per minute
                proxyManager.builder()
                    .build(bucketKey, createDefaultLimitSupplier())
            }
        }
    }

    private fun createStrictAuthLimitSupplier(): Supplier<BucketConfiguration> {
        return Supplier {
            BucketConfiguration.builder()
                .addLimit(
                    Bandwidth.simple(1, Duration.ofSeconds(20))
                )
                .build()
        }
    }

    private fun createGeneralApiLimitSupplier(): Supplier<BucketConfiguration> {
        return Supplier {
            BucketConfiguration.builder()
                .addLimit(
                    Bandwidth.simple(20, Duration.ofMinutes(1))
                )
                .build()
        }
    }

    private fun createDefaultLimitSupplier(): Supplier<BucketConfiguration> {
        return Supplier {
            BucketConfiguration.builder()
                .addLimit(
                    Bandwidth.simple(60, Duration.ofMinutes(1))
                )
                .build()
        }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")

        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr
        }
    }
}