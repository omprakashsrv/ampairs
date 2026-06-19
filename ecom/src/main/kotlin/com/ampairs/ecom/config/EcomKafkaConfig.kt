package com.ampairs.ecom.config

import com.ampairs.event.kafka.KafkaAvailableCondition
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
@Conditional(KafkaAvailableCondition::class)
class EcomKafkaConfig(
    @Value("\${ecom.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
) {

    private fun dlqKafkaTemplate(): KafkaTemplate<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }

    private fun defaultErrorHandler(): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(dlqKafkaTemplate())
        return DefaultErrorHandler(recoverer, FixedBackOff(500L, 3L))
    }

    @Bean
    fun ecomCatalogListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "ecom-catalog-consumer",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        )
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(DefaultKafkaConsumerFactory(config))
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(defaultErrorHandler())
        }
    }

    @Bean
    fun ecomOrderStatusListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "ecom-order-status-consumer",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        )
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(DefaultKafkaConsumerFactory(config))
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setCommonErrorHandler(defaultErrorHandler())
        }
    }

    @Bean
    fun ecomOrderKafkaTemplate(): KafkaTemplate<String, String> {
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
