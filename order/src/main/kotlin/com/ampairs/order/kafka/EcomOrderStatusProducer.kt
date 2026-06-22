package com.ampairs.order.kafka

import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Publishes ecom order status changes in-process via Spring [ApplicationEventPublisher]; the ecom
 * module listens and updates the storefront order. No Kafka required — extensible to a broker later
 * via an event bridge that `@EventListener`s [EcomOrderStatusEvent].
 */
@Component
class EcomOrderStatusProducer(
    private val publisher: ApplicationEventPublisher,
) {
    fun publishStatusUpdate(event: EcomOrderStatusEvent) {
        publisher.publishEvent(event)
    }
}
