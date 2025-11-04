package com.ampairs.workspace.config

import com.ampairs.workspace.service.LocalUserDetailProvider
import com.ampairs.workspace.service.UserDetailProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for user detail providers
 * This allows external modules to provide their own UserDetailProvider implementation
 * while falling back to the local implementation when none is available
 */
@Configuration
class UserDetailConfiguration {

    /**
     * Provides a default LocalUserDetailProvider when no other implementation is available
     * This bean will be overridden if another UserDetailProvider bean is registered
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailProvider::class)
    fun localUserDetailProvider(): UserDetailProvider {
        return LocalUserDetailProvider()
    }
}