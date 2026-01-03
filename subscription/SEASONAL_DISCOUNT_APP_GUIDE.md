# Mobile App Implementation Guide - Seasonal/Festival Discounts

## Overview

This guide explains how to integrate seasonal discount functionality into the mobile app (Kotlin Multiplatform). Seasonal discounts automatically activate during festival periods and stack with multi-workspace and billing cycle discounts.

---

## API Changes

### Updated Plan Response

**Endpoint**: `GET /api/v1/subscription/plans`

**New Field Added**: `seasonalDiscount`

```json
{
  "success": true,
  "data": [
    {
      "uid": "PLAN20251123090255640...",
      "planCode": "PROFESSIONAL",
      "displayName": "Professional",
      "monthlyPriceInr": 500.00,
      "monthlyPriceUsd": 6.00,
      "multiWorkspaceDiscount": {
        "minWorkspaces": 3,
        "discountPercent": 20,
        "isAvailable": true
      },
      "seasonalDiscount": {
        "discountPercent": 15,
        "discountName": "Diwali Dhamaka 2025",
        "startAt": "2025-10-10T00:00:00Z",
        "endAt": "2025-11-05T23:59:59Z",
        "isActive": true
      }
    }
  ]
}
```

---

## Data Models

### Update Your DTOs

```kotlin
// Add to your subscription DTOs file

data class PlanResponse(
    val uid: String,
    val planCode: String,
    val displayName: String,
    val description: String?,
    val monthlyPriceInr: BigDecimal,
    val monthlyPriceUsd: BigDecimal,
    val limits: PlanLimitsResponse,
    val features: PlanFeaturesResponse,
    val trialDays: Int,
    val multiWorkspaceDiscount: MultiWorkspaceDiscountResponse,
    val seasonalDiscount: SeasonalDiscountResponse,  // NEW FIELD
    val googlePlayProductIdMonthly: String?,
    val googlePlayProductIdAnnual: String?,
    val appStoreProductIdMonthly: String?,
    val appStoreProductIdAnnual: String?,
    val displayOrder: Int
)

data class SeasonalDiscountResponse(
    val discountPercent: Int,
    val discountName: String?,      // e.g., "Diwali Dhamaka 2025"
    val startAt: Instant?,           // Discount start date (UTC)
    val endAt: Instant?,             // Discount end date (UTC)
    val isActive: Boolean            // Whether discount is currently active
)
```

---

## UI Implementation

### 1. Plan Card with Seasonal Badge

Show a prominent seasonal discount badge when active:

```kotlin
@Composable
fun PlanCard(
    plan: PlanResponse,
    userWorkspaceCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Plan header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = plan.displayName,
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                        )

                        // Show original price with strikethrough if discounts apply
                        val hasDiscounts = plan.seasonalDiscount.isActive ||
                                         plan.multiWorkspaceDiscount.isAvailable

                        if (hasDiscounts) {
                            Text(
                                text = "₹${plan.monthlyPriceInr}",
                                style = MaterialTheme.typography.body2,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Discounted price
                        val discountedPrice = calculateDiscountedPrice(
                            plan = plan,
                            workspaceCount = userWorkspaceCount
                        )

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${discountedPrice}",
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDiscounts) {
                                    MaterialTheme.colors.secondary
                                } else {
                                    MaterialTheme.colors.onSurface
                                }
                            )
                            Text(
                                text = "/workspace/month",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // Seasonal badge (top-right corner)
                    if (plan.seasonalDiscount.isActive) {
                        SeasonalDiscountBadge(plan.seasonalDiscount)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discount breakdown
                DiscountBreakdown(
                    plan = plan,
                    workspaceCount = userWorkspaceCount
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Features list
                plan.features.forEach { feature ->
                    FeatureItem(feature)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Select button
                Button(
                    onClick = { /* Handle selection */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Plan")
                }
            }
        }
    }
}
```

### 2. Seasonal Discount Badge

Eye-catching badge for active seasonal offers:

```kotlin
@Composable
fun SeasonalDiscountBadge(seasonalDiscount: SeasonalDiscountResponse) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 8.dp,
            bottomStart = 8.dp,
            bottomEnd = 0.dp
        ),
        color = Color(0xFFFF6B35), // Festive orange/red
        modifier = Modifier.padding(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${seasonalDiscount.discountPercent}% OFF",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            seasonalDiscount.discountName?.let { name ->
                Text(
                    text = name.split(" ").take(2).joinToString(" "), // First 2 words
                    style = MaterialTheme.typography.overline,
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}
```

