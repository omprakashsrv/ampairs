package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.ecom.domain.enums.StorefrontAccessIdentifierType
import jakarta.persistence.*

@Entity
@Table(
    name = "ecom_storefront_access_entry",
    indexes = [
        Index(name = "idx_ecom_storefront_access_entry_storefront", columnList = "storefront_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_ecom_storefront_access_entry",
            columnNames = ["storefront_id", "identifier_type", "identifier_value"]
        )
    ]
)
class StorefrontAccessEntry : OwnableBaseDomain() {

    @Column(name = "storefront_id", nullable = false, length = 200)
    var storefrontId: String = ""

    @Column(name = "identifier_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var identifierType: StorefrontAccessIdentifierType = StorefrontAccessIdentifierType.USER_ID

    @Column(name = "identifier_value", nullable = false, length = 320)
    var identifierValue: String = ""

    override fun obtainSeqIdPrefix(): String = "SAE"
}
