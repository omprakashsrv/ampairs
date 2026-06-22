package com.ampairs.ecom.kafka

import com.ampairs.ecom.service.CatalogSyncService
import com.ampairs.event.domain.kafka.CatalogEventType
import com.ampairs.event.domain.kafka.EcomCatalogEvent
import com.ampairs.event.kafka.KafkaAvailableCondition
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Conditional(KafkaAvailableCondition::class)
class EcomCatalogKafkaConsumer(
    private val catalogSyncService: CatalogSyncService,
) {
    private val log = LoggerFactory.getLogger(EcomCatalogKafkaConsumer::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    @KafkaListener(
        topics = ["ecom-catalog-events"],
        containerFactory = "ecomCatalogListenerContainerFactory",
        groupId = "ecom-catalog-consumer",
    )
    fun consume(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        try {
            val event = objectMapper.readValue(record.value(), EcomCatalogEvent::class.java)
            log.debug("Received catalog event {} for product {}", event.eventType, event.managementProductId)

            when (event.eventType) {
                CatalogEventType.PRODUCT_LISTED   -> catalogSyncService.handleProductListed(event)
                CatalogEventType.PRODUCT_UNLISTED -> catalogSyncService.handleProductUnlisted(event)
                CatalogEventType.PRICE_UPDATED    -> catalogSyncService.handlePriceUpdated(event)
                CatalogEventType.STOCK_UPDATED    -> catalogSyncService.handleStockUpdated(event)
                CatalogEventType.DETAILS_UPDATED  -> catalogSyncService.handleDetailsUpdated(event)
            }
            ack.acknowledge()
        } catch (ex: Exception) {
            log.error("Failed to process catalog event offset={}: {}", record.offset(), ex.message, ex)
            ack.acknowledge()
        }
    }
}