### 3. Discount Breakdown Component

Show all active discounts with savings:

```kotlin
@Composable
fun DiscountBreakdown(
    plan: PlanResponse,
    workspaceCount: Int
) {
    val basePrice = plan.monthlyPriceInr
    var currentPrice = basePrice
    val discounts = mutableListOf<Pair<String, BigDecimal>>()

    // Multi-workspace discount
    if (workspaceCount >= plan.multiWorkspaceDiscount.minWorkspaces &&
        plan.multiWorkspaceDiscount.isAvailable
    ) {
        val discount = currentPrice * plan.multiWorkspaceDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
        discounts.add("Multi-workspace (${plan.multiWorkspaceDiscount.discountPercent}%)" to discount)
        currentPrice -= discount
    }

    // Seasonal discount
    if (plan.seasonalDiscount.isActive) {
        val discount = currentPrice * plan.seasonalDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
        discounts.add(
            "${plan.seasonalDiscount.discountName ?: "Seasonal"} (${plan.seasonalDiscount.discountPercent}%)"
            to discount
        )
        currentPrice -= discount
    }

    // Show breakdown if any discounts apply
    if (discounts.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💰 Your Savings",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.secondary
                )

                val totalSavings = basePrice - currentPrice
                Text(
                    text = "₹${totalSavings} saved!",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            discounts.forEach { (name, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "✓ $name",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "-₹$amount",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
```

### 4. Seasonal Discount Banner

Show a banner on the pricing screen when seasonal discount is active:

```kotlin
@Composable
fun SeasonalDiscountBanner(plans: List<PlanResponse>) {
    // Find if any plan has active seasonal discount
    val activeDiscount = plans.firstOrNull { it.seasonalDiscount.isActive }?.seasonalDiscount

    activeDiscount?.let { discount ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xFFFF6B35),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated festive icon
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = discount.discountName ?: "Limited Time Offer!",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Get ${discount.discountPercent}% OFF on all plans!",
                        style = MaterialTheme.typography.body2,
                        color = Color.White
                    )

                    // Countdown timer
                    discount.endAt?.let { endDate ->
                        val daysRemaining = calculateDaysRemaining(endDate)
                        if (daysRemaining > 0) {
                            Text(
                                text = "⏰ Ends in $daysRemaining days",
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun calculateDaysRemaining(endDate: Instant): Long {
    val now = Clock.System.now()
    val duration = endDate - now
    return duration.inWholeDays
}
```

### 5. Price Calculator with All Discounts

Complete pricing calculator showing all discount types:

```kotlin
@Composable
fun CompletePricingCalculator(
    plan: PlanResponse,
    userWorkspaceCount: Int,
    billingCycle: BillingCycle = BillingCycle.MONTHLY
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💵 Your Price Breakdown",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Base price
            var currentPrice = plan.monthlyPriceInr
            PriceRow(
                label = "Base Price",
                amount = currentPrice,
                isBase = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Multi-workspace discount
            if (userWorkspaceCount >= plan.multiWorkspaceDiscount.minWorkspaces &&
                plan.multiWorkspaceDiscount.isAvailable
            ) {
                val discountAmount = currentPrice *
                    plan.multiWorkspaceDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
                currentPrice -= discountAmount

                PriceRow(
                    label = "Multi-workspace Discount (${plan.multiWorkspaceDiscount.discountPercent}%)",
                    amount = -discountAmount,
                    isDiscount = true
                )
            }

            // Seasonal discount
            if (plan.seasonalDiscount.isActive) {
                val discountAmount = currentPrice *
                    plan.seasonalDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
                currentPrice -= discountAmount

                PriceRow(
                    label = "${plan.seasonalDiscount.discountName} (${plan.seasonalDiscount.discountPercent}%)",
                    amount = -discountAmount,
                    isDiscount = true,
                    highlight = true  // Highlight seasonal discount
                )
            }

            // Billing cycle discount (if annual)
            if (billingCycle == BillingCycle.ANNUAL) {
                val annualDiscountPercent = 20 // Example: 20% for annual
                val discountAmount = currentPrice * annualDiscountPercent.toBigDecimal() / 100.toBigDecimal()
                currentPrice -= discountAmount

                PriceRow(
                    label = "Annual Billing Discount ($annualDiscountPercent%)",
                    amount = -discountAmount,
                    isDiscount = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Final price per workspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Price per workspace",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹$currentPrice",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            // Total for all workspaces
            if (userWorkspaceCount > 1) {
                Spacer(modifier = Modifier.height(8.dp))

                val totalPrice = currentPrice * userWorkspaceCount.toBigDecimal()
                val billingPeriodText = when (billingCycle) {
                    BillingCycle.MONTHLY -> "/month"
                    BillingCycle.ANNUAL -> "/year"
                    else -> ""
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total ($userWorkspaceCount workspaces)",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹$totalPrice$billingPeriodText",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                }

                // Show total savings
                val originalTotal = plan.monthlyPriceInr * userWorkspaceCount.toBigDecimal()
                val totalSavings = originalTotal - totalPrice
                if (totalSavings > BigDecimal.ZERO) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🎉 You save ₹$totalSavings$billingPeriodText!",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriceRow(
    label: String,
    amount: BigDecimal,
    isBase: Boolean = false,
    isDiscount: Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (highlight) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = when {
                    highlight -> Color(0xFFFF6B35)
                    isDiscount -> MaterialTheme.colors.secondary
                    else -> MaterialTheme.colors.onSurface
                },
                fontWeight = if (highlight || isDiscount) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = if (amount >= BigDecimal.ZERO) "₹$amount" else "-₹${amount.abs()}",
            style = MaterialTheme.typography.body1,
            color = when {
                highlight -> Color(0xFFFF6B35)
                isDiscount -> MaterialTheme.colors.secondary
                else -> MaterialTheme.colors.onSurface
            },
            fontWeight = if (isBase || isDiscount) FontWeight.Bold else FontWeight.Normal
        )
    }
}

enum class BillingCycle {
    MONTHLY, ANNUAL
}
```

