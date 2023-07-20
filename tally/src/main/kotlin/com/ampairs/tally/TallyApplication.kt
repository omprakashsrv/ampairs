package com.ampairs.tally

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TallyApplication

fun main(args: Array<String>) {
    runApplication<TallyApplication>(*args)
}
