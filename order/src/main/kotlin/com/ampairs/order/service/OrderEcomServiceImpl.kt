package com.ampairs.order.service

import com.ampairs.core.service.ConfirmedLineItem
import com.ampairs.core.service.OrderEcomService
import com.ampairs.event.domain.kafka.ConfirmedLineItemPayload
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.kafka.EcomOrderStatusProducer
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OrderEcomServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderService: OrderService,
    private val ecomOrderStatusProducer: EcomOrderStatusProducer,
) : OrderEcomService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun confirmEcomOrder(ecomOrderRef: String, confirmedItems: List<ConfirmedLineItem>) {
        val order = orderRepository.findByEcomOrderRef(ecomOrderRef)
        if (order == null) {
            log.warn("confirmEcomOrder: no order found for ecomOrderRef={}", ecomOrderRef)
            return
        }
        if (order.status != OrderStatus.PENDING_MERCHANT_REVIEW) {
            log.warn("confirmEcomOrder: order {} is not in PENDING_MERCHANT_REVIEW (current={})", ecomOrderRef, order.status)
            return
        }
        order.status = OrderStatus.CONFIRMED
        orderRepository.save(order)

        // Merchant confirmation raises the invoice for the order (idempotent: OrderService.createInvoice
        // reuses an existing invoiceRefId). Confirm + invoice are atomic — createInvoice joins this
        // transaction, so if the invoice can't be created the confirmation rolls back too (no confirmed
        // ecom order without an invoice), and the merchant can retry.
        if (order.invoiceRefId.isNullOrEmpty()) {
            val items = orderItemRepository.findByOrderIdOrderByIndexAsc(order.uid)
            orderService.createInvoice(order, items)
        }

        val confirmedPayloads = confirmedItems.map { item ->
            ConfirmedLineItemPayload(
                managementProductId = item.managementProductId,
                quantityConfirmed = item.quantityConfirmed,
                status = item.status,
            )
        }
        ecomOrderStatusProducer.publishStatusUpdate(
            EcomOrderStatusEvent(
                ecomOrderRef = ecomOrderRef,
                workspaceId = order.ownerId,
                newStatus = OrderStatus.CONFIRMED.name,
                managementOrderRef = order.uid,
                confirmedLineItems = confirmedPayloads,
                updatedAt = Instant.now(),
            )
        )
    }
}
