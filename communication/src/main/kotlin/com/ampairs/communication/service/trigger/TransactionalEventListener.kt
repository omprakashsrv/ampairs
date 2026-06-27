package com.ampairs.communication.service.trigger

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.service.send.CommunicationDispatchService
import com.ampairs.communication.service.template.TemplateService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Fires transactional sends from domain events. Maps an event type to its [event_template_binding],
 * builds the variable context from the event, and dispatches. Bypasses promotional opt-out/quiet
 * hours by virtue of the template category being TRANSACTIONAL.
 *
 * Recipient (email/phone/locale) is resolved from the event's `customerId` via the customer module's
 * public `CustomerContactProvider` (through `CustomerAudiencePort`). A workspace must have an
 * `event_template_binding` for the event type and the customer must have a contact on the chosen
 * channel; otherwise the per-channel send is skipped and logged.
 */
@Component
class TransactionalEventListener(
    private val bindingService: BindingService,
    private val templateService: TemplateService,
    private val audiencePort: CustomerAudiencePort,
    private val dispatchService: CommunicationDispatchService,
) {
    private val logger = LoggerFactory.getLogger(TransactionalEventListener::class.java)

    @EventListener
    @Async
    fun onInvoiceCreated(event: InvoiceCreatedEvent) {
        TenantContextHolder.withTenant(event.workspaceId) {
            handle(
                eventType = "INVOICE_CREATED",
                dedupKey = "INVOICE_CREATED:${event.entityId}",
                customerUid = event.customerId,
                variables = mapOf(
                    "invoice_number" to event.invoiceNumber,
                    "customer_name" to event.customerName,
                    "total_amount" to event.totalAmount.toString(),
                ),
            )
        }
    }

    @EventListener
    @Async
    fun onOrderCreated(event: OrderCreatedEvent) {
        TenantContextHolder.withTenant(event.workspaceId) {
            handle(
                eventType = "ORDER_CREATED",
                dedupKey = "ORDER_CREATED:${event.entityId}",
                customerUid = event.customerId,
                variables = mapOf(
                    "order_number" to event.orderNumber,
                    "customer_name" to event.customerName,
                    "total_amount" to event.totalAmount.toString(),
                ),
            )
        }
    }

    private fun handle(
        eventType: String,
        dedupKey: String,
        customerUid: String?,
        variables: Map<String, String>,
    ) {
        val binding = bindingService.findForEvent(eventType) ?: return
        val (template, variants) = templateService.findByUid(binding.templateUid) ?: run {
            logger.warn("Binding for {} references missing template {}", eventType, binding.templateUid)
            return
        }
        val channels = binding.channels.split(",")
            .mapNotNull { runCatching { Channel.valueOf(it.trim().uppercase()) }.getOrNull() }
            .ifEmpty { variants.map { it.channel }.distinct().mapNotNull { runCatching { Channel.valueOf(it) }.getOrNull() } }

        val recipients = audiencePort.resolve("SINGLE", customerUid, explicit = emptyList())
        if (recipients.isEmpty()) {
            logger.warn(
                "Transactional event {} matched template '{}' but no recipients resolved " +
                    "(customer contact provider not yet wired); skipping send", eventType, template.code
            )
            return
        }
        dispatchService.dispatch(
            template = template,
            variants = variants,
            channels = channels,
            recipients = recipients,
            variables = variables,
            triggerType = TriggerType.EVENT,
            sourceRef = dedupKey,
            dedupKey = dedupKey,
        )
    }
}
