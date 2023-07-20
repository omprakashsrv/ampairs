package com.ampairs.tally.task

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UnitSyncTask {

    @Scheduled(fixedDelay = 2 * 10 * 1000)
    fun syncUnits() {
        println("javaClass = ${javaClass}")
    }
}

