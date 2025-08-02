package com.ampairs.notification.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Configuration for notification processing
 */
@Configuration
class NotificationConfig {

    @Value("\${notification.parallel-threads:5}")
    private var parallelThreads: Int = 5

    @Value("\${notification.thread-pool.queue-capacity:100}")
    private var queueCapacity: Int = 100

    @Value("\${notification.thread-pool.keep-alive-seconds:60}")
    private var keepAliveSeconds: Int = 60

    /**
     * Thread pool executor for parallel notification processing
     */
    @Bean("notificationTaskExecutor")
    fun notificationTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()

        // Core pool size - minimum number of threads
        executor.corePoolSize = parallelThreads

        // Maximum pool size - maximum number of threads
        executor.maxPoolSize = parallelThreads * 2

        // Queue capacity for pending tasks
        executor.queueCapacity = queueCapacity

        // Keep alive time for idle threads
        executor.keepAliveSeconds = keepAliveSeconds

        // Thread name prefix for identification
        executor.threadNamePrefix = "notification-async-"

        // Rejection policy when queue is full
        executor.setRejectedExecutionHandler { task, executor ->
            // Log the rejection and execute in caller thread as fallback
            println("Notification task queue full, executing in caller thread")
            task.run()
        }

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)

        executor.initialize()
        return executor
    }
}