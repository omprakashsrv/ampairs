# Mobile App Implementation Guide - Pre-Launch Discount

## Overview

This guide explains how to integrate the **50% Pre-Launch Discount** into the mobile app (Kotlin Multiplatform). The pre-launch discount is a special early-bird offer for users who join before the official launch date. It stacks with multi-workspace, seasonal, and billing cycle discounts.

### Key Features:
- **50% discount** for early adopters
- Automatically expires on official launch date
- Can be restricted to new users only
- Stacks with all other discounts
- Creates urgency and FOMO (Fear of Missing Out)

---

## API Changes

### Updated Plan Response

**Endpoint**: `GET /api/v1/subscription/plans`

**New Field Added**: `preLaunchDiscount`

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
      },
      "preLaunchDiscount": {
        "discountPercent": 50,
        "endAt": "2026-03-01T00:00:00Z",
        "newUsersOnly": true,
        "isActive": true
      }
    }
  ]
}
```

### Price Calculation Example

**Original Price**: ₹500/month

**With Pre-Launch (50%) + Multi-Workspace (20%) + Seasonal (15%) + Annual (16.67%)**:
1. Pre-launch: ₹500 × 50% = ₹250
2. Multi-workspace: ₹250 × 80% = ₹200
3. Seasonal: ₹200 × 85% = ₹170
4. Annual: ₹170 × 83.33% = ₹141.67/month

**Total Savings**: 71.67% off!

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
    val seasonalDiscount: SeasonalDiscountResponse,
    val preLaunchDiscount: PreLaunchDiscountResponse,  // NEW FIELD
    val googlePlayProductIdMonthly: String?,
    val googlePlayProductIdAnnual: String?,
    val appStoreProductIdMonthly: String?,
    val appStoreProductIdAnnual: String?,
    val displayOrder: Int
)

data class PreLaunchDiscountResponse(
    val discountPercent: Int,         // 50 = 50% off
    val endAt: Instant?,              // When the pre-launch period ends
    val newUsersOnly: Boolean,        // true = only new users eligible
    val isActive: Boolean             // Whether discount is currently active
)
```

---

## UI Implementation

### 1. Pre-Launch Hero Banner

Display a prominent banner at the top of the subscription screen:

```kotlin
@Composable
fun PreLaunchHeroBanner(
    discount: PreLaunchDiscountResponse
) {
    if (!discount.isActive) return

    val timeRemaining = remember(discount.endAt) {
        calculateTimeRemaining(discount.endAt)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),  // Vibrant orange
                        Color(0xFFF7931E)   // Golden orange
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "PRE-LAUNCH" label
            Text(
                text = "🚀 PRE-LAUNCH OFFER",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main discount message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${discount.discountPercent}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OFF",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Early Bird Special - Limited Time!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Countdown timer
            CountdownTimer(
                endAt = discount.endAt,
                textColor = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // User eligibility notice
            if (discount.newUsersOnly) {
                Text(
                    text = "✨ Available for new subscribers",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
```

### 2. Pre-Launch Badge (Compact)

For use on plan cards:

