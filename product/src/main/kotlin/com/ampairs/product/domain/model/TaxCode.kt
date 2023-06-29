package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.sql.Timestamp

@Entity(name = "tax_code")
@Table(
    indexes = arrayOf(
        Index(
            name = "tax_code_idx",
            columnList = "code"
        )
    )
)
class TaxCode : BaseDomain() {

    @Column(name = "code", length = 10)
    var code: String = ""

    @Column(name = "effective_from")
    var effectiveFrom: Timestamp? = null

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