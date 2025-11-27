# Payment Gateway Integration Plan

## Overview

This document outlines the comprehensive payment gateway integration strategy for the Ampairs subscription system across all platforms:

- **Android**: Google Play Billing
- **iOS**: Apple App Store (In-App Purchase)
- **Desktop (Windows/Mac/Linux)**: Razorpay & Stripe (Web Checkout Flow)
- **Web**: Razorpay & Stripe (Checkout Flow)

## Architecture

### Current Infrastructure ✅

The following components are **already implemented** and ready to use:

1. **Payment Enums** (`SubscriptionEnums.kt`)
   - `PaymentProvider` (GOOGLE_PLAY, APP_STORE, RAZORPAY, STRIPE, MANUAL)
   - `PaymentStatus` (PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED, DISPUTED, CANCELLED)
   - `PaymentMethodType` (CARD, UPI, NET_BANKING, WALLET, BANK_TRANSFER, IN_APP_PURCHASE)

2. **Database Entities**
   - `PaymentTransaction` - Records all payment attempts and outcomes
   - `PaymentMethod` - Stores customer payment methods (cards, UPI, etc.)
   - `Subscription` - Links workspace to plan with payment provider details

3. **Service Layer**
   - `PaymentProviderService` interface - Contract for all payment providers
   - `PaymentOrchestrationService` - Orchestrates payment operations across providers
   - `SubscriptionService` - Manages subscription lifecycle
   - `BillingService` - Records transactions and generates invoices

4. **Data Models**
   - `PurchaseVerificationResult` - Mobile purchase verification response
   - `ProviderSubscriptionResult` - Subscription creation result
   - `ProviderSubscriptionStatus` - Provider subscription status

### What Needs Implementation 🔨

1. **Payment Provider Implementations**
   - Google Play Billing service implementation
   - Apple App Store service implementation
   - Razorpay service implementation
   - Stripe service implementation

2. **Webhook Handlers**
   - Google Play Real-Time Developer Notifications
   - Apple App Store Server Notifications
   - Razorpay Webhooks
   - Stripe Webhooks

3. **Controllers**
   - Payment initiation endpoints
   - Purchase verification endpoints
   - Webhook receiver endpoints

---

## 1. Google Play Billing (Android)

### Implementation Components

#### 1.1 Google Play Billing Service

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/provider/GooglePlayBillingService.kt`

```kotlin
@Service
class GooglePlayBillingService(
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) : PaymentProviderService {

    override val provider = PaymentProvider.GOOGLE_PLAY

    private lateinit var publisher: AndroidPublisher

    @PostConstruct
    fun init() {
        // Initialize Google Play Developer API
        val credential = GoogleCredential.fromStream(
            // Load service account JSON from config
        ).createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))

        publisher = AndroidPublisher.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Ampairs").build()
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        // Call Google Play Developer API to verify purchase token
        val packageName = "com.ampairs.app" // From config
        val purchase = publisher.purchases()
            .subscriptions()
            .get(packageName, request.productId, request.purchaseToken)
            .execute()

        // Validate subscription state
        val valid = purchase.paymentState == 1 // Payment received
        val autoRenewing = purchase.autoRenewing ?: false

        // Map Google Play product ID to plan code
        val planCode = mapProductIdToPlanCode(request.productId)
        val billingCycle = mapProductIdToBillingCycle(request.productId)

        return PurchaseVerificationResult(
            valid = valid,
            planCode = planCode,
            billingCycle = billingCycle,
            externalSubscriptionId = purchase.orderId,
            externalCustomerId = null,
            orderId = purchase.orderId,
            purchaseTime = Instant.ofEpochMilli(purchase.startTimeMillis),
            expiryTime = Instant.ofEpochMilli(purchase.expiryTimeMillis),
            autoRenewing = autoRenewing
        )
    }

    override suspend fun createSubscription(...): ProviderSubscriptionResult {
        // Not applicable for Google Play - purchases happen on device
        throw UnsupportedOperationException("Google Play subscriptions are created on device")
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        // Google Play subscriptions are cancelled by user in Play Store
        // This method is for backend-initiated cancellations (rare)
        return true
    }

    // Other interface methods...
}
```

#### 1.2 Google Play Webhook Handler

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/webhook/GooglePlayWebhookController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/webhooks/google-play")
class GooglePlayWebhookController(
    private val orchestrationService: PaymentOrchestrationService,
    private val googlePlayService: GooglePlayBillingService
) {

    @PostMapping("/rtdn")
    fun handleRealTimeNotification(@RequestBody payload: String): ResponseEntity<String> {
        // Parse Real-Time Developer Notification
        // Verify authenticity

        // Handle different notification types:
        // - SUBSCRIPTION_PURCHASED
        // - SUBSCRIPTION_RENEWED
        // - SUBSCRIPTION_CANCELED
        // - SUBSCRIPTION_EXPIRED
        // - SUBSCRIPTION_PAUSED
        // - SUBSCRIPTION_RESUMED

        return ResponseEntity.ok("OK")
    }
}
```

