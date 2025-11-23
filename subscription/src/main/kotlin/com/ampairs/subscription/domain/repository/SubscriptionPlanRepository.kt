package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SubscriptionPlanRepository : JpaRepository<SubscriptionPlanDefinition, Long> {

    fun findByUid(uid: String): SubscriptionPlanDefinition?

    fun findByPlanCode(planCode: String): SubscriptionPlanDefinition?

    fun findByActiveTrue(): List<SubscriptionPlanDefinition>

    @Query("SELECT p FROM SubscriptionPlanDefinition p WHERE p.active = true ORDER BY p.displayOrder ASC")
    fun findAllActivePlansOrdered(): List<SubscriptionPlanDefinition>

    fun existsByPlanCode(planCode: String): Boolean

    @Query("SELECT p FROM SubscriptionPlanDefinition p WHERE p.googlePlayProductIdMonthly = :productId OR p.googlePlayProductIdAnnual = :productId")
    fun findByGooglePlayProductId(productId: String): SubscriptionPlanDefinition?

    @Query("SELECT p FROM SubscriptionPlanDefinition p WHERE p.appStoreProductIdMonthly = :productId OR p.appStoreProductIdAnnual = :productId")
    fun findByAppStoreProductId(productId: String): SubscriptionPlanDefinition?

    @Query("SELECT p FROM SubscriptionPlanDefinition p WHERE p.razorpayPlanIdMonthly = :planId OR p.razorpayPlanIdAnnual = :planId")
    fun findByRazorpayPlanId(planId: String): SubscriptionPlanDefinition?

    @Query("SELECT p FROM SubscriptionPlanDefinition p WHERE p.stripePriceIdMonthly = :priceId OR p.stripePriceIdAnnual = :priceId")
    fun findByStripePriceId(priceId: String): SubscriptionPlanDefinition?
}
