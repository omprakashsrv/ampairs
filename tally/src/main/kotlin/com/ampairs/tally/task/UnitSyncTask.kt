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
        val tallyXML = TallyXML()
        tallyXML.header.id = "CUSTOMLEDGERCOL"
        tallyXML.body.desc.tdl.tdlMessage.collection.name = "CUSTOMLEDGERCOL"
        tallyXML.body.desc.tdl.tdlMessage.collection.type = "LEDGER"
        val responseEntity = tallyClient.post(tallyXML)
        println("responseEntity = ${responseEntity}")
    }
}