#### 1.3 Android Client Integration

**Kotlin Multiplatform App** (`ampairs-mp-app/android`):

```kotlin
// In Android-specific code
class GooglePlayBillingManager(
    private val activity: Activity,
    private val apiClient: SubscriptionApiClient
) {
    private lateinit var billingClient: BillingClient

    fun initializeBilling() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Ready to query purchases
                    queryAvailableProducts()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }

    suspend fun purchasePlan(planCode: String, billingCycle: BillingCycle) {
        val productId = getGooglePlayProductId(planCode, billingCycle)

        val productDetailsParams = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(productDetailsParams))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.firstOrNull()?.let { productDetails ->
                    launchBillingFlow(productDetails)
                }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Verify purchase with backend
            val response = apiClient.verifyPurchase(
                VerifyPurchaseRequest(
                    provider = PaymentProvider.GOOGLE_PLAY,
                    purchaseToken = purchase.purchaseToken,
                    productId = purchase.products.first(),
                    orderId = purchase.orderId
                )
            )

            // Acknowledge purchase
            if (response.success) {
                acknowledgePurchase(purchase.purchaseToken)
            }
        }
    }
}
```

### Configuration

**application.yml**:
```yaml
google-play:
  package-name: com.ampairs.app
  service-account-json: ${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON}
  product-mappings:
    starter-monthly: "ampairs_starter_monthly"
    starter-annual: "ampairs_starter_annual"
    professional-monthly: "ampairs_professional_monthly"
    professional-annual: "ampairs_professional_annual"
    enterprise-monthly: "ampairs_enterprise_monthly"
    enterprise-annual: "ampairs_enterprise_annual"
```

---

## 2. Apple App Store (iOS)

### Implementation Components

