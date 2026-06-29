package com.ampairs.dms.listener

import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.dms.domain.service.SecondarySalesRecomputeService
import com.ampairs.dms.domain.service.SnapshotDebounceCoordinator
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.product.service.ProductService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TradeSalesListenerTest {

    private val invoiceService: InvoiceService = mock()
    private val productService: ProductService = mock()
    private val customerService: CustomerService = mock()
    private val recomputeService: SecondarySalesRecomputeService = mock()
    private val debounce: SnapshotDebounceCoordinator = mock()
    private val listener = TradeSalesListener(invoiceService, productService, customerService, recomputeService, debounce)

    @Test
    fun `rebuild runs the recompute when the debounce gate opens`() {
        whenever(debounce.shouldRebuild(eq("DIST"), any())).thenReturn(true)
        whenever(invoiceService.getInvoices(anyOrNull())).thenReturn(emptyList())
        listener.rebuild("DIST")
        verify(recomputeService).recompute(eq("DIST"), any(), any(), any())
    }

    @Test
    fun `rebuild is skipped when coalesced away`() {
        whenever(debounce.shouldRebuild(eq("DIST"), any())).thenReturn(false)
        listener.rebuild("DIST")
        verify(recomputeService, never()).recompute(any(), any(), any(), any())
    }

    @Test
    fun `blank distributor is ignored`() {
        listener.rebuild("")
        verify(debounce, never()).shouldRebuild(any(), any())
    }
}
