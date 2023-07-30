package com.ampairs.tally.task

import com.ampairs.network.customer.CustomerApi
import com.ampairs.tally.model.Type
import com.ampairs.tally.model.dto.toCustomers
import com.ampairs.tally.model.toTallyXML
import com.ampairs.tally.service.TallyClient
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CustomerSyncTask @Autowired constructor(val tallyClient: TallyClient, val customerApi: CustomerApi) {

    @Scheduled(fixedDelay = 2 * 10 * 1000)
    fun syncCustomer() {
        runBlocking {
            val tallyLedgers = tallyClient.post(Type.LEDGER.toTallyXML())
            val customersResponse =
                tallyLedgers?.body?.data?.collection?.ledgers?.toCustomers()?.let { customerApi.updateCustomers(it) }
            customersResponse?.onSuccess {
                println("Customer Synced")
            }

        }
    }
}