#### 2.1 Apple App Store Service

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/provider/AppleAppStoreService.kt`

```kotlin
@Service
class AppleAppStoreService(
    @Value("\${apple-app-store.shared-secret}") private val sharedSecret: String,
    @Value("\${apple-app-store.bundle-id}") private val bundleId: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) : PaymentProviderService {

    override val provider = PaymentProvider.APP_STORE

    private val verifyReceiptUrl = "https://buy.itunes.apple.com/verifyReceipt"
    private val sandboxVerifyReceiptUrl = "https://sandbox.itunes.apple.com/verifyReceipt"

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        // Call Apple's verifyReceipt API
        val receiptData = request.purchaseToken // Base64 encoded receipt

        val response = verifyReceiptWithApple(receiptData, production = true)

        // If production fails with 21007, retry with sandbox
        val finalResponse = if (response.status == 21007) {
            verifyReceiptWithApple(receiptData, production = false)
        } else {
            response
        }

        if (finalResponse.status != 0) {
            return PurchaseVerificationResult(
                valid = false,
                planCode = null,
                billingCycle = null,
                externalSubscriptionId = null,
                externalCustomerId = null,
                orderId = null,
                purchaseTime = null,
                expiryTime = null,
                errorMessage = "Invalid receipt: ${finalResponse.status}"
            )
        }

        // Find latest subscription in receipt
        val latestReceipt = finalResponse.latestReceiptInfo?.firstOrNull()
        val productId = latestReceipt?.productId
        val originalTransactionId = latestReceipt?.originalTransactionId

        val planCode = mapProductIdToPlanCode(productId)
        val billingCycle = mapProductIdToBillingCycle(productId)

        return PurchaseVerificationResult(
            valid = true,
            planCode = planCode,
            billingCycle = billingCycle,
            externalSubscriptionId = originalTransactionId,
            externalCustomerId = null,
            orderId = latestReceipt?.transactionId,
            purchaseTime = latestReceipt?.purchaseDate?.let { Instant.ofEpochMilli(it) },
            expiryTime = latestReceipt?.expiresDate?.let { Instant.ofEpochMilli(it) },
            autoRenewing = finalResponse.pendingRenewalInfo?.firstOrNull()?.autoRenewStatus == "1"
        )
    }

    private suspend fun verifyReceiptWithApple(
        receiptData: String,
        production: Boolean
    ): AppleReceiptResponse {
        val url = if (production) verifyReceiptUrl else sandboxVerifyReceiptUrl

        val requestBody = mapOf(
            "receipt-data" to receiptData,
            "password" to sharedSecret,
            "exclude-old-transactions" to true
        )

        // Make HTTP POST request
        // Parse response
    }

    override suspend fun createSubscription(...): ProviderSubscriptionResult {
        throw UnsupportedOperationException("App Store subscriptions are created on device")
    }

    // Other interface methods...
}

data class AppleReceiptResponse(
    val status: Int,
    val latestReceiptInfo: List<AppleReceiptInfo>?,
    val pendingRenewalInfo: List<ApplePendingRenewalInfo>?
)
```

#### 2.2 Apple App Store Webhook Handler

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/webhook/AppleAppStoreWebhookController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/webhooks/apple-app-store")
class AppleAppStoreWebhookController(
    private val orchestrationService: PaymentOrchestrationService,
    private val appleService: AppleAppStoreService
) {

    @PostMapping("/notifications")
    fun handleServerNotification(@RequestBody payload: String): ResponseEntity<String> {
        // Parse Apple Server-to-Server notification
        // Verify signature (using Apple's public key)

        // Handle notification types:
        // - INITIAL_BUY
        // - DID_RENEW
        // - DID_FAIL_TO_RENEW
        // - DID_CHANGE_RENEWAL_STATUS
        // - CANCEL
        // - REFUND

        return ResponseEntity.ok("OK")
    }
}
```

#### 2.3 iOS Client Integration

**Swift/KMM Code** (`ampairs-mp-app/ios`):

```swift
import StoreKit

class AppStoreManager: NSObject, SKPaymentTransactionObserver {
    private let apiClient: SubscriptionApiClient

    func initializeStoreKit() {
        SKPaymentQueue.default().add(self)
        loadProducts()
    }

    func purchasePlan(planCode: String, billingCycle: BillingCycle) {
        let productId = getAppStoreProductId(planCode: planCode, billingCycle: billingCycle)

        // Request product
        let request = SKProductsRequest(productIdentifiers: [productId])
        request.delegate = self
        request.start()
    }

    func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for transaction in transactions {
            switch transaction.transactionState {
            case .purchased:
                handlePurchased(transaction)
            case .failed:
                SKPaymentQueue.default().finishTransaction(transaction)
            case .restored:
                handleRestored(transaction)
            default:
                break
            }
        }
    }

    private func handlePurchased(_ transaction: SKPaymentTransaction) {
        // Get receipt
        guard let receiptURL = Bundle.main.appStoreReceiptURL,
              let receiptData = try? Data(contentsOf: receiptURL) else {
            return
        }

        let receiptString = receiptData.base64EncodedString()

        // Verify with backend
        Task {
            let response = try await apiClient.verifyPurchase(
                VerifyPurchaseRequest(
                    provider: .APP_STORE,
                    purchaseToken: receiptString,
                    productId: transaction.payment.productIdentifier,
                    orderId: transaction.transactionIdentifier
                )
            )

            if response.success {
                SKPaymentQueue.default().finishTransaction(transaction)
            }
        }
    }
}
```

