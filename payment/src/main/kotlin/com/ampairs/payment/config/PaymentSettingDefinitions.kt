package com.ampairs.payment.config

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingDefinitionProvider
import com.ampairs.core.setting.SettingValueType
import org.springframework.stereotype.Component

/** Payment module's workspace settings, aggregated by the `setting` module (R11 / FR-027). */
@Component
class PaymentSettingDefinitions : SettingDefinitionProvider {

    override fun definitions(): List<SettingDefinition> = listOf(
        SettingDefinition(
            module = "payment",
            key = "enabled_payment_modes",
            valueType = SettingValueType.STRING,
            defaultValue = "CASH,CHEQUE,UPI,NEFT,RTGS,IMPS,NET_BANKING,CARD,BANK_TRANSFER",
            label = "Enabled payment modes",
            description = "Comma-separated payment modes a workspace accepts.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = "payment",
            key = "default_payment_mode",
            valueType = SettingValueType.ENUM,
            defaultValue = "CASH",
            allowedValues = listOf(
                "CASH", "CHEQUE", "UPI", "NEFT", "RTGS", "IMPS",
                "NET_BANKING", "CARD", "BANK_TRANSFER", "WALLET", "OTHER",
            ),
            label = "Default payment mode",
            description = "Pre-selected mode on the record-payment screen.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = "payment",
            key = "cheque_requires_clearance",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Cheques require a clearance step",
            description = "When true, cheque receipts start as PENDING and must be cleared/bounced.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = "payment",
            key = "allow_on_account_receipts",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Allow on-account (advance) receipts",
            description = "Whether an unallocated payment remainder may be retained as an advance.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = "payment",
            key = "enforce_credit_limit",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Enforce credit limit",
            description = "Whether to warn/block when a party's receivable exceeds its credit limit.",
            requiresModule = Constants.MODULE_CODE,
        ),
        SettingDefinition(
            module = "payment",
            key = "aging_buckets",
            valueType = SettingValueType.STRING,
            defaultValue = "30,60,90",
            label = "Aging bucket boundaries (days)",
            description = "Comma-separated upper day-bounds; e.g. 30,60,90 → 0-30/31-60/61-90/90+.",
            requiresModule = Constants.MODULE_CODE,
        ),
    )
}
