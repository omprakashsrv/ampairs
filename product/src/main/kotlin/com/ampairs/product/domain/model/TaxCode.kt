package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
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
class TaxCode : OwnableBaseDomain() {

    @Column(name = "code", length = 20)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TaxCode

        return code == other.code
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + code.hashCode()
        return result
    }


}