### Configuration

**application.yml**:
```yaml
apple-app-store:
  bundle-id: com.ampairs.app
  shared-secret: ${APPLE_SHARED_SECRET}
  product-mappings:
    starter-monthly: "com.ampairs.starter.monthly"
    starter-annual: "com.ampairs.starter.annual"
    professional-monthly: "com.ampairs.professional.monthly"
    professional-annual: "com.ampairs.professional.annual"
    enterprise-monthly: "com.ampairs.enterprise.monthly"
    enterprise-annual: "com.ampairs.enterprise.annual"
```

---

## 3. Razorpay (Desktop & Web)

### Implementation Components

#### 3.1 Razorpay Service

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/provider/RazorpayService.kt`

```kotlin
@Service
class RazorpayService(
    @Value("\${razorpay.key-id}") private val keyId: String,
    @Value("\${razorpay.key-secret}") private val keySecret: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) : PaymentProviderService {

    override val provider = PaymentProvider.RAZORPAY

    private val razorpayClient: RazorpayClient by lazy {
        RazorpayClient(keyId, keySecret)
    }

    override suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult {
        // Get or create Razorpay customer
        val rzpCustomer = if (customerId != null) {
            razorpayClient.customers.fetch(customerId)
        } else {
            createRazorpayCustomer(workspaceId)
        }

        // Get Razorpay plan ID from subscription plan
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        val razorpayPlanId = when (billingCycle) {
            BillingCycle.MONTHLY -> plan.razorpayPlanIdMonthly
            BillingCycle.ANNUAL -> plan.razorpayPlanIdAnnual
            else -> throw IllegalArgumentException("Unsupported billing cycle for Razorpay")
        } ?: throw IllegalStateException("Razorpay plan ID not configured")

        // Create Razorpay subscription
        val subscriptionRequest = JSONObject().apply {
            put("plan_id", razorpayPlanId)
            put("customer_id", rzpCustomer.get("id"))
            put("total_count", 120) // 10 years for monthly
            put("notify_info", JSONObject().apply {
                put("notify_email", rzpCustomer.get("email"))
            })
        }

        val rzpSubscription = razorpayClient.subscriptions.create(subscriptionRequest)

        return ProviderSubscriptionResult(
            success = true,
            externalSubscriptionId = rzpSubscription.get("id") as String,
            externalCustomerId = rzpCustomer.get("id") as String,
            checkoutUrl = rzpSubscription.get("short_url") as String?, // For hosted checkout
            clientSecret = null,
            orderId = null,
            currentPeriodStart = Instant.now(),
            currentPeriodEnd = null // Will be set after first payment
        )
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        // For Razorpay, verification happens via webhook
        // This method is primarily for mobile in-app purchases
        throw UnsupportedOperationException("Use webhook for Razorpay payment verification")
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        val cancelRequest = JSONObject().apply {
            put("cancel_at_cycle_end", !immediate)
        }

        razorpayClient.subscriptions.cancel(externalSubscriptionId, cancelRequest)
        return true
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        val expectedSignature = HmacUtils.hmacSha256Hex(keySecret, payload)
        return expectedSignature == signature
    }

    private fun createRazorpayCustomer(workspaceId: String): Customer {
        // Fetch workspace/user details
        // Create Razorpay customer
    }
}
```

#### 3.2 Razorpay Webhook Handler

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/webhook/RazorpayWebhookController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
class RazorpayWebhookController(
    private val orchestrationService: PaymentOrchestrationService,
    private val razorpayService: RazorpayService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-Razorpay-Signature") signature: String
    ): ResponseEntity<String> {
        // Verify webhook signature
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            logger.warn("Invalid Razorpay webhook signature")
            return ResponseEntity.status(401).body("Invalid signature")
        }

        val event = JSONObject(payload)
        val eventType = event.getString("event")
        val eventPayload = event.getJSONObject("payload")

        when (eventType) {
            "subscription.activated" -> handleSubscriptionActivated(eventPayload)
            "subscription.charged" -> handleSubscriptionCharged(eventPayload)
            "subscription.pending" -> handleSubscriptionPending(eventPayload)
            "subscription.halted" -> handleSubscriptionHalted(eventPayload)
            "subscription.cancelled" -> handleSubscriptionCancelled(eventPayload)
            "subscription.completed" -> handleSubscriptionCompleted(eventPayload)
            "payment.failed" -> handlePaymentFailed(eventPayload)
            else -> logger.info("Unhandled Razorpay event: $eventType")
        }

        return ResponseEntity.ok("OK")
    }

    private fun handleSubscriptionCharged(payload: JSONObject) {
        val subscription = payload.getJSONObject("subscription").getJSONObject("entity")
        val payment = payload.getJSONObject("payment").getJSONObject("entity")

        orchestrationService.handleRenewal(
            provider = PaymentProvider.RAZORPAY,
            externalSubscriptionId = subscription.getString("id"),
            orderId = payment.getString("id"),
            amount = BigDecimal(payment.getInt("amount")).divide(BigDecimal(100)), // Convert paise to rupees
            currency = payment.getString("currency"),
            periodStart = Instant.ofEpochSecond(subscription.getLong("current_start")),
            periodEnd = Instant.ofEpochSecond(subscription.getLong("current_end"))
        )
    }

    private fun handlePaymentFailed(payload: JSONObject) {
        val payment = payload.getJSONObject("payment").getJSONObject("entity")
        val subscriptionId = payment.optString("subscription_id")

        if (subscriptionId.isNotEmpty()) {
            orchestrationService.handlePaymentFailure(
                provider = PaymentProvider.RAZORPAY,
                externalSubscriptionId = subscriptionId,
                failureCode = payment.optString("error_code"),
                failureReason = payment.optString("error_description")
            )
        }
    }

    // Other event handlers...
}
```

