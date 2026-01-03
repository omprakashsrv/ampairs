package com.ampairs.subscription.exception

import org.springframework.http.HttpStatus

/**
 * Base exception for subscription-related errors
 */
sealed class SubscriptionException(
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val errorCode: String
) : RuntimeException(message) {

    class PlanNotFoundException(planCode: String) : SubscriptionException(
        message = "Subscription plan not found: $planCode",
        status = HttpStatus.NOT_FOUND,
        errorCode = "PLAN_NOT_FOUND"
    )

    class SubscriptionNotFoundException(workspaceId: String) : SubscriptionException(
        message = "No subscription found for workspace: $workspaceId",
        status = HttpStatus.NOT_FOUND,
        errorCode = "SUBSCRIPTION_NOT_FOUND"
    )

    class SubscriptionAlreadyExists(workspaceId: String) : SubscriptionException(
        message = "Subscription already exists for workspace: $workspaceId",
        status = HttpStatus.CONFLICT,
        errorCode = "SUBSCRIPTION_EXISTS"
    )

    class InvalidSubscriptionStatus(current: String, expected: String) : SubscriptionException(
        message = "Invalid subscription status: $current (expected: $expected)",
        status = HttpStatus.BAD_REQUEST,
        errorCode = "INVALID_STATUS"
    )

    class LimitExceeded(resource: String, current: Int, limit: Int) : SubscriptionException(
        message = "Limit exceeded for $resource: $current/$limit",
        status = HttpStatus.PAYMENT_REQUIRED,
        errorCode = "LIMIT_EXCEEDED"
    )

    class FeatureNotAvailable(feature: String, planCode: String) : SubscriptionException(
        message = "Feature '$feature' is not available in plan: $planCode",
        status = HttpStatus.PAYMENT_REQUIRED,
        errorCode = "FEATURE_NOT_AVAILABLE"
    )

    class OperationNotAllowed(reason: String) : SubscriptionException(
        message = "Operation not allowed: $reason",
        status = HttpStatus.FORBIDDEN,
        errorCode = "OPERATION_NOT_ALLOWED"
    )

    class DeviceNotFound(deviceId: String) : SubscriptionException(
        message = "Device not found: $deviceId",
        status = HttpStatus.NOT_FOUND,
        errorCode = "DEVICE_NOT_FOUND"
    )

    class DeviceDeactivated(deviceId: String) : SubscriptionException(
        message = "Device has been deactivated: $deviceId",
        status = HttpStatus.FORBIDDEN,
        errorCode = "DEVICE_DEACTIVATED"
    )

    class DeviceLimitExceeded(current: Int, limit: Int) : SubscriptionException(
        message = "Device limit exceeded: $current/$limit devices registered",
        status = HttpStatus.PAYMENT_REQUIRED,
        errorCode = "DEVICE_LIMIT_EXCEEDED"
    )

    class TransactionNotFound(transactionId: String) : SubscriptionException(
        message = "Payment transaction not found: $transactionId",
        status = HttpStatus.NOT_FOUND,
        errorCode = "TRANSACTION_NOT_FOUND"
    )

    class PaymentMethodNotFound(paymentMethodId: String) : SubscriptionException(
        message = "Payment method not found: $paymentMethodId",
        status = HttpStatus.NOT_FOUND,
        errorCode = "PAYMENT_METHOD_NOT_FOUND"
    )

    class PaymentFailed(reason: String) : SubscriptionException(
        message = "Payment failed: $reason",
        status = HttpStatus.PAYMENT_REQUIRED,
        errorCode = "PAYMENT_FAILED"
    )

    class InvoiceNotFound(invoiceId: String) : SubscriptionException(
        message = "Invoice not found: $invoiceId",
        status = HttpStatus.NOT_FOUND,
        errorCode = "INVOICE_NOT_FOUND"
    )

    class WorkspaceRequired : SubscriptionException(
        message = "Workspace ID is required",
        status = HttpStatus.BAD_REQUEST,
        errorCode = "WORKSPACE_REQUIRED"
    )

    class NotFound(message: String) : SubscriptionException(
        message = message,
        status = HttpStatus.NOT_FOUND,
        errorCode = "NOT_FOUND"
    )

    class InvalidPurchaseToken(provider: String) : SubscriptionException(
        message = "Invalid purchase token from: $provider",
        status = HttpStatus.BAD_REQUEST,
        errorCode = "INVALID_PURCHASE_TOKEN"
    )

    class WebhookVerificationFailed(provider: String) : SubscriptionException(
        message = "Webhook signature verification failed for: $provider",
        status = HttpStatus.UNAUTHORIZED,
        errorCode = "WEBHOOK_VERIFICATION_FAILED"
    )

    class ProviderError(provider: String, error: String) : SubscriptionException(
        message = "Error from $provider: $error",
        status = HttpStatus.BAD_GATEWAY,
        errorCode = "PROVIDER_ERROR"
    )

    class AddonNotFound(addonCode: String) : SubscriptionException(
        message = "Add-on not found: $addonCode",
        status = HttpStatus.NOT_FOUND,
        errorCode = "ADDON_NOT_FOUND"
    )

    class AddonAlreadyActive(addonCode: String) : SubscriptionException(
        message = "Add-on already active: $addonCode",
        status = HttpStatus.CONFLICT,
        errorCode = "ADDON_ALREADY_ACTIVE"
    )

    class TrialNotAvailable(reason: String) : SubscriptionException(
        message = "Trial not available: $reason",
        status = HttpStatus.BAD_REQUEST,
        errorCode = "TRIAL_NOT_AVAILABLE"
    )

    class TrialAlreadyUsed(workspaceId: String) : SubscriptionException(
        message = "Trial has already been used for workspace: $workspaceId",
        status = HttpStatus.CONFLICT,
        errorCode = "TRIAL_ALREADY_USED"
    )

    class AlreadyHasActivePlan(workspaceId: String, planCode: String) : SubscriptionException(
        message = "Workspace $workspaceId already has an active subscription: $planCode",
        status = HttpStatus.CONFLICT,
        errorCode = "ACTIVE_PLAN_EXISTS"
    )

    class SubscriptionExpired(workspaceId: String) : SubscriptionException(
        message = "Subscription has expired for workspace: $workspaceId",
        status = HttpStatus.PAYMENT_REQUIRED,
        errorCode = "SUBSCRIPTION_EXPIRED"
    )
}
