package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import com.ampairs.product.domain.enums.TaxSpec
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table


@Entity(name = "tax_info")
@Table(
    indexes = arrayOf(
        Index(
            name = "tax_info_name_idx",
            columnList = "name"
        )
    )
)
class TaxInfo : OwnableBaseDomain() {
    @Column(name = "name", nullable = false, length = 30)
    var name: String = ""

    @Column(name = "percentage", nullable = false)
    var percentage: Double = 0.0

    @Column(name = "formatted_name", nullable = false, length = 30)
    var formattedName: String = ""

    @Column(name = "tax_spec", nullable = false, length = 10)
    var taxSpec: TaxSpec = TaxSpec.INTER
    override fun obtainIdPrefix(): String {
        return Constants.TAX_INFO_PREFIX
    }
}