#### 3.3 Desktop Client Integration

**Kotlin Multiplatform Desktop**:

```kotlin
// In Desktop-specific code
class RazorpayCheckoutManager(
    private val apiClient: SubscriptionApiClient
) {
    suspend fun initiatePurchase(
        planCode: String,
        billingCycle: BillingCycle
    ): String {
        // Call backend to create Razorpay subscription
        val response = apiClient.initiatePurchase(
            InitiatePurchaseRequest(
                planCode = planCode,
                billingCycle = billingCycle,
                currency = "INR",
                provider = PaymentProvider.RAZORPAY
            )
        )

        // Open checkout URL in system browser
        val checkoutUrl = response.checkoutUrl ?: throw Exception("No checkout URL")
        Desktop.getDesktop().browse(URI(checkoutUrl))

        // Start polling for payment completion
        return response.subscriptionId
    }

    suspend fun pollPaymentStatus(subscriptionId: String): SubscriptionResponse {
        // Poll backend every 3 seconds for subscription status
        repeat(60) { // Poll for 3 minutes
            delay(3000)

            val subscription = apiClient.getSubscription()
            if (subscription.status == SubscriptionStatus.ACTIVE) {
                return subscription
            }
        }

        throw TimeoutException("Payment verification timeout")
    }
}
```

### Configuration

**application.yml**:
```yaml
razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
```

---

## 4. Stripe (Desktop & Web)

### Implementation Components

