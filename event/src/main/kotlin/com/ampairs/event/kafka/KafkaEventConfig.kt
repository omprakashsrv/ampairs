package com.ampairs.event.kafka

import com.ampairs.event.config.WebSocketConfigProperties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import java.util.UUID

@Configuration
@Conditional(KafkaAvailableCondition::class)
class KafkaEventConfig(private val props: WebSocketConfigProperties) {

    @Bean
    fun workspaceEventKafkaTemplate(): KafkaTemplate<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to props.kafka.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "1",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 500
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }

    @Bean
    fun workspaceEventListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        // Each app instance gets a unique consumer group so all instances receive every message (fan-out).
        val instanceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to props.kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "ampairs-ws-$instanceId",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true
        )
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(DefaultKafkaConsumerFactory(config))
        }
    }
}
