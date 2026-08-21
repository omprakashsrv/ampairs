package com.ampairs.ecom.controller

import com.ampairs.AmpairsApplication
import com.ampairs.core.service.BuyerAgingBucket
import com.ampairs.core.service.BuyerInvoiceDetail
import com.ampairs.core.service.BuyerInvoiceLine
import com.ampairs.core.service.BuyerInvoiceSummary
import com.ampairs.core.service.BuyerOpenBill
import com.ampairs.core.service.BuyerOutstandingResponse
import com.ampairs.core.service.BuyerStatementLine
import com.ampairs.core.service.BuyerStatementResponse
import com.ampairs.core.service.InvoiceEcomService
import com.ampairs.core.service.PartyLedgerEcomService
import com.ampairs.customer.domain.service.EcomCustomerServiceImpl
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.Storefront
import com.ampairs.ecom.kafka.EcomCatalogKafkaConsumer
import com.ampairs.ecom.kafka.EcomOrderKafkaProducer
import com.ampairs.ecom.kafka.EcomOrderStatusKafkaConsumer
import com.ampairs.ecom.service.CustomerAddressService
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.ecom.service.StorefrontService
import com.ampairs.workspace.service.WorkspaceMemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 — web-layer behavior of the buyer invoice/statement endpoints: link gate (403), wrong-party
 * (404), order↔invoice ref swap, order-detail invoices[], and the money-position surfaces. Services are
 * mocked; this asserts controller wiring + JSON contract (order_ref, PageResponse) and status codes.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
