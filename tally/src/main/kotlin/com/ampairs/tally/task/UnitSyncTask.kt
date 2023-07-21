package com.ampairs.tally.task

import com.ampairs.tally.model.TallyXML
import com.ampairs.tally.service.TallyClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UnitSyncTask @Autowired constructor(val tallyClient: TallyClient) {

    @Scheduled(fixedDelay = 2 * 10 * 1000)
    fun syncUnits() {
        val responseEntity = tallyClient.post(TallyXML())
        println("responseEntity = ${responseEntity}")
    }
}