#### 4.1 Stripe Service

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/provider/StripeService.kt`

```kotlin
@Service
class StripeService(
    @Value("\${stripe.secret-key}") secretKey: String,
    @Value("\${stripe.webhook-secret}") private val webhookSecret: String,
    @Value("\${stripe.success-url}") private val successUrl: String,
    @Value("\${stripe.cancel-url}") private val cancelUrl: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) : PaymentProviderService {

    override val provider = PaymentProvider.STRIPE

    init {
        Stripe.apiKey = secretKey
    }

    override suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult {
        // Get or create Stripe customer
        val stripeCustomer = if (customerId != null) {
            Customer.retrieve(customerId)
        } else {
            createStripeCustomer(workspaceId)
        }

        // Get Stripe price ID from subscription plan
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        val stripePriceId = when (billingCycle) {
            BillingCycle.MONTHLY -> plan.stripePriceIdMonthly
            BillingCycle.ANNUAL -> plan.stripePriceIdAnnual
            else -> throw IllegalArgumentException("Unsupported billing cycle for Stripe")
        } ?: throw IllegalStateException("Stripe price ID not configured")

        // Create Checkout Session
        val sessionParams = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(stripeCustomer.id)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(stripePriceId)
                    .setQuantity(1L)
                    .build()
            )
            .setSuccessUrl("$successUrl?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(cancelUrl)
            .putMetadata("workspace_id", workspaceId)
            .putMetadata("plan_code", planCode)
            .build()

        val session = Session.create(sessionParams)

        return ProviderSubscriptionResult(
            success = true,
            externalSubscriptionId = session.id,
            externalCustomerId = stripeCustomer.id,
            checkoutUrl = session.url,
            clientSecret = session.clientSecret,
            orderId = null,
            currentPeriodStart = null,
            currentPeriodEnd = null
        )
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        throw UnsupportedOperationException("Use webhook for Stripe payment verification")
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        val subscription = Subscription.retrieve(externalSubscriptionId)

        if (immediate) {
            subscription.cancel()
        } else {
            val updateParams = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build()
            subscription.update(updateParams)
        }

        return true
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        return try {
            Webhook.constructEvent(payload, signature, webhookSecret)
            true
        } catch (e: SignatureVerificationException) {
            false
        }
    }

    private fun createStripeCustomer(workspaceId: String): Customer {
        // Fetch workspace/user details
        // Create Stripe customer
    }
}
```

#### 4.2 Stripe Webhook Handler

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/webhook/StripeWebhookController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
class StripeWebhookController(
    private val orchestrationService: PaymentOrchestrationService,
    private val stripeService: StripeService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String
    ): ResponseEntity<String> {
        // Verify webhook signature
        val event = try {
            Webhook.constructEvent(payload, signature, stripeService.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.warn("Invalid Stripe webhook signature")
            return ResponseEntity.status(401).body("Invalid signature")
        }

        when (event.type) {
            "checkout.session.completed" -> handleCheckoutCompleted(event)
            "customer.subscription.created" -> handleSubscriptionCreated(event)
            "customer.subscription.updated" -> handleSubscriptionUpdated(event)
            "customer.subscription.deleted" -> handleSubscriptionDeleted(event)
            "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event)
            "invoice.payment_failed" -> handleInvoicePaymentFailed(event)
            else -> logger.info("Unhandled Stripe event: ${event.type}")
        }

        return ResponseEntity.ok("OK")
    }

    private fun handleCheckoutCompleted(event: Event) {
        val session = event.dataObjectDeserializer.`object`.get() as Session
        val subscriptionId = session.subscription
        val customerId = session.customer
        val workspaceId = session.metadata["workspace_id"] ?: return
        val planCode = session.metadata["plan_code"] ?: return

        // Activate subscription in our system
        // This will be called after first successful payment
    }

    private fun handleInvoicePaymentSucceeded(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
        val subscriptionId = invoice.subscription ?: return

        orchestrationService.handleRenewal(
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subscriptionId,
            orderId = invoice.charge,
            amount = BigDecimal(invoice.amountPaid).divide(BigDecimal(100)),
            currency = invoice.currency.uppercase(),
            periodStart = Instant.ofEpochSecond(invoice.periodStart),
            periodEnd = Instant.ofEpochSecond(invoice.periodEnd)
        )
    }

    private fun handleInvoicePaymentFailed(event: Event) {
        val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
        val subscriptionId = invoice.subscription ?: return

        orchestrationService.handlePaymentFailure(
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subscriptionId,
            failureCode = invoice.charge?.let { Charge.retrieve(it).failureCode },
            failureReason = invoice.charge?.let { Charge.retrieve(it).failureMessage }
        )
    }

    // Other event handlers...
}
```

