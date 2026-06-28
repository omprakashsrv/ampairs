package com.ampairs.communication.service.trigger

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.domain.model.EventTemplateBinding
import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.service.send.CommunicationDispatchService
import com.ampairs.communication.service.template.TemplateService
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TransactionalEventListenerTest {

    private val bindingService: BindingService = mock()
    private val templateService: TemplateService = mock()
    private val audiencePort: CustomerAudiencePort = mock()
    private val dispatchService: CommunicationDispatchService = mock()
    private val listener = TransactionalEventListener(bindingService, templateService, audiencePort, dispatchService)

    @Test
    fun `invoice-created event renders bound template and dispatches to the resolved customer`() {
        whenever(bindingService.findForEvent("INVOICE_CREATED")).thenReturn(
            EventTemplateBinding().apply { eventType = "INVOICE_CREATED"; templateUid = "CTPL1"; channels = "EMAIL,SMS" }
        )
        val template = MessageTemplate().apply { uid = "CTPL1"; code = "INV" }
        whenever(templateService.findByUid("CTPL1")).thenReturn(template to listOf(TemplateVariant()))
        whenever(audiencePort.resolve(eq("SINGLE"), eq("CUS1"), any()))
            .thenReturn(listOf(Recipient(customerUid = "CUS1", email = "a@b.com")))

        listener.onInvoiceCreated(
            InvoiceCreatedEvent(this, "WS1", "INV-uid", "user", "device", "INV-1", "Asha", 999.0, "CUS1")
        )

        val channels = argumentCaptor<List<Channel>>()
        val vars = argumentCaptor<Map<String, String>>()
        verify(dispatchService).dispatch(
            eq(template), any(), channels.capture(), any(), vars.capture(),
            eq(TriggerType.EVENT), any(), any(),
        )
        assert(channels.firstValue.containsAll(listOf(Channel.EMAIL, Channel.SMS)))
        assert(vars.firstValue["invoice_number"] == "INV-1")
        assert(vars.firstValue["customer_name"] == "Asha")
    }

    @Test
    fun `no binding means no dispatch`() {
        whenever(bindingService.findForEvent(any())).thenReturn(null)
        listener.onInvoiceCreated(
            InvoiceCreatedEvent(this, "WS1", "INV-uid", "user", "device", "INV-1", "Asha", 999.0, "CUS1")
        )
        verify(dispatchService, org.mockito.kotlin.never()).dispatch(any(), any(), any(), any(), any(), any(), any(), any())
    }
}
