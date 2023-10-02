package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import com.ampairs.product.domain.enums.TaxType
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
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
    @Enumerated(EnumType.STRING)
    var type: TaxType = TaxType.HSN

    @Column(name = "description", length = 255)
    var description: String = ""

    @Type(JsonType::class)
    @Column(name = "tax_info", length = 255, nullable = false, columnDefinition = "json")
    var taxInfos: List<TaxInfoModel> = listOf()

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