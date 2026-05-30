package com.ampairs.ecom.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.ecom.domain.enums.StorefrontStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "ecom_storefront",
    indexes = [
        Index(name = "idx_ecom_storefront_owner", columnList = "owner_id"),
        Index(name = "idx_ecom_storefront_status", columnList = "status"),
    ]
)
class Storefront : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    @Column(name = "slug", nullable = false, length = 50, unique = true)
    var slug: String = ""

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null

    @Column(name = "banner_url", length = 500)
    var bannerUrl: String? = null

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: StorefrontStatus = StorefrontStatus.DRAFT

    @Column(name = "published_at")
    var publishedAt: Instant? = null

    @Column(name = "unpublished_at")
    var unpublishedAt: Instant? = null

    override fun obtainSeqIdPrefix(): String = "SFR"
}
