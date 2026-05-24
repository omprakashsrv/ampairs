package com.ampairs.notification.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.VirtualThreadTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableConfigurationProperties(NotificationProperties::class)
class NotificationConfig {

    /**
     * Virtual threads bring lightweight concurrency without manual pool sizing.
     */
    @Bean("notificationTaskExecutor")
    fun notificationTaskExecutor(): Executor {
        return VirtualThreadTaskExecutor("notification-async-")
    }
}