### 6. Countdown Timer for Seasonal Discount

Add urgency with a countdown:

```kotlin
@Composable
fun SeasonalDiscountCountdown(seasonalDiscount: SeasonalDiscountResponse) {
    if (!seasonalDiscount.isActive || seasonalDiscount.endAt == null) return

    val endDate = seasonalDiscount.endAt
    var timeRemaining by remember { mutableStateOf(calculateTimeRemaining(endDate)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Update every minute
            timeRemaining = calculateTimeRemaining(endDate)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFFFF6B35).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Offer ends in: ${formatTimeRemaining(timeRemaining)}",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35)
            )
        }
    }
}

data class TimeRemaining(
    val days: Long,
    val hours: Long,
    val minutes: Long
)

fun calculateTimeRemaining(endDate: Instant): TimeRemaining {
    val now = Clock.System.now()
    val duration = endDate - now

    return TimeRemaining(
        days = duration.inWholeDays,
        hours = (duration.inWholeHours % 24),
        minutes = (duration.inWholeMinutes % 60)
    )
}

fun formatTimeRemaining(time: TimeRemaining): String {
    return when {
        time.days > 0 -> "${time.days}d ${time.hours}h"
        time.hours > 0 -> "${time.hours}h ${time.minutes}m"
        else -> "${time.minutes}m"
    }
}
```

---

## ViewModel Implementation

```kotlin
class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<PlanResponse>>(emptyList())
    val plans: StateFlow<List<PlanResponse>> = _plans.asStateFlow()

    private val _activeSeasonalDiscount = MutableStateFlow<SeasonalDiscountResponse?>(null)
    val activeSeasonalDiscount: StateFlow<SeasonalDiscountResponse?> = _activeSeasonalDiscount.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            try {
                val fetchedPlans = subscriptionRepository.getAvailablePlans()
                _plans.value = fetchedPlans

                // Check if any plan has active seasonal discount
                val seasonalDiscount = fetchedPlans
                    .firstOrNull { it.seasonalDiscount.isActive }
                    ?.seasonalDiscount
                _activeSeasonalDiscount.value = seasonalDiscount

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Calculate discounted price with all discounts applied
     */
    fun calculateDiscountedPrice(
        plan: PlanResponse,
        workspaceCount: Int,
        billingCycle: BillingCycle = BillingCycle.MONTHLY
    ): BigDecimal {
        var price = plan.monthlyPriceInr

        // Apply multi-workspace discount
        if (workspaceCount >= plan.multiWorkspaceDiscount.minWorkspaces &&
            plan.multiWorkspaceDiscount.isAvailable
        ) {
            val discountMultiplier = (100 - plan.multiWorkspaceDiscount.discountPercent).toBigDecimal() / 100.toBigDecimal()
            price *= discountMultiplier
        }

        // Apply seasonal discount
        if (plan.seasonalDiscount.isActive) {
            val discountMultiplier = (100 - plan.seasonalDiscount.discountPercent).toBigDecimal() / 100.toBigDecimal()
            price *= discountMultiplier
        }

        // Apply billing cycle discount
        when (billingCycle) {
            BillingCycle.MONTHLY -> {
                // No additional discount
            }
            BillingCycle.ANNUAL -> {
                // Example: 20% off for annual
                price *= 0.8.toBigDecimal()
            }
        }

        return price.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculate total savings compared to base price
     */
    fun calculateTotalSavings(
        plan: PlanResponse,
        workspaceCount: Int,
        billingCycle: BillingCycle = BillingCycle.MONTHLY
    ): BigDecimal {
        val basePrice = plan.monthlyPriceInr * workspaceCount.toBigDecimal()
        val discountedPrice = calculateDiscountedPrice(plan, workspaceCount, billingCycle) * workspaceCount.toBigDecimal()

        return basePrice - discountedPrice
    }

    /**
     * Check if should show seasonal discount banner
     */
    fun shouldShowSeasonalBanner(): Boolean {
        return _activeSeasonalDiscount.value != null
    }
}
```