@Transactional
class BuyerAccountControllerTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var addressService: CustomerAddressService
    @field:MockitoBean private lateinit var orderService: EcomOrderService
    @field:MockitoBean private lateinit var storefrontService: StorefrontService
    // Mock the concrete impl (not the core interface) so CustomerController's concrete-typed
    // injection point still resolves to this same bean.
    @field:MockitoBean private lateinit var ecomCustomerService: EcomCustomerServiceImpl
    @field:MockitoBean private lateinit var invoiceEcomService: InvoiceEcomService
    @field:MockitoBean private lateinit var partyLedgerEcomService: PartyLedgerEcomService
    @field:MockitoBean private lateinit var workspaceMemberService: WorkspaceMemberService
    @field:MockitoBean private lateinit var ecomOrderKafkaProducer: EcomOrderKafkaProducer
    @field:MockitoBean private lateinit var ecomCatalogKafkaConsumer: EcomCatalogKafkaConsumer
    @field:MockitoBean private lateinit var ecomOrderStatusKafkaConsumer: EcomOrderStatusKafkaConsumer

    private val slug = "my-shop"

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        whenever(storefrontService.getPublishedStorefrontBySlug(slug)).thenReturn(storefront())
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun storefront() = Storefront().apply {
        uid = "sf-1"; ownerId = "ws-1"; this.slug = "my-shop"; name = "Shop"; status = StorefrontStatus.PUBLISHED
    }

    private fun linked(party: String = "PARTY1") =
        whenever(ecomCustomerService.resolveLinkedCustomerId(eq("cust-1"), anyOrNull())).thenReturn(party)

    private fun summary(uid: String = "INV1", orderRefId: String? = "ORD9") = BuyerInvoiceSummary(
        invoiceUid = uid, invoiceNumber = "INV-$uid", invoiceDate = Instant.parse("2026-08-15T10:00:00Z"),
        status = "RAISED", total = BigDecimal("9207.50"), orderRefId = orderRefId,
    )

    // ── US1 invoice list ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /invoices - linked buyer sees finalized invoices with swapped order_ref")
    fun `invoice list linked`() {
        linked()
        whenever(invoiceEcomService.listBuyerInvoices(eq("PARTY1"), any())).thenReturn(PageImpl(listOf(summary())))
        whenever(orderService.findBuyerOrderRef("ORD9")).thenReturn("ECO-7")

        mockMvc.perform(get("/v1/ecom/account/invoices?storefront_slug=$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].invoice_number").value("INV-INV1"))
            .andExpect(jsonPath("$.data.content[0].status").value("RAISED"))
            .andExpect(jsonPath("$.data.content[0].order_ref").value("ECO-7"))
    }

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /invoices - unlinked buyer is forbidden")
    fun `invoice list unlinked 403`() {
        whenever(ecomCustomerService.resolveLinkedCustomerId(eq("cust-1"), anyOrNull())).thenReturn(null)

        mockMvc.perform(get("/v1/ecom/account/invoices?storefront_slug=$slug"))
            .andExpect(status().isForbidden)
    }

    // ── US2 invoice detail ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /invoices/{uid} - own finalized invoice returns detail + order_ref")
    fun `invoice detail ok`() {
        linked()
        whenever(invoiceEcomService.getBuyerInvoice(eq("INV1"), eq("PARTY1"))).thenReturn(
            BuyerInvoiceDetail(
                invoiceUid = "INV1", invoiceNumber = "INV-42", invoiceDate = Instant.parse("2026-08-15T10:00:00Z"),
                status = "RAISED", orderRefId = "ORD9",
                lines = listOf(BuyerInvoiceLine("Widget", BigDecimal.TEN, BigDecimal("900.0"), BigDecimal("9000.0"))),
                subtotal = BigDecimal("9000.0"), taxTotal = BigDecimal("207.5"), total = BigDecimal("9207.5"),
            ),
        )
        whenever(orderService.findBuyerOrderRef("ORD9")).thenReturn("ECO-7")

        mockMvc.perform(get("/v1/ecom/account/invoices/INV1?storefront_slug=$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.order_ref").value("ECO-7"))
            .andExpect(jsonPath("$.data.lines[0].description").value("Widget"))
            .andExpect(jsonPath("$.data.total").value(9207.5))
    }

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /invoices/{uid} - other party / draft returns 404")
    fun `invoice detail not found`() {
        linked()
        whenever(invoiceEcomService.getBuyerInvoice(eq("INVX"), eq("PARTY1"))).thenReturn(null)

        mockMvc.perform(get("/v1/ecom/account/invoices/INVX?storefront_slug=$slug"))
            .andExpect(status().isNotFound)
    }

    // ── US3/US4 order ↔ invoice ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /orders/{ref}/invoices - returns finalized invoices for the order")
    fun `order invoices`() {
        linked()
        whenever(orderService.getCustomerOrder("cust-1", "ECO-7")).thenReturn(order(managementRef = "ORD9"))
        whenever(invoiceEcomService.listInvoicesForOrder(eq("ORD9"), eq("PARTY1"))).thenReturn(listOf(summary()))
        whenever(orderService.findBuyerOrderRef("ORD9")).thenReturn("ECO-7")

        mockMvc.perform(get("/v1/ecom/account/orders/ECO-7/invoices?storefront_slug=$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].invoice_number").value("INV-INV1"))
            .andExpect(jsonPath("$.data[0].order_ref").value("ECO-7"))
    }

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /orders/{ref} - order detail carries invoices[] (non-ecom invoice -> null order_ref)")
    fun `order detail invoices`() {
        linked()
        whenever(orderService.getCustomerOrder("cust-1", "ECO-7")).thenReturn(order(managementRef = "ORD9"))
        whenever(invoiceEcomService.listInvoicesForOrder(eq("ORD9"), eq("PARTY1")))
            .thenReturn(listOf(summary(orderRefId = "ORD9")))
        whenever(orderService.findBuyerOrderRef("ORD9")).thenReturn(null) // non-ecom mapping

        mockMvc.perform(get("/v1/ecom/account/orders/ECO-7?storefront_slug=$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.invoices[0].invoice_number").value("INV-INV1"))
            .andExpect(jsonPath("$.data.invoices[0].order_ref").doesNotExist())
    }

    // ── US5 outstanding / US6 statement ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /outstanding - linked buyer sees balance + bills + aging")
    fun `outstanding ok`() {
        linked()
        whenever(partyLedgerEcomService.outstanding(eq("PARTY1"), any())).thenReturn(
            BuyerOutstandingResponse(
                currentBalance = BigDecimal("15420.00"), balanceDirection = "DR",
                openBills = listOf(
                    BuyerOpenBill("INV-42", Instant.parse("2026-08-15T10:00:00Z"), BigDecimal("9207.50"),
                        BigDecimal("9207.50"), Instant.parse("2026-09-14T10:00:00Z"), 0, "0-30"),
                ),
                aging = listOf(BuyerAgingBucket("0-30", BigDecimal("9207.50"))),
            ),
        )

        mockMvc.perform(get("/v1/ecom/account/outstanding?storefront_slug=$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.balance_direction").value("DR"))
            .andExpect(jsonPath("$.data.open_bills[0].aging_bucket").value("0-30"))
    }

    @Test
    @WithMockUser(username = "cust-1", roles = ["USER"])
    @DisplayName("GET /statement - passes window and returns running-balance lines")
    fun `statement ok`() {
        linked()
        whenever(partyLedgerEcomService.statement(eq("PARTY1"), anyOrNull(), anyOrNull())).thenReturn(
            BuyerStatementResponse(
                from = null, to = Instant.parse("2026-08-20T00:00:00Z"),
                openingBalance = BigDecimal.ZERO, openingDirection = "DR",
                lines = listOf(
                    BuyerStatementLine(Instant.parse("2026-08-15T10:00:00Z"), "INVOICE", "INV-42", null,
                        BigDecimal("9207.50"), BigDecimal.ZERO, BigDecimal("9207.50")),
                ),
                closingBalance = BigDecimal("9207.50"), closingDirection = "DR",
            ),
        )

        mockMvc.perform(get("/v1/ecom/account/statement?storefront_slug=$slug&from=2026-08-01T00:00:00Z"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.closing_balance").value(9207.50))
            .andExpect(jsonPath("$.data.lines[0].kind").value("INVOICE"))
    }

    private fun order(managementRef: String?) = EcomOrder().apply {
        uid = "eo-1"; ecomOrderRef = "ECO-7"; orderNumber = "ECO-7"; storefrontId = "sf-1"; workspaceId = "ws-1"
        customerId = "cust-1"; customerName = "Buyer"; customerEmail = "b@x.com"
        status = EcomOrderStatus.PLACED; managementOrderRef = managementRef
    }
}
