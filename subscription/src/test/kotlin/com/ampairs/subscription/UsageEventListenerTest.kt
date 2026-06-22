package com.ampairs.subscription

import com.ampairs.event.domain.events.CustomerCreatedEvent
import com.ampairs.event.domain.events.CustomerDeletedEvent
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.InvoiceDeletedEvent
import com.ampairs.event.domain.events.MemberActivatedEvent
import com.ampairs.event.domain.events.MemberAddedEvent
import com.ampairs.event.domain.events.MemberDeactivatedEvent
import com.ampairs.event.domain.events.MemberRemovedEvent
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderDeletedEvent
import com.ampairs.event.domain.events.ProductCreatedEvent
import com.ampairs.event.domain.events.ProductDeletedEvent
import com.ampairs.subscription.domain.service.UsageTrackingService
import com.ampairs.subscription.listener.ResourceType
import com.ampairs.subscription.listener.UsageEventListener
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsageEventListenerTest {

    @Mock private lateinit var usageTrackingService: UsageTrackingService
    private lateinit var listener: UsageEventListener

    private val src = Any()
    private val ws = "WS-1"

    @BeforeEach
    fun setUp() {
        listener = UsageEventListener(usageTrackingService)
    }

    @Test
    fun `member events adjust member count`() {
        listener.onMemberAdded(MemberAddedEvent(src, ws, "E1", "U1", "D1", "MU1", "ADMIN"))
        listener.onMemberActivated(MemberActivatedEvent(src, ws, "E1", "U1", "D1", "MU1"))
        verify(usageTrackingService, org.mockito.kotlin.times(2)).incrementCount(ws, ResourceType.MEMBER)

        listener.onMemberRemoved(MemberRemovedEvent(src, ws, "E1", "U1", "D1", "MU1"))
        listener.onMemberDeactivated(MemberDeactivatedEvent(src, ws, "E1", "U1", "D1", "MU1"))
        verify(usageTrackingService, org.mockito.kotlin.times(2)).decrementCount(ws, ResourceType.MEMBER)
    }

    @Test
    fun `customer events adjust customer count`() {
        listener.onCustomerCreated(CustomerCreatedEvent(src, ws, "C1", "U1", "D1", "Acme", "B2B"))
        verify(usageTrackingService).incrementCount(ws, ResourceType.CUSTOMER)

        listener.onCustomerDeleted(CustomerDeletedEvent(src, ws, "C1", "U1", "D1", "Acme"))
        verify(usageTrackingService).decrementCount(ws, ResourceType.CUSTOMER)
    }

    @Test
    fun `product events adjust product count`() {
        listener.onProductCreated(ProductCreatedEvent(src, ws, "P1", "U1", "D1", "Widget", "SKU1"))
        verify(usageTrackingService).incrementCount(ws, ResourceType.PRODUCT)

        listener.onProductDeleted(ProductDeletedEvent(src, ws, "P1", "U1", "D1", "Widget"))
        verify(usageTrackingService).decrementCount(ws, ResourceType.PRODUCT)
    }

    @Test
    fun `invoice events adjust invoice count`() {
        listener.onInvoiceCreated(InvoiceCreatedEvent(src, ws, "I1", "U1", "D1", "INV-1", "Acme", 100.0))
        verify(usageTrackingService).incrementCount(ws, ResourceType.INVOICE)

        listener.onInvoiceDeleted(InvoiceDeletedEvent(src, ws, "I1", "U1", "D1", "INV-1"))
        verify(usageTrackingService).decrementCount(ws, ResourceType.INVOICE)
    }

    @Test
    fun `order events adjust order count`() {
        listener.onOrderCreated(OrderCreatedEvent(src, ws, "O1", "U1", "D1", "ORD-1", "Acme", 50.0))
        verify(usageTrackingService).incrementCount(ws, ResourceType.ORDER)

        listener.onOrderDeleted(OrderDeletedEvent(src, ws, "O1", "U1", "D1", "ORD-1"))
        verify(usageTrackingService).decrementCount(ws, ResourceType.ORDER)
    }
}