---

## Analytics Events

Track seasonal discount engagement:

```kotlin
// When seasonal discount is viewed
analyticsTracker.trackEvent(
    event = "seasonal_discount_viewed",
    properties = mapOf(
        "discount_name" to seasonalDiscount.discountName,
        "discount_percent" to seasonalDiscount.discountPercent,
        "days_remaining" to calculateDaysRemaining(seasonalDiscount.endAt!!),
        "plan_code" to planCode
    )
)

// When user purchases during seasonal discount
analyticsTracker.trackEvent(
    event = "seasonal_discount_purchase",
    properties = mapOf(
        "discount_name" to seasonalDiscount.discountName,
        "discount_percent" to seasonalDiscount.discountPercent,
        "plan_code" to planCode,
        "workspace_count" to workspaceCount,
        "savings_amount" to savingsAmount,
        "multi_workspace_also_applied" to hasMultiWorkspaceDiscount
    )
)

// When seasonal discount expires while user is viewing
analyticsTracker.trackEvent(
    event = "seasonal_discount_expired_in_session",
    properties = mapOf(
        "discount_name" to seasonalDiscount.discountName,
        "time_on_pricing_page" to timeOnPage
    )
)
```

---

## Push Notifications

### Notify Users About Seasonal Discounts

```kotlin
// Send push notification when seasonal discount starts
fun sendSeasonalDiscountNotification(discount: SeasonalDiscountResponse) {
    val notification = PushNotification(
        title = "${discount.discountName} 🎉",
        body = "Get ${discount.discountPercent}% OFF on all subscription plans!",
        data = mapOf(
            "type" to "seasonal_discount",
            "discount_percent" to discount.discountPercent.toString(),
            "deep_link" to "ampairs://subscription/plans"
        )
    )

    pushNotificationService.sendToAllUsers(notification)
}

// Reminder before seasonal discount ends
fun sendSeasonalDiscountReminderNotification(discount: SeasonalDiscountResponse) {
    val notification = PushNotification(
        title = "Last Chance! ⏰",
        body = "${discount.discountName} ends in 24 hours. Save ${discount.discountPercent}% now!",
        data = mapOf(
            "type" to "seasonal_discount_ending",
            "deep_link" to "ampairs://subscription/plans"
        )
    )

    pushNotificationService.sendToAllUsers(notification)
}
```

---

## Testing Checklist

### UI Testing

- [ ] Seasonal badge displays correctly on plan cards
- [ ] Discount breakdown shows all applicable discounts
- [ ] Countdown timer updates correctly
- [ ] Banner appears when seasonal discount is active
- [ ] Banner disappears when discount expires
- [ ] Price calculator shows correct final price
- [ ] Strikethrough on original price when discounts apply

### Integration Testing

- [ ] API returns `seasonalDiscount` field correctly
- [ ] `isActive` flag is accurate based on current time
- [ ] Discounts stack correctly (multi-workspace + seasonal + billing)
- [ ] Price calculation matches backend calculation
- [ ] Seasonal discount auto-activates at start time
- [ ] Seasonal discount auto-deactivates at end time

### Edge Cases

- [ ] No seasonal discount configured (discountPercent = 0)
- [ ] Seasonal discount configured but not yet started
- [ ] Seasonal discount expired
- [ ] Seasonal discount active but no name provided
- [ ] Multiple seasonal discounts on different plans
- [ ] User on seasonal discount upgrades/downgrades plan
- [ ] Seasonal discount expires during checkout flow

### Time Zone Testing

