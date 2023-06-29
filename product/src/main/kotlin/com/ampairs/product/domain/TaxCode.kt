package com.ampairs.product.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity(name = "tax_code")
class TaxCode : BaseDomain() {

    @Column(name = "hsn_code", length = 10, unique = true)
    var code: String = ""

    @Column(name = "type", length = 10)
    var type: TaxType = TaxType.HSN

    @Column(name = "description", length = 255)
    var description: String = ""

    @Column(name = "cgst")
    var cgst: Double = 0.0

    @Column(name = "sgst")
    var sgst: Double = 0.0

    @Column(name = "igst")
    var igst: Double = 0.0

    @Column(name = "cess")
    var cess: Double = 0.0

    override fun obtainIdPrefix(): String {
        return Constants.HSN_CODE_PREFIX
    }
}