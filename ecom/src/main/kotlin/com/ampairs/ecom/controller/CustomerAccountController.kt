package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.service.BuyerOutstandingResponse
import com.ampairs.core.service.BuyerStatementResponse
import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.core.service.EcomLinkCandidate
import com.ampairs.core.service.InvoiceEcomService
import com.ampairs.core.service.PartyLedgerEcomService
import com.ampairs.ecom.domain.dto.BuyerInvoiceDetailResponse
import com.ampairs.ecom.domain.dto.BuyerInvoiceSummaryResponse
import com.ampairs.ecom.domain.dto.ConfirmLinkRequest
import com.ampairs.ecom.domain.dto.CustomerAddressRequest
import com.ampairs.ecom.domain.dto.CustomerAddressResponse
import com.ampairs.ecom.domain.dto.EcomOrderResponse
import com.ampairs.ecom.domain.dto.asAddressResponse
import com.ampairs.ecom.domain.dto.asEcomOrderResponse
import com.ampairs.ecom.domain.dto.toResponse
import com.ampairs.ecom.service.CustomerAddressService
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.ecom.service.StorefrontService
import com.ampairs.user.model.User
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/v1/ecom/account")
@PreAuthorize("isAuthenticated()")
class CustomerAccountController(
    private val addressService: CustomerAddressService,
    private val orderService: EcomOrderService,
    private val storefrontService: StorefrontService,
    private val ecomCustomerService: EcomCustomerService,
    private val invoiceEcomService: InvoiceEcomService,
    private val partyLedgerEcomService: PartyLedgerEcomService,
) {

    /**
     * The CRM accounts the signed-in buyer can order for (the "ordering for" picker). Typically 0 or
     * 1 (individual shopper); returns >1 for a buyer linked to multiple accounts (owner/manager/worker).
     */
    @GetMapping("/customers")
    fun getCustomers(
        @RequestParam("storefront_slug") storefrontSlug: String,
        authentication: Authentication,
    ): ApiResponse<List<EcomCustomerAccount>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            return ApiResponse.success(ecomCustomerService.listAccountsForUser(userId))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    /**
     * Read-only: is there a CRM customer in this workspace whose phone matches the signed-in buyer's
     * own (verified, JWT-sourced) phone? Never creates a link — the result is a candidate for
     * [confirmLink] to commit once the buyer approves it.
     */
    @GetMapping("/link-candidate")
    fun getLinkCandidate(
        @RequestParam("storefront_slug") storefrontSlug: String,
        authentication: Authentication,
    ): ApiResponse<EcomLinkCandidate?> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val phone = principalPhone(authentication)
            return ApiResponse.success(phone?.let { ecomCustomerService.findLinkCandidateByPhone(it) })
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    /**
     * Commits the buyer's link to a candidate returned by [getLinkCandidate]. Re-validated
     * server-side inside [EcomCustomerService.confirmLink] — the buyer can never link to an account
     * whose phone doesn't match their own, regardless of what the client sends.
     */
    @PostMapping("/link")
    fun confirmLink(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestBody @Valid request: ConfirmLinkRequest,
        authentication: Authentication,
    ): ApiResponse<EcomCustomerAccount> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            val principal = authentication.principal
            val name = if (principal is User) principal.getDisplayName() else userId
            val email = if (principal is User) principal.email else null
            val account = ecomCustomerService.confirmLink(userId, request.customerId, name, principalPhone(authentication), email)
            return ApiResponse.success(account)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    // The login's phone, sourced only from the authenticated principal — never from the request —
    // so a buyer can never probe or link against a phone number that isn't their own.
    private fun principalPhone(authentication: Authentication): String? =
        (authentication.principal as? User)?.phone?.takeIf { it.isNotBlank() }

    @GetMapping("/addresses")
    fun getAddresses(authentication: Authentication): ApiResponse<List<CustomerAddressResponse>> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.getAddresses(customerId).map { it.asAddressResponse() })
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    fun addAddress(
        @RequestBody @Valid request: CustomerAddressRequest,
        authentication: Authentication,
    ): ApiResponse<CustomerAddressResponse> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.addAddress(customerId, request).asAddressResponse())
    }

    @PutMapping("/addresses/{addressId}")
    fun updateAddress(
        @PathVariable addressId: String,
        @RequestBody @Valid request: CustomerAddressRequest,
        authentication: Authentication,
    ): ApiResponse<CustomerAddressResponse> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.updateAddress(customerId, addressId, request).asAddressResponse())
    }

    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAddress(
        @PathVariable addressId: String,
        authentication: Authentication,
    ) {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        addressService.deleteAddress(customerId, addressId)
    }

    @GetMapping("/orders")
    fun getOrders(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<PageResponse<EcomOrderResponse>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = AuthenticationHelper.getUserId(authentication)!!
            val page = orderService.getCustomerOrders(customerId, storefront.uid, pageable)
            return ApiResponse.success(PageResponse.from(page))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/orders/{ecomOrderRef}")
    fun getOrder(
        @PathVariable ecomOrderRef: String,
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        authentication: Authentication,
    ): ApiResponse<EcomOrderResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            val order = orderService.getCustomerOrder(userId, ecomOrderRef)
            // Order↔invoice link (spec 029): attach the finalized invoices raised for this order.
            // Order viewing does not require a CRM link, so an unlinked buyer just sees an empty list.
            val partyUid = ecomCustomerService.resolveLinkedCustomerId(userId, customerId)
            val invoices = partyUid
                ?.let { swapOrderRefs(invoiceEcomService.listInvoicesForOrder(order.managementOrderRef.orEmpty(), it), unpaidInvoiceUids(it)) }
                ?: emptyList()
            return ApiResponse.success(order.asEcomOrderResponse().copy(invoices = invoices))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    // ── Spec 029: buyer invoices, order↔invoice link, and money position ────────────────────────

    /** A linked buyer's finalized invoices, newest first (US1). */
    @GetMapping("/invoices")
    fun getInvoices(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<PageResponse<BuyerInvoiceSummaryResponse>> =
        withParty(authentication, storefrontSlug, customerId) { partyUid ->
            val unpaid = unpaidInvoiceUids(partyUid)
            val page = invoiceEcomService.listBuyerInvoices(partyUid, pageable)
                .map { it.toResponse(orderService.findBuyerOrderRef(it.orderRefId), paymentStatus(it.invoiceUid, unpaid)) }
            ApiResponse.success(PageResponse.from(page))
        }

    /** A single finalized invoice owned by the linked buyer (US2). Wrong party / draft → 404. */
    @GetMapping("/invoices/{invoiceUid}")
    fun getInvoice(
        @PathVariable invoiceUid: String,
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        authentication: Authentication,
    ): ApiResponse<BuyerInvoiceDetailResponse> =
        withParty(authentication, storefrontSlug, customerId) { partyUid ->
            val detail = invoiceEcomService.getBuyerInvoice(invoiceUid, partyUid)
                ?: throw NotFoundException("Invoice not found: $invoiceUid")
            val status = paymentStatus(detail.invoiceUid, unpaidInvoiceUids(partyUid))
            ApiResponse.success(detail.toResponse(orderService.findBuyerOrderRef(detail.orderRefId), status))
        }

    /** Finalized invoices raised for one of the buyer's orders (US3). */
    @GetMapping("/orders/{ecomOrderRef}/invoices")
    fun getOrderInvoices(
        @PathVariable ecomOrderRef: String,
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        authentication: Authentication,
    ): ApiResponse<List<BuyerInvoiceSummaryResponse>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            val order = orderService.getCustomerOrder(userId, ecomOrderRef)
            val partyUid = ecomCustomerService.resolveLinkedCustomerId(userId, customerId)
                ?: throw AccessDeniedException("NOT_LINKED")
            val invoices = swapOrderRefs(
                invoiceEcomService.listInvoicesForOrder(order.managementOrderRef.orEmpty(), partyUid),
                unpaidInvoiceUids(partyUid),
            )
            return ApiResponse.success(invoices)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    /** Current balance + open bills + aging for the linked buyer (US5). */
    @GetMapping("/outstanding")
    fun getOutstanding(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        authentication: Authentication,
    ): ApiResponse<BuyerOutstandingResponse> =
        withParty(authentication, storefrontSlug, customerId) { partyUid ->
            ApiResponse.success(partyLedgerEcomService.outstanding(partyUid, Instant.now()))
        }

    /** Running-balance statement of invoices + payments for the linked buyer (US6). */
    @GetMapping("/statement")
    fun getStatement(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestParam("customer_id", required = false) customerId: String?,
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?,
        authentication: Authentication,
    ): ApiResponse<BuyerStatementResponse> =
        withParty(authentication, storefrontSlug, customerId) { partyUid ->
            ApiResponse.success(partyLedgerEcomService.statement(partyUid, parseInstant(from), parseInstant(to)))
        }

    // A malformed from/to must be a 400, not a 500: IllegalArgumentException maps to BAD_REQUEST in
    // the global handler, whereas the raw DateTimeParseException would surface as an unexpected error.
    private fun parseInstant(raw: String?): Instant? =
        raw?.takeIf { it.isNotBlank() }?.let {
            try {
                Instant.parse(it)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("Invalid ISO-8601 instant '$it' — expected e.g. 2026-08-01T00:00:00Z", e)
            }
        }

    /**
     * Runs [block] under the storefront's tenant with the buyer resolved to a linked CRM party.
     * Throws [AccessDeniedException] (`NOT_LINKED`, → 403) when the login is not linked to any account.
     */
    private fun <T> withParty(
        authentication: Authentication,
        storefrontSlug: String,
        requestedCustomerId: String?,
        block: (partyUid: String) -> T,
    ): T {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            val partyUid = ecomCustomerService.resolveLinkedCustomerId(userId, requestedCustomerId)
                ?: throw AccessDeniedException("NOT_LINKED")
            return block(partyUid)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    // Replace each summary's raw workspace orderRefId with the buyer-facing storefront order ref, and
    // stamp its buyer-facing payment status ("Paid"/"Unpaid") from the party's open bills.
    private fun swapOrderRefs(
        summaries: List<com.ampairs.core.service.BuyerInvoiceSummary>,
        unpaidUids: Set<String>,
    ): List<BuyerInvoiceSummaryResponse> =
        summaries.map { it.toResponse(orderService.findBuyerOrderRef(it.orderRefId), paymentStatus(it.invoiceUid, unpaidUids)) }

    // The invoice uids that still carry an outstanding balance for this party (spec OQ-6). An invoice
    // appears in the party ledger's open bills (keyed by invoice uid) exactly while it is not fully paid.
    private fun unpaidInvoiceUids(partyUid: String): Set<String> =
        partyLedgerEcomService.outstanding(partyUid, Instant.now()).openBills.map { it.billUid }.toSet()

    private fun paymentStatus(invoiceUid: String, unpaidUids: Set<String>): String =
        if (invoiceUid in unpaidUids) "Unpaid" else "Paid"
}
