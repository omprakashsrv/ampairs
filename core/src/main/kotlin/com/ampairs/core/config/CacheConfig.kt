package com.ampairs.core.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit
import javax.cache.Caching
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.CacheManager as JCacheManager
import javax.cache.expiry.Duration as JCacheDuration

@Configuration
@EnableCaching
class CacheConfig(
    private val applicationProperties: ApplicationProperties,
) {

    @Bean("defaultCacheManager")
    @Primary
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(applicationProperties.cache.maxSize)
                .expireAfterAccess(applicationProperties.cache.defaultTtl.toMinutes(), TimeUnit.MINUTES)
                .recordStats()
        )
        return cacheManager
    }

    @Bean("rateLimitCacheManager")
    @ConditionalOnProperty(name = ["bucket4j.enabled"], havingValue = "true", matchIfMissing = false)
    fun rateLimitCacheManager(): JCacheManager {
        val cacheManager = Caching.getCachingProvider(
            "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider"
        ).cacheManager

        // Configure rate limit cache
        val rateLimitConfig = MutableConfiguration<String, ByteArray>()
            .setTypes(String::class.java, ByteArray::class.java)
            .setExpiryPolicyFactory(
                AccessedExpiryPolicy.factoryOf(
                    JCacheDuration(
                        TimeUnit.MINUTES,
                        applicationProperties.cache.rateLimitCache.expireAfterAccess.toMinutes()
                    )
                )
            )
            .setStatisticsEnabled(true)

        cacheManager.createCache("rate-limit-bucket", rateLimitConfig)

        return cacheManager
    }

    @Bean("rateLimitCaffeineCacheManager")
    fun rateLimitCaffeineCacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("rate-limit-bucket")
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(applicationProperties.cache.rateLimitCache.maximumSize)
                .expireAfterAccess(
                    applicationProperties.cache.rateLimitCache.expireAfterAccess.toMinutes(),
                    TimeUnit.MINUTES
                )
                .recordStats()
        )
        return cacheManager
    }
}