package com.ampairs

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.TimeZone


@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
class AmpairsApplication {

    private val logger = LoggerFactory.getLogger(AmpairsApplication::class.java)

    /**
     * Initialize JVM default timezone to UTC.
     *
     * This ensures all date/time operations default to UTC timezone,
     * preventing timezone-related bugs from LocalDateTime.now() and similar calls.
     *
     * Must be called before any date/time operations occur.
     */
    @PostConstruct
    fun initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        logger.info("JVM default timezone set to UTC: {}", TimeZone.getDefault().id)
    }
}

fun main(args: Array<String>) {
    runApplication<AmpairsApplication>(*args)
}
