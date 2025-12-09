package com.ampairs.tax.service

import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.ComponentReference
import org.springframework.stereotype.Service

/**
 * Service for generating standard Indian GST rule templates based on tax rates.
 *
 * Indian GST Structure:
 * - INTRA_STATE (within same state): Split into CGST + SGST (50/50)
 * - INTER_STATE (between states): Single IGST (full rate)
 */
@Service
class GstRuleTemplateService {

    /**
     * Generates standard GST component composition for a given tax rate.
     *
     * @param taxRate The total GST rate (e.g., 5.0, 12.0, 18.0, 28.0)
     * @return Map of scenario -> ComponentComposition
     */
    fun generateStandardGstComposition(taxRate: Double): Map<String, ComponentComposition> {
        val halfRate = taxRate / 2.0

        return mapOf(
            "INTRA_STATE" to ComponentComposition(
                scenario = "INTRA_STATE",
                components = listOf(
                    ComponentReference(
                        id = "COMP_CGST_${halfRate.toInt()}",
                        name = "CGST",
                        rate = halfRate,
                        order = 1
                    ),
                    ComponentReference(
                        id = "COMP_SGST_${halfRate.toInt()}",
                        name = "SGST",
                        rate = halfRate,
                        order = 2
                    )
                ),
                totalRate = taxRate
            ),
            "INTER_STATE" to ComponentComposition(
                scenario = "INTER_STATE",
                components = listOf(
                    ComponentReference(
                        id = "COMP_IGST_${taxRate.toInt()}",
                        name = "IGST",
                        rate = taxRate,
                        order = 1
                    )
                ),
                totalRate = taxRate
            )
        )
    }

    /**
     * Generates GST composition with additional cess component.
     * Used for luxury/sin goods with additional cess.
     */
    fun generateGstWithCess(
        baseRate: Double,
        cessRate: Double
    ): Map<String, ComponentComposition> {
        val halfBaseRate = baseRate / 2.0
        val totalRate = baseRate + cessRate

        return mapOf(
            "INTRA_STATE" to ComponentComposition(
                scenario = "INTRA_STATE",
                components = listOf(
                    ComponentReference(
                        id = "COMP_CGST_${halfBaseRate.toInt()}",
                        name = "CGST",
                        rate = halfBaseRate,
                        order = 1
                    ),
                    ComponentReference(
                        id = "COMP_SGST_${halfBaseRate.toInt()}",
                        name = "SGST",
                        rate = halfBaseRate,
                        order = 2
                    ),
                    ComponentReference(
                        id = "COMP_CESS_${cessRate.toInt()}",
                        name = "CESS",
                        rate = cessRate,
                        order = 3
                    )
                ),
                totalRate = totalRate
            ),
            "INTER_STATE" to ComponentComposition(
                scenario = "INTER_STATE",
                components = listOf(
                    ComponentReference(
                        id = "COMP_IGST_${baseRate.toInt()}",
                        name = "IGST",
                        rate = baseRate,
                        order = 1
                    ),
                    ComponentReference(
                        id = "COMP_CESS_${cessRate.toInt()}",
                        name = "CESS",
                        rate = cessRate,
                        order = 2
                    )
                ),
                totalRate = totalRate
            )
        )
    }

    /**
     * Check if a given rate is a standard GST rate.
     */
    fun isStandardGstRate(rate: Double): Boolean {
        return rate in listOf(0.0, 0.25, 3.0, 5.0, 12.0, 18.0, 28.0)
    }
}