- [ ] Seasonal discount times handled correctly in UTC
- [ ] Countdown timer shows correct time in user's timezone
- [ ] Discount activates at correct local time

---

## Best Practices

### 1. Cache Seasonal Discount Info

```kotlin
// Cache the seasonal discount check to avoid excessive recalculation
class SeasonalDiscountCache {
    private var cachedDiscount: SeasonalDiscountResponse? = null
    private var lastCheckTime: Instant? = null
    private val cacheDuration = 5.minutes

    fun getActiveDiscount(plans: List<PlanResponse>): SeasonalDiscountResponse? {
        val now = Clock.System.now()

        // Use cache if valid
        if (lastCheckTime != null &&
            (now - lastCheckTime!!) < cacheDuration &&
            cachedDiscount != null
        ) {
            return cachedDiscount
        }

        // Refresh cache
        cachedDiscount = plans.firstOrNull { it.seasonalDiscount.isActive }?.seasonalDiscount
        lastCheckTime = now

        return cachedDiscount
    }

    fun invalidate() {
        cachedDiscount = null
        lastCheckTime = null
    }
}
```

### 2. Show Discount Expiry Urgency

```kotlin
fun getDiscountUrgencyLevel(endDate: Instant): DiscountUrgency {
    val hoursRemaining = (endDate - Clock.System.now()).inWholeHours

    return when {
        hoursRemaining < 24 -> DiscountUrgency.CRITICAL  // Red, animated
        hoursRemaining < 72 -> DiscountUrgency.HIGH      // Orange
        hoursRemaining < 168 -> DiscountUrgency.MEDIUM   // Yellow
        else -> DiscountUrgency.LOW                      // Normal
    }
}

enum class DiscountUrgency {
    LOW, MEDIUM, HIGH, CRITICAL
}
```

### 3. Localization Support

```kotlin
// strings.xml
<string name="seasonal_discount_banner_title">%1$s is live!</string>
<string name="seasonal_discount_banner_subtitle">Get %1$d%% OFF on all plans</string>
<string name="seasonal_discount_countdown">Offer ends in: %1$s</string>
<string name="seasonal_discount_savings">You save ₹%1$s!</string>

// Usage
Text(
    text = stringResource(
        R.string.seasonal_discount_banner_title,
        seasonalDiscount.discountName ?: ""
    )
)
```

---

## FAQs

**Q: How often should we refresh plan data to check for seasonal discounts?**
A: Refresh when app opens, when user navigates to pricing screen, and every 5 minutes while on pricing screen.

**Q: What if seasonal discount starts/ends while user is on the pricing screen?**
A: Implement a real-time listener or periodic refresh (every 5 mins) to update UI automatically.

**Q: Should we show expired seasonal discounts?**
A: No, only show active discounts. Backend `isActive` flag handles this.

**Q: Can we show upcoming seasonal discounts?**
A: Yes! Check if `startAt` is in the future and show "Coming Soon" badge.

**Q: How to handle seasonal discount during checkout?**
A: Backend calculates final price. Client shows estimate. Backend price is authoritative.

**Q: Should FREE plan show seasonal discounts?**
A: Typically no, but backend supports it if needed for promotions.

---

## Example: Diwali Theme

Add special theming during festivals:

```kotlin
@Composable
fun DiwaliThemedPricingScreen() {
    // Diwali colors
    val diwaliOrange = Color(0xFFFF6B35)
    val diwaliGold = Color(0xFFFFD700)

    // Add diya (lamp) animations
    // Add fireworks particle effects
    // Use festive fonts

    Column {
        // Animated diya header
        DiyaAnimation()

        // Seasonal banner with festive colors
        SeasonalDiscountBanner(
            plans = plans,
            backgroundColor = diwaliOrange,
            accentColor = diwaliGold
        )

        // Plan cards with festive styling
        plans.forEach { plan ->
            DiwaliThemedPlanCard(plan)
        }
    }
}
```

---

## Summary

**Key Implementation Points:**

✅ **Display Active Badge**: Show prominent seasonal discount indicator
✅ **Stacked Discounts**: Show breakdown of all 3 discount types
✅ **Countdown Timer**: Add urgency with time remaining
✅ **Savings Calculator**: Show total savings clearly
✅ **Festive Theming**: Match app theme to festival season
✅ **Push Notifications**: Notify users when discounts start/end
✅ **Analytics Tracking**: Track engagement and conversions
✅ **Edge Case Handling**: Handle expired, upcoming, and missing discounts

The implementation is straightforward - backend handles all logic, frontend just displays the data attractively! 🎉
