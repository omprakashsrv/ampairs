package com.ampairs.product.kafka

import com.ampairs.event.domain.kafka.EcomCatalogEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Configuration
class EcomCatalogKafkaConfig(
    @Value("\${ecom.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
) {
    @Bean("ecomCatalogKafkaTemplate")
    fun ecomCatalogKafkaTemplate(): KafkaTemplate<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "1",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 500,
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }
}

@Component
class EcomCatalogKafkaProducer(
    private val ecomCatalogKafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(EcomCatalogKafkaProducer::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    fun publish(event: EcomCatalogEvent) {
        try {
            val payload = objectMapper.writeValueAsString(event)
            ecomCatalogKafkaTemplate.send("ecom-catalog-events", event.workspaceId, payload)
            log.debug("Published catalog event {} for product {}", event.eventType, event.managementProductId)
        } catch (ex: Exception) {
            log.error("Failed to publish catalog event {} for product {}: {}", event.eventType, event.managementProductId, ex.message, ex)
            throw ex
        }
    }
}
