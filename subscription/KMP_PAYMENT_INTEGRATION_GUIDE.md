# Kotlin Multiplatform Payment Integration Guide

This guide explains how to integrate Google Play, Apple App Store, and Razorpay payments in the Ampairs KMP application (Android, iOS, Desktop).

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    KMP Application Layer                     │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐             │
│  │  Android   │  │    iOS     │  │  Desktop   │             │
│  │ Google Play│  │ App Store  │  │  Razorpay  │             │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘             │
│        │                │                │                    │
│        └────────────────┼────────────────┘                    │
│                         │                                     │
│            ┌────────────▼────────────┐                        │
│            │  Common Payment Module  │                        │
│            │  (expect/actual pattern)│                        │
│            └────────────┬────────────┘                        │
└─────────────────────────┼──────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  Backend API Calls    │
              │  (Verification/Sync)  │
              └───────────────────────┘
```

---

## Part 1: Google Play Integration (Android)

### Prerequisites

1. **Google Play Console Setup**:
   - Create app in Google Play Console
   - Enable in-app billing
   - Create subscription products matching backend product IDs
   - Set up test accounts

2. **Backend Configuration**:
   - Service account JSON configured
   - Product IDs in database match Google Play product IDs

### Step 1: Add Dependencies

**File**: `ampairs-mp-app/build.gradle.kts` (Android source set)

```kotlin
androidMain {
    dependencies {
        // Google Play Billing Library
        implementation("com.android.billingclient:billing-ktx:6.1.0")

        // Coroutines for async operations
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}
```

### Step 2: Create Common Interface

**File**: `ampairs-mp-app/shared/src/commonMain/kotlin/com/ampairs/payment/PaymentService.kt`

```kotlin
package com.ampairs.payment

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for payment operations across platforms
 */
interface PaymentService {

    /**
     * Initialize payment service
     */
    suspend fun initialize()

    /**
     * Fetch available subscription plans
     */
    suspend fun getAvailablePlans(): Result<List<SubscriptionPlan>>

    /**
     * Purchase a subscription
     */
    suspend fun purchaseSubscription(planId: String): Result<PurchaseResult>

    /**
     * Restore purchases
     */
    suspend fun restorePurchases(): Result<List<PurchaseResult>>

    /**
     * Check subscription status
     */
    suspend fun getSubscriptionStatus(): Result<SubscriptionStatus>

    /**
     * Listen to purchase updates
     */
    fun observePurchaseUpdates(): Flow<PurchaseUpdate>
}

data class SubscriptionPlan(
    val planId: String,
    val name: String,
    val price: String,
    val billingCycle: BillingCycle,
    val features: List<String>
)

data class PurchaseResult(
    val purchaseToken: String,
    val productId: String,
    val orderId: String,
    val purchaseTime: Long,
    val isAcknowledged: Boolean
)

data class SubscriptionStatus(
    val isActive: Boolean,
    val planCode: String?,
    val expiresAt: Long?,
    val autoRenewing: Boolean
)

sealed class PurchaseUpdate {
    data class Success(val result: PurchaseResult) : PurchaseUpdate()
    data class Pending(val productId: String) : PurchaseUpdate()
    data class Failed(val error: String) : PurchaseUpdate()
}

enum class BillingCycle {
    MONTHLY, ANNUAL
}
```

### Step 3: Android Implementation

**File**: `ampairs-mp-app/shared/src/androidMain/kotlin/com/ampairs/payment/GooglePlayPaymentService.kt`

```kotlin
package com.ampairs.payment

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlayPaymentService(
    private val context: Context,
    private val activity: Activity
) : PaymentService, PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private val purchaseUpdates = MutableStateFlow<PurchaseUpdate?>(null)

    override suspend fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        // Connect to Google Play
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(Unit)
                    } else {
                        continuation.cancel(Exception("Billing setup failed: ${billingResult.debugMessage}"))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to reconnect on next operation
                }
            })
        }
    }

    override suspend fun getAvailablePlans(): Result<List<SubscriptionPlan>> {
        return try {
            // Query product details from Google Play
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("ampairs_professional_monthly")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("ampairs_professional_annual")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = suspendCancellableCoroutine<ProductDetailsResult> { continuation ->
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    continuation.resume(ProductDetailsResult(billingResult, productDetailsList))
                }
            }

            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val plans = productDetailsResult.productDetailsList.map { product ->
                    SubscriptionPlan(
                        planId = product.productId,
                        name = product.name,
                        price = product.subscriptionOfferDetails?.firstOrNull()
                            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                            ?.formattedPrice ?: "",
                        billingCycle = if (product.productId.contains("annual"))
                            BillingCycle.ANNUAL else BillingCycle.MONTHLY,
                        features = listOf() // Load from backend
                    )
                }
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to load plans: ${productDetailsResult.billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchaseSubscription(planId: String): Result<PurchaseResult> {
        return try {
            // Get product details
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(planId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = suspendCancellableCoroutine<ProductDetailsResult> { continuation ->
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    continuation.resume(ProductDetailsResult(billingResult, productDetailsList))
                }
            }

            val productDetails = productDetailsResult.productDetailsList.firstOrNull()
                ?: return Result.failure(Exception("Product not found"))

            // Launch billing flow
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return Result.failure(Exception("No offer available"))

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(PurchaseResult("", planId, "", 0, false)) // Will be updated in callback
            } else {
                Result.failure(Exception("Purchase failed: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<List<PurchaseResult>> {
        return try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val purchasesResult = billingClient.queryPurchasesAsync(params)

            if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val results = purchasesResult.purchasesList.map { purchase ->
                    PurchaseResult(
                        purchaseToken = purchase.purchaseToken,
                        productId = purchase.products.firstOrNull() ?: "",
                        orderId = purchase.orderId ?: "",
                        purchaseTime = purchase.purchaseTime,
                        isAcknowledged = purchase.isAcknowledged
                    )
                }
                Result.success(results)
            } else {
                Result.failure(Exception("Restore failed: ${purchasesResult.billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        // Call backend API to get subscription status
        // This ensures server-side validation
        return Result.success(SubscriptionStatus(false, null, null, false))
    }

    override fun observePurchaseUpdates(): Flow<PurchaseUpdate> {
        return purchaseUpdates as Flow<PurchaseUpdate>
    }

    // PurchasesUpdatedListener callback
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    // Verify purchase with backend
                    val result = PurchaseResult(
                        purchaseToken = purchase.purchaseToken,
                        productId = purchase.products.firstOrNull() ?: "",
                        orderId = purchase.orderId ?: "",
                        purchaseTime = purchase.purchaseTime,
                        isAcknowledged = purchase.isAcknowledged
                    )
                    purchaseUpdates.value = PurchaseUpdate.Success(result)

                    // Acknowledge purchase
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseUpdates.value = PurchaseUpdate.Failed("User canceled")
            }
            else -> {
                purchaseUpdates.value = PurchaseUpdate.Failed(billingResult.debugMessage)
            }
        }
    }
}

private data class ProductDetailsResult(
    val billingResult: BillingResult,
    val productDetailsList: List<ProductDetails>
)
```

### Step 4: Verify Purchase with Backend

**File**: `ampairs-mp-app/shared/src/commonMain/kotlin/com/ampairs/api/PaymentApi.kt`

```kotlin
package com.ampairs.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class PaymentApi(private val httpClient: HttpClient) {

    suspend fun verifyGooglePlayPurchase(
        purchaseToken: String,
        productId: String
    ): Result<VerificationResponse> {
        return try {
            val response: HttpResponse = httpClient.post("https://api.ampairs.com/api/v1/payment/google-play/verify") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "purchase_token" to purchaseToken,
                    "product_id" to productId
                ))
            }

            if (response.status.isSuccess()) {
                // Parse response
                Result.success(VerificationResponse(true, "PROFESSIONAL", "MONTHLY"))
            } else {
                Result.failure(Exception("Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class VerificationResponse(
    val success: Boolean,
    val planCode: String,
    val billingCycle: String
)
```

---

## Part 2: Apple App Store Integration (iOS)

### Prerequisites

1. **App Store Connect Setup**:
   - Create in-app purchase subscriptions
   - Product IDs must match backend configuration
   - Submit for review

2. **Xcode Configuration**:
   - Enable "In-App Purchase" capability
   - Configure StoreKit Configuration file for testing

### Step 1: iOS Implementation

**File**: `ampairs-mp-app/shared/src/iosMain/kotlin/com/ampairs/payment/AppStorePaymentService.kt`

```kotlin
package com.ampairs.payment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.StoreKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

class AppStorePaymentService : PaymentService, NSObjectProtocol {

    private val purchaseUpdates = MutableStateFlow<PurchaseUpdate?>(null)
    private lateinit var transactionObserver: SKPaymentTransactionObserver

    override suspend fun initialize() {
        // Add transaction observer
        transactionObserver = createTransactionObserver()
        SKPaymentQueue.defaultQueue().addTransactionObserver(transactionObserver)
    }

    override suspend fun getAvailablePlans(): Result<List<SubscriptionPlan>> {
        return try {
            val productIdentifiers = setOf(
                "com.ampairs.professional.monthly",
                "com.ampairs.professional.annual"
            )

            // Request products from App Store
            val request = SKProductsRequest(productIdentifiers)
            // ... Set delegate and handle response

            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchaseSubscription(planId: String): Result<PurchaseResult> {
        return try {
            // Create payment for product
            // val payment = SKPayment.paymentWithProduct(product)
            // SKPaymentQueue.defaultQueue().addPayment(payment)

            Result.success(PurchaseResult("", planId, "", 0, false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<List<PurchaseResult>> {
        return try {
            SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        // Call backend API for server-side validation
        return Result.success(SubscriptionStatus(false, null, null, false))
    }

    override fun observePurchaseUpdates(): Flow<PurchaseUpdate> {
        return purchaseUpdates as Flow<PurchaseUpdate>
    }

    private fun createTransactionObserver(): SKPaymentTransactionObserver {
        // Create observer implementation
        return object : NSObject(), SKPaymentTransactionObserverProtocol {
            override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
                updatedTransactions.forEach { transaction ->
                    val skTransaction = transaction as SKPaymentTransaction
                    when (skTransaction.transactionState) {
                        SKPaymentTransactionStatePurchased -> {
                            // Verify with backend
                            handlePurchasedTransaction(skTransaction)
                        }
                        SKPaymentTransactionStateFailed -> {
                            purchaseUpdates.value = PurchaseUpdate.Failed(
                                skTransaction.error?.localizedDescription ?: "Unknown error"
                            )
                            SKPaymentQueue.defaultQueue().finishTransaction(skTransaction)
                        }
                        SKPaymentTransactionStateRestored -> {
                            handleRestoredTransaction(skTransaction)
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    private fun handlePurchasedTransaction(transaction: SKPaymentTransaction) {
        // Verify receipt with backend
        val receiptData = NSBundle.mainBundle.appStoreReceiptURL?.let {
            NSData.dataWithContentsOfURL(it)
        }

        receiptData?.let {
            val receiptString = it.base64EncodedStringWithOptions(0)
            // Send to backend for verification
        }

        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }

    private fun handleRestoredTransaction(transaction: SKPaymentTransaction) {
        // Handle restored purchase
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }
}
```

---

## Part 3: Razorpay Integration (Desktop & Web)

### Prerequisites

1. **Razorpay Account**:
   - Create account at dashboard.razorpay.com
   - Get API keys (test/live)
   - Create subscription plans

2. **Desktop App Configuration**:
   - Razorpay Checkout SDK (web-based)
   - WebView integration for payment UI

### Step 1: Desktop Implementation

**File**: `ampairs-mp-app/shared/src/desktopMain/kotlin/com/ampairs/payment/RazorpayPaymentService.kt`

```kotlin
package com.ampairs.payment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class RazorpayPaymentService(
    private val httpClient: HttpClient
) : PaymentService {

    private val purchaseUpdates = MutableStateFlow<PurchaseUpdate?>(null)

    override suspend fun initialize() {
        // No initialization needed for Razorpay
    }

    override suspend fun getAvailablePlans(): Result<List<SubscriptionPlan>> {
        return try {
            // Fetch plans from backend
            val response: HttpResponse = httpClient.get("https://api.ampairs.com/api/v1/subscription/plans")

            // Parse and return plans
            Result.success(emptyList()) // Placeholder
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun purchaseSubscription(planId: String): Result<PurchaseResult> {
        return try {
            // Create subscription on backend
            val response: HttpResponse = httpClient.post("https://api.ampairs.com/api/v1/payment/razorpay/initiate") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "plan_code" to "PROFESSIONAL",
                    "billing_cycle" to "MONTHLY"
                ))
            }

            // Response contains subscription_id and short_url
            // Open short_url in WebView for payment
            // After payment, Razorpay will call webhook
            // Backend will verify and update subscription

            Result.success(PurchaseResult("", planId, "", 0, false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<List<PurchaseResult>> {
        // Call backend to get current subscription status
        return Result.success(emptyList())
    }

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatus> {
        return try {
            val response: HttpResponse = httpClient.get("https://api.ampairs.com/api/v1/subscription/status")
            // Parse response
            Result.success(SubscriptionStatus(false, null, null, false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePurchaseUpdates(): Flow<PurchaseUpdate> {
        return purchaseUpdates as Flow<PurchaseUpdate>
    }
}
```

---

## Part 4: Platform-Specific Factory

**File**: `ampairs-mp-app/shared/src/commonMain/kotlin/com/ampairs/payment/PaymentServiceFactory.kt`

```kotlin
package com.ampairs.payment

expect object PaymentServiceFactory {
    fun create(): PaymentService
}
```

**File**: `ampairs-mp-app/shared/src/androidMain/kotlin/com/ampairs/payment/PaymentServiceFactory.kt`

```kotlin
package com.ampairs.payment

import android.app.Activity
import android.content.Context

actual object PaymentServiceFactory {
    lateinit var context: Context
    lateinit var activity: Activity

    actual fun create(): PaymentService {
        return GooglePlayPaymentService(context, activity)
    }
}
```

**File**: `ampairs-mp-app/shared/src/iosMain/kotlin/com/ampairs/payment/PaymentServiceFactory.kt`

```kotlin
package com.ampairs.payment

actual object PaymentServiceFactory {
    actual fun create(): PaymentService {
        return AppStorePaymentService()
    }
}
```

**File**: `ampairs-mp-app/shared/src/desktopMain/kotlin/com/ampairs/payment/PaymentServiceFactory.kt`

```kotlin
package com.ampairs.payment

import io.ktor.client.*

actual object PaymentServiceFactory {
    lateinit var httpClient: HttpClient

    actual fun create(): PaymentService {
        return RazorpayPaymentService(httpClient)
    }
}
```

---

## Part 5: UI Integration (Compose Multiplatform)

**File**: `ampairs-mp-app/shared/src/commonMain/kotlin/com/ampairs/ui/SubscriptionScreen.kt`

```kotlin
package com.ampairs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.payment.*
import kotlinx.coroutines.launch

@Composable
fun SubscriptionScreen(
    paymentService: PaymentService
) {
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<SubscriptionPlan>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        paymentService.initialize()
        paymentService.getAvailablePlans().onSuccess {
            plans = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Choose Your Plan", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        plans.forEach { plan ->
            PlanCard(
                plan = plan,
                onPurchaseClick = {
                    scope.launch {
                        loading = true
                        paymentService.purchaseSubscription(plan.planId)
                            .onSuccess {
                                // Handle success
                            }
                            .onFailure {
                                // Handle error
                            }
                        loading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (loading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    onPurchaseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge)
            Text(plan.price, style = MaterialTheme.typography.bodyLarge)
            Text(plan.billingCycle.name, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onPurchaseClick) {
                Text("Subscribe")
            }
        }
    }
}
```

---

## Testing Checklist

### Android (Google Play)
- [ ] Add test account in Google Play Console
- [ ] Test subscription purchase flow
- [ ] Test subscription restoration
- [ ] Verify backend receives purchase token
- [ ] Test subscription renewal (use test subscriptions with shorter periods)
- [ ] Test subscription cancellation

### iOS (App Store)
- [ ] Configure StoreKit Configuration file for testing
- [ ] Test with sandbox account
- [ ] Test purchase, restore, and renewal
- [ ] Verify receipt validation with backend
- [ ] Test subscription management UI

### Desktop (Razorpay)
- [ ] Test with Razorpay test mode
- [ ] Verify webhook delivery
- [ ] Test payment success/failure scenarios
- [ ] Test subscription renewal webhook

---

## Security Best Practices

1. **Never Store Secrets in App**:
   - API keys should be in backend only
   - Use backend for all payment verification

2. **Always Verify with Backend**:
   - Android: Send purchase token to backend
   - iOS: Send receipt to backend
   - Razorpay: Backend handles all payment operations

3. **Handle Pending Purchases**:
   - Check for pending transactions on app start
   - Resume interrupted purchase flows

4. **Secure Communication**:
   - Use HTTPS for all API calls
   - Validate SSL certificates
   - Don't log sensitive data

---

## Related Documentation

- Backend API: `/api/v1/payment/google-play/verify`
- Backend API: `/api/v1/payment/app-store/verify`
- Backend API: `/api/v1/payment/razorpay/initiate`
- Backend Webhook Handling: `WebhookController.kt`
- Database Schema: `V1.0.35__add_payment_provider_product_ids.sql`

---

## Support

For questions or issues:
1. Check backend logs for verification errors
2. Verify product IDs match between app and backend
3. Ensure webhooks are configured correctly
4. Test with sandbox/test mode first
