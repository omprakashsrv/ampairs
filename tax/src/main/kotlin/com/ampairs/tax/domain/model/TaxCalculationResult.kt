package com.ampairs.tax.domain.model

import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class TaxCalculationResult(
    val baseAmount: BigDecimal,
    val totalTaxAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val hsnCode: String,
    val transactionType: TransactionType,
    val taxComponents: List<TaxComponent>,
    val calculationDate: LocalDateTime = LocalDateTime.now(),
    val isReverseChargeApplicable: Boolean = false,
    val exemptionApplied: String? = null,
    val calculationNotes: List<String> = emptyList()
) {
    fun getTaxComponentAmount(componentType: TaxComponentType): BigDecimal {
        return taxComponents.find { it.componentType == componentType }?.amount ?: BigDecimal.ZERO
    }

    fun getCgstAmount(): BigDecimal = getTaxComponentAmount(TaxComponentType.CGST)
    fun getSgstAmount(): BigDecimal = getTaxComponentAmount(TaxComponentType.SGST)
    fun getIgstAmount(): BigDecimal = getTaxComponentAmount(TaxComponentType.IGST)
    fun getUtgstAmount(): BigDecimal = getTaxComponentAmount(TaxComponentType.UTGST)
    fun getCessAmount(): BigDecimal = getTaxComponentAmount(TaxComponentType.CESS)

    fun getGstComponents(): List<TaxComponent> {
        return taxComponents.filter { it.componentType.isGstComponent }
    }

    fun getNonGstComponents(): List<TaxComponent> {
        return taxComponents.filter { !it.componentType.isGstComponent }
    }

    fun getTotalGstAmount(): BigDecimal {
        return getGstComponents().sumOf { it.amount }
    }

    fun getTotalCessAmount(): BigDecimal {
        return getNonGstComponents().filter { it.componentType.name.contains("CESS") }.sumOf { it.amount }
    }

    fun getEffectiveRate(): BigDecimal {
        return if (baseAmount > BigDecimal.ZERO) {
            totalTaxAmount.divide(baseAmount, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
    }
}

data class TaxComponent(
    val componentType: TaxComponentType,
    val name: String,
    val rate: BigDecimal,
    val amount: BigDecimal,
    val baseAmount: BigDecimal,
    val isFixed: Boolean = false,
    val description: String? = null
) {
    fun getEffectiveRate(): BigDecimal {
        return if (baseAmount > BigDecimal.ZERO && !isFixed) {
            amount.divide(baseAmount, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal(100))
        } else {
            rate
        }
    }
}