### Configuration

**application.yml**:
```yaml
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  publishable-key: ${STRIPE_PUBLISHABLE_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
  success-url: https://app.ampairs.com/subscription/success
  cancel-url: https://app.ampairs.com/subscription/cancelled
```

---

## 5. Unified Payment Controller

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/controller/PaymentController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/subscription/payment")
class PaymentController(
    private val orchestrationService: PaymentOrchestrationService
) {

    /**
     * Initiate purchase for desktop/web (returns checkout URL)
     */
    @PostMapping("/initiate")
    fun initiatePurchase(
        @RequestBody request: InitiatePurchaseRequest
    ): ApiResponse<InitiatePurchaseResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            orchestrationService.initiatePurchase(workspaceId, request, request.provider)
        }

        return ApiResponse.success(response)
    }

    /**
     * Verify mobile in-app purchase
     */
    @PostMapping("/verify")
    fun verifyPurchase(
        @RequestBody request: VerifyPurchaseRequest
    ): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            orchestrationService.verifyPurchase(workspaceId, request)
        }

        return ApiResponse.success(response)
    }
}
```

---

## 6. Dependencies

### build.gradle.kts

```kotlin
dependencies {
    // Google Play Billing verification
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20230110-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // Razorpay
    implementation("com.razorpay:razorpay-java:1.4.3")

    // Stripe
    implementation("com.stripe:stripe-java:24.0.0")

    // HTTP client for Apple API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing
    implementation("org.json:json:20230618")
}
```

---

## 7. Implementation Checklist

### Phase 1: Backend Foundation ✅
- [x] Payment provider interface defined
- [x] Payment orchestration service implemented
- [x] Database entities created
- [x] DTOs and request/response models defined

### Phase 2: Google Play Billing (Android)
- [ ] Implement GooglePlayBillingService
- [ ] Create Google Play webhook controller
- [ ] Configure Google Play service account
- [ ] Add product mappings to database
- [ ] Integrate in Android app (KMM)
- [ ] Test purchase flow end-to-end
- [ ] Test webhook notifications

### Phase 3: Apple App Store (iOS)
- [ ] Implement AppleAppStoreService
- [ ] Create App Store webhook controller
- [ ] Configure App Store shared secret
- [ ] Add product mappings to database
- [ ] Integrate in iOS app (KMM)
- [ ] Test purchase flow end-to-end
- [ ] Test server notifications

### Phase 4: Razorpay (Desktop/Web)
- [ ] Implement RazorpayService
- [ ] Create Razorpay webhook controller
- [ ] Configure Razorpay credentials
- [ ] Create Razorpay plans via Dashboard
- [ ] Add plan IDs to database
- [ ] Integrate in Desktop app (KMM)
- [ ] Integrate in Web app (Angular)
- [ ] Test checkout flow end-to-end
- [ ] Test webhooks

### Phase 5: Stripe (Desktop/Web)
- [ ] Implement StripeService
- [ ] Create Stripe webhook controller
- [ ] Configure Stripe credentials
- [ ] Create Stripe products/prices
- [ ] Add price IDs to database
- [ ] Integrate in Desktop app (KMM)
- [ ] Integrate in Web app (Angular)
- [ ] Test checkout flow end-to-end
- [ ] Test webhooks

### Phase 6: Testing & Validation
- [ ] Unit tests for all payment services
- [ ] Integration tests for payment flows
- [ ] Webhook signature validation tests
- [ ] Test payment failures and retries
- [ ] Test subscription renewals
- [ ] Test subscription cancellations
- [ ] Load testing for webhook endpoints
- [ ] Security audit of payment flows

### Phase 7: Monitoring & Observability
- [ ] Add metrics for payment success/failure rates
- [ ] Set up alerts for payment failures
- [ ] Log all payment transactions
- [ ] Dashboard for subscription analytics
- [ ] Revenue tracking and reporting

---

## 8. Security Considerations

1. **Webhook Security**
   - ✅ Always verify webhook signatures
   - ✅ Use HTTPS for all webhook endpoints
   - ✅ Implement rate limiting on webhook endpoints
   - ✅ Log all webhook events for audit

2. **Credentials Management**
   - ✅ Store all API keys in environment variables
   - ✅ Never commit secrets to version control
   - ✅ Rotate keys periodically
   - ✅ Use different credentials for staging/production

3. **Purchase Verification**
   - ✅ Always verify purchases on backend
   - ✅ Never trust client-side purchase validation
   - ✅ Implement idempotency for purchase verification
   - ✅ Prevent replay attacks

4. **PCI Compliance**
   - ✅ Never store card details
   - ✅ Use payment provider's tokenization
   - ✅ Redirect to provider's checkout page
   - ✅ Ensure HTTPS everywhere

---

## 9. Testing Strategy

### Sandbox/Test Environments

1. **Google Play Billing**
   - Use test accounts for testing
   - Create test subscriptions in Play Console
   - Test with real Google Play app in alpha/internal testing

2. **Apple App Store**
   - Use sandbox environment
   - Create sandbox test accounts
   - Test with real iOS app via TestFlight

3. **Razorpay**
   - Use test mode API keys
   - Test cards: 4111 1111 1111 1111
   - Use webhook simulator

4. **Stripe**
   - Use test mode API keys
   - Test cards: 4242 4242 4242 4242
   - Use Stripe CLI for webhook testing

### Test Scenarios

- [ ] Successful purchase
- [ ] Failed payment (card declined)
- [ ] Subscription renewal
- [ ] Subscription cancellation
- [ ] Plan upgrade/downgrade
- [ ] Refund processing
- [ ] Webhook delivery failure and retry
- [ ] Duplicate webhook handling (idempotency)
- [ ] Concurrent purchase attempts
- [ ] Invalid/expired purchase tokens

---

## 10. Deployment Checklist

### Pre-Production
- [ ] Configure all payment provider accounts
- [ ] Set up production API keys
- [ ] Configure webhook URLs in provider dashboards
- [ ] Test webhooks from production to staging
- [ ] Load test webhook endpoints
- [ ] Security audit of payment integration

### Production Launch
- [ ] Deploy payment service with feature flag OFF
- [ ] Verify webhook endpoints are reachable
- [ ] Enable payments for beta users
- [ ] Monitor error rates and payment success
- [ ] Gradual rollout to all users

### Post-Launch
- [ ] Set up 24/7 monitoring
- [ ] Alert on payment failures > threshold
- [ ] Daily revenue reconciliation
- [ ] Weekly payment provider status check
- [ ] Monthly security audit

---

## Summary

This plan provides a complete roadmap for integrating all four payment gateways:

✅ **Google Play Billing** - Native Android in-app subscriptions
✅ **Apple App Store** - Native iOS in-app subscriptions
✅ **Razorpay** - Desktop/Web checkout with UPI, Cards, Net Banking
✅ **Stripe** - Desktop/Web checkout with international card support

All four integrations will:
- Use the existing `PaymentProviderService` interface
- Be orchestrated by `PaymentOrchestrationService`
- Store transactions in `PaymentTransaction` table
- Update subscription status automatically
- Handle webhooks for automated renewal/cancellation
- Support immediate and scheduled cancellations
- Auto-downgrade to FREE plan on cancellation/expiry

The architecture is provider-agnostic, making it easy to add more payment providers in the future.