```kotlin
@Composable
fun PreLaunchBadge(
    discount: PreLaunchDiscountResponse,
    modifier: Modifier = Modifier
) {
    if (!discount.isActive) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFF6B35),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚀",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${discount.discountPercent}% OFF",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PRE-LAUNCH",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

### 3. Complete Discount Breakdown Card

Shows all active discounts including pre-launch:

```kotlin
@Composable
fun CompleteDiscountBreakdown(
    plan: PlanResponse,
    currency: String,
    billingCycle: BillingCycle,
    workspaceCount: Int,
    isNewUser: Boolean
) {
    val basePrice = when (currency) {
        "INR" -> plan.monthlyPriceInr
        "USD" -> plan.monthlyPriceUsd
        else -> plan.monthlyPriceUsd
    }

    // Calculate stacked discounts
    var currentPrice = basePrice
    val discountsApplied = mutableListOf<DiscountInfo>()

    // 1. Pre-launch discount (if active and user eligible)
    if (plan.preLaunchDiscount.isActive &&
        (!plan.preLaunchDiscount.newUsersOnly || isNewUser)) {
        val discountAmount = currentPrice * plan.preLaunchDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
        currentPrice -= discountAmount
        discountsApplied.add(
            DiscountInfo(
                name = "🚀 Pre-Launch Special",
                percent = plan.preLaunchDiscount.discountPercent,
                amount = discountAmount,
                badge = "EARLY BIRD"
            )
        )
    }

    // 2. Multi-workspace discount
    if (plan.multiWorkspaceDiscount.isAvailable &&
        workspaceCount >= plan.multiWorkspaceDiscount.minWorkspaces) {
        val discountAmount = currentPrice * plan.multiWorkspaceDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
        currentPrice -= discountAmount
        discountsApplied.add(
            DiscountInfo(
                name = "Multi-Workspace Bundle",
                percent = plan.multiWorkspaceDiscount.discountPercent,
                amount = discountAmount,
                badge = "${workspaceCount}x"
            )
        )
    }

    // 3. Seasonal discount
    if (plan.seasonalDiscount.isActive) {
        val discountAmount = currentPrice * plan.seasonalDiscount.discountPercent.toBigDecimal() / 100.toBigDecimal()
        currentPrice -= discountAmount
        discountsApplied.add(
            DiscountInfo(
                name = plan.seasonalDiscount.discountName ?: "Seasonal Offer",
                percent = plan.seasonalDiscount.discountPercent,
                amount = discountAmount,
                badge = "FESTIVAL"
            )
        )
    }

    // 4. Billing cycle discount
    val cycleDiscount = when (billingCycle) {
        BillingCycle.ANNUAL -> 16.67
        BillingCycle.QUARTERLY -> 8.33
        else -> 0.0
    }
    if (cycleDiscount > 0) {
        val discountAmount = currentPrice * cycleDiscount.toBigDecimal() / 100.toBigDecimal()
        currentPrice -= discountAmount
        discountsApplied.add(
            DiscountInfo(
                name = "${billingCycle.name.lowercase().capitalize()} Billing",
                percent = cycleDiscount.toInt(),
                amount = discountAmount,
                badge = billingCycle.name
            )
        )
    }

    val totalSavings = basePrice - currentPrice
    val totalSavingsPercent = ((totalSavings / basePrice) * 100.toBigDecimal()).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Discounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Save $totalSavingsPercent%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Base price
            PriceRow(
                label = "Base Price",
                amount = basePrice,
                currency = currency,
                isStrikethrough = discountsApplied.isNotEmpty()
            )

            // Applied discounts
            discountsApplied.forEach { discount ->
                Spacer(modifier = Modifier.height(8.dp))
                DiscountRow(discount, currency)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Final price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Final Price",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPrice(currentPrice, currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Savings highlight
            if (totalSavings > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💰",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You save ${formatPrice(totalSavings, currency)} every month!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

data class DiscountInfo(
    val name: String,
    val percent: Int,
    val amount: BigDecimal,
    val badge: String
)

@Composable
private fun DiscountRow(
    discount: DiscountInfo,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = discount.badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${discount.name} (-${discount.percent}%)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "-${formatPrice(discount.amount, currency)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4CAF50)
        )
    }
}
```

### 4. Countdown Timer Component

Creates urgency by showing time remaining:

```kotlin
@Composable
fun CountdownTimer(
    endAt: Instant?,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (endAt == null) return

    var timeRemaining by remember { mutableStateOf(calculateTimeRemaining(endAt)) }

    LaunchedEffect(endAt) {
        while (true) {
            delay(1000)
            timeRemaining = calculateTimeRemaining(endAt)
            if (timeRemaining.isExpired) break
        }
    }

    if (timeRemaining.isExpired) {
        Text(
            text = "Offer Ended",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.7f)
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⏰ Ends in: ",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Days
        if (timeRemaining.days > 0) {
            TimeUnit(timeRemaining.days, "d", textColor)
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Hours
        TimeUnit(timeRemaining.hours, "h", textColor)
        Spacer(modifier = Modifier.width(4.dp))

        // Minutes
        TimeUnit(timeRemaining.minutes, "m", textColor)
        Spacer(modifier = Modifier.width(4.dp))

        // Seconds
        TimeUnit(timeRemaining.seconds, "s", textColor)
    }
}

@Composable
private fun TimeUnit(
    value: Int,
    unit: String,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

data class TimeRemaining(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val isExpired: Boolean
)

fun calculateTimeRemaining(endAt: Instant?): TimeRemaining {
    if (endAt == null) return TimeRemaining(0, 0, 0, 0, true)

    val now = Clock.System.now()
    val duration = endAt - now

    if (duration.isNegative()) {
        return TimeRemaining(0, 0, 0, 0, true)
    }

    val totalSeconds = duration.inWholeSeconds
    val days = (totalSeconds / 86400).toInt()
    val hours = ((totalSeconds % 86400) / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()

    return TimeRemaining(days, hours, minutes, seconds, false)
}
```

### 5. Pre-Launch Announcement Modal

Show on first app open during pre-launch period:

```kotlin
@Composable
fun PreLaunchAnnouncementModal(
    discount: PreLaunchDiscountResponse,
    onDismiss: () -> Unit,
    onViewPlans: () -> Unit
) {
    if (!discount.isActive) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rocket animation or icon
                Text(
                    text = "🚀",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Welcome, Early Adopter!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Discount announcement
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF6B35)
                ) {
                    Text(
                        text = "${discount.discountPercent}% OFF",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Join us during our pre-launch phase and get exclusive early-bird pricing on all premium plans!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Countdown
                CountdownTimer(
                    endAt = discount.endAt,
                    textColor = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Benefits list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BenefitRow("✅ 50% discount on all plans")
                    BenefitRow("✅ Stacks with other offers")
                    BenefitRow("✅ Lock in early-bird pricing")
                    BenefitRow("✅ Priority support access")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA Button
                Button(
                    onClick = onViewPlans,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text(
                        text = "View Plans & Save ${discount.discountPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss button
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later")
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

---

## ViewModel Implementation

```kotlin
class SubscriptionViewModel : ViewModel() {

    private val _plans = MutableStateFlow<List<PlanResponse>>(emptyList())
    val plans: StateFlow<List<PlanResponse>> = _plans.asStateFlow()

    private val _hasSeenPreLaunchAnnouncement = MutableStateFlow(false)
    val hasSeenPreLaunchAnnouncement: StateFlow<Boolean> = _hasSeenPreLaunchAnnouncement.asStateFlow()

    fun loadPlans() {
        viewModelScope.launch {
            try {
                val response = subscriptionApi.getPlans()
                _plans.value = response.data ?: emptyList()

                // Check if we should show pre-launch announcement
                val hasActivePreLaunch = response.data?.any {
                    it.preLaunchDiscount.isActive
                } == true

                if (hasActivePreLaunch && !prefs.hasSeenPreLaunchAnnouncement()) {
                    _hasSeenPreLaunchAnnouncement.value = true
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun markPreLaunchAnnouncementSeen() {
        prefs.setPreLaunchAnnouncementSeen(true)
        _hasSeenPreLaunchAnnouncement.value = false
    }

    fun calculateFinalPrice(
        plan: PlanResponse,
        currency: String,
        billingCycle: BillingCycle,
        workspaceCount: Int,
        isNewUser: Boolean
    ): BigDecimal {
        var price = when (currency) {
            "INR" -> plan.monthlyPriceInr
            "USD" -> plan.monthlyPriceUsd
            else -> plan.monthlyPriceUsd
        }

        // Apply pre-launch discount (if eligible)
        if (plan.preLaunchDiscount.isActive &&
            (!plan.preLaunchDiscount.newUsersOnly || isNewUser)) {
            price *= (100 - plan.preLaunchDiscount.discountPercent).toBigDecimal() / 100.toBigDecimal()
        }

        // Apply multi-workspace discount
        if (plan.multiWorkspaceDiscount.isAvailable &&
            workspaceCount >= plan.multiWorkspaceDiscount.minWorkspaces) {
            price *= (100 - plan.multiWorkspaceDiscount.discountPercent).toBigDecimal() / 100.toBigDecimal()
        }

        // Apply seasonal discount
        if (plan.seasonalDiscount.isActive) {
            price *= (100 - plan.seasonalDiscount.discountPercent).toBigDecimal() / 100.toBigDecimal()
        }

        // Apply billing cycle discount
        val cycleMultiplier = when (billingCycle) {
            BillingCycle.ANNUAL -> 0.8333.toBigDecimal()  // 16.67% off
            BillingCycle.QUARTERLY -> 0.9167.toBigDecimal()  // 8.33% off
            else -> BigDecimal.ONE
        }
        price *= cycleMultiplier

        return price.setScale(2, RoundingMode.HALF_UP)
    }
}
```

---

## Analytics Events

Track pre-launch discount engagement:

```kotlin
object AnalyticsEvents {

    fun logPreLaunchDiscountViewed(
        planCode: String,
        discountPercent: Int,
        daysRemaining: Int
    ) {
        analytics.logEvent("pre_launch_discount_viewed") {
            param("plan_code", planCode)
            param("discount_percent", discountPercent.toLong())
            param("days_remaining", daysRemaining.toLong())
        }
    }

    fun logPreLaunchAnnouncementShown() {
        analytics.logEvent("pre_launch_announcement_shown")
    }

    fun logPreLaunchAnnouncementDismissed(
        viewedForSeconds: Long
    ) {
        analytics.logEvent("pre_launch_announcement_dismissed") {
            param("viewed_for_seconds", viewedForSeconds)
        }
    }

    fun logPreLaunchSubscriptionStarted(
        planCode: String,
        originalPrice: Double,
        finalPrice: Double,
        totalSavings: Double
    ) {
        analytics.logEvent("pre_launch_subscription_started") {
            param("plan_code", planCode)
            param("original_price", originalPrice)
            param("final_price", finalPrice)
            param("total_savings", totalSavings)
            param("savings_percent", ((totalSavings / originalPrice) * 100).toLong())
        }
    }
}
```

---

## Testing Checklist

### UI Tests
- [ ] Pre-launch badge displays correctly on plan cards
- [ ] Hero banner shows correct discount percentage (50%)
- [ ] Countdown timer updates every second
- [ ] Discount breakdown shows all stacked discounts
- [ ] Announcement modal appears only once per user
- [ ] "New users only" restriction displays when applicable

### Integration Tests
- [ ] API returns `preLaunchDiscount` field
- [ ] Discount becomes inactive after `endAt` date
- [ ] Price calculation includes pre-launch discount first
- [ ] All discounts stack correctly (pre-launch + multi + seasonal + cycle)
- [ ] New user vs existing user eligibility works

### Edge Cases
- [ ] Pre-launch discount at 0% doesn't show UI
- [ ] `endAt` is null - discount doesn't activate
- [ ] Expired pre-launch discount hides all UI
- [ ] Multiple plans with different pre-launch settings
- [ ] User timezone doesn't affect UTC countdown

### Business Logic
- [ ] 50% discount applies correctly
- [ ] Stacking order: pre-launch → multi-workspace → seasonal → billing cycle
- [ ] Final price never goes below zero
- [ ] Discount only applies to paid plans (not FREE)

---

## Best Practices

### 1. **Create Urgency Without Being Pushy**
```kotlin
// Good: Informative urgency
"⏰ Pre-launch pricing ends in 5 days"

// Avoid: Aggressive urgency
"LAST CHANCE!!! ACT NOW OR MISS OUT FOREVER!!!"
```

### 2. **Highlight the Value**
```kotlin
// Show what they're getting
"Lock in 50% off forever as an early adopter"

// Not just the discount
"50% off"
```

### 3. **Be Transparent**
```kotlin
// Clear about restrictions
"Available for new subscribers during pre-launch period"

// Not hidden in fine print
```

### 4. **Make It Feel Exclusive**
```kotlin
"🚀 Early Adopter Exclusive"
"Join the founding members"
"VIP Pre-Launch Access"
```

### 5. **Cache Pre-Launch Status**
```kotlin
// Avoid flickering UI during load
val preLaunchStatus = remember {
    prefs.getCachedPreLaunchStatus()
}

// Update cache when plans load
LaunchedEffect(plans) {
    if (plans.isNotEmpty()) {
        prefs.cachePreLaunchStatus(plans.first().preLaunchDiscount)
    }
}
```

---

## Push Notification Examples

### When Pre-Launch is Active
```json
{
  "title": "🚀 Early Bird Special - 50% Off!",
  "body": "Join during our pre-launch and save 50% on all premium plans. Offer ends March 1st!",
  "action": "view_plans",
  "image": "pre_launch_banner.jpg"
}
```

### Countdown Reminders
```json
{
  "title": "⏰ Pre-Launch Ends in 3 Days",
  "body": "Lock in 50% off pricing before we officially launch. Don't miss out!",
  "action": "view_plans"
}
```

### Last Day Reminder
```json
{
  "title": "🔥 FINAL HOURS - Pre-Launch 50% Off",
  "body": "This is your last chance to get early-bird pricing. Offer expires tonight at midnight!",
  "action": "view_plans",
  "priority": "high"
}
```

---

## Configuration Examples

### Extend Pre-Launch Period
```sql
-- Extend to June 1, 2026
UPDATE subscription_plans
SET pre_launch_discount_end_at = '2026-06-01 00:00:00'
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
```

### Make Available to All Users (Not Just New)
```sql
UPDATE subscription_plans
SET pre_launch_new_users_only = 0
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
```

### Change Discount Percentage
```sql
-- Change to 40% off
UPDATE subscription_plans
SET pre_launch_discount_percent = 40
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
```

### Disable Pre-Launch Discount
```sql
-- Set discount to 0% to disable
UPDATE subscription_plans
SET pre_launch_discount_percent = 0
WHERE plan_code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE');
```

---

## Summary

The pre-launch discount feature provides a powerful tool to:
- ✅ Reward early adopters with 50% off
- ✅ Create urgency with countdown timers
- ✅ Stack with other discounts for maximum savings
- ✅ Drive conversions during pre-launch phase
- ✅ Build a loyal founding member base

**Discount Stacking Example**:
- Base: ₹500/month
- Pre-Launch (50%): ₹250
- Multi-Workspace (20%): ₹200
- Seasonal (15%): ₹170
- Annual (16.67%): ₹141.67
- **Total Savings**: 71.67% off!

---

**Questions?** Check the backend implementation in:
- `SubscriptionPlanDefinition.kt` - Entity and discount logic
- `SubscriptionDtos.kt` - API response structure
- `V1.0.33__add_pre_launch_discount.sql` - Database migration
