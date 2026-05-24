package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.UpdateBillingPreferencesRequest
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.repository.BillingPreferencesRepository
import com.ampairs.subscription.domain.repository.PaymentMethodRepository
import com.ampairs.subscription.exception.SubscriptionException
import org.springframework.stereotype.Service

@Service
class BillingPreferencesService(
    private val billingPreferencesRepository: BillingPreferencesRepository,
    private val paymentMethodRepository: PaymentMethodRepository
) {

    fun getBillingPreferences(workspaceId: String): BillingPreferences =
        billingPreferencesRepository.findByWorkspaceId(workspaceId)
            ?: throw SubscriptionException.NotFound("Billing preferences not found")

    fun updateBillingPreferences(workspaceId: String, request: UpdateBillingPreferencesRequest): BillingPreferences {
        var preferences = billingPreferencesRepository.findByWorkspaceId(workspaceId)
            ?: BillingPreferences().apply { this.workspaceId = workspaceId }

        request.autoPaymentEnabled?.let { preferences.autoPaymentEnabled = it }
        request.defaultPaymentMethodId?.let { methodId ->
            val paymentMethod = paymentMethodRepository.findById(methodId)
                .orElseThrow { SubscriptionException.PaymentMethodNotFound(methodId.toString()) }
            if (paymentMethod.workspaceId != workspaceId) {
                throw SubscriptionException.PaymentFailed("Payment method does not belong to this workspace")
            }
            preferences.defaultPaymentMethodId = methodId
        }
        request.billingMode?.let { preferences.billingMode = it }
        request.billingEmail?.let { preferences.billingEmail = it }
        request.sendPaymentReminders?.let { preferences.sendPaymentReminders = it }
        request.gracePeriodDays?.let { preferences.gracePeriodDays = it }
        request.taxIdentifier?.let { preferences.taxIdentifier = it }
        request.billingAddress?.let { address ->
            preferences.billingAddressLine1 = address.line1
            preferences.billingAddressLine2 = address.line2
            preferences.billingCity = address.city
            preferences.billingState = address.state
            preferences.billingPostalCode = address.postalCode
            preferences.billingCountry = address.country
        }

        return billingPreferencesRepository.save(preferences)
    }
}
