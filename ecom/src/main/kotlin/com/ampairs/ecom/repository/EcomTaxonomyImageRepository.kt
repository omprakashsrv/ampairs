package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.enums.TaxonomyType
import com.ampairs.ecom.domain.model.EcomTaxonomyImage
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EcomTaxonomyImageRepository : CrudRepository<EcomTaxonomyImage, Long> {

    fun findByStorefrontIdOrderBySortOrderAsc(storefrontId: String): List<EcomTaxonomyImage>

    fun findByStorefrontIdAndTaxonomyTypeOrderBySortOrderAsc(
        storefrontId: String,
        taxonomyType: TaxonomyType,
    ): List<EcomTaxonomyImage>

    fun findByStorefrontIdAndTaxonomyTypeAndName(
        storefrontId: String,
        taxonomyType: TaxonomyType,
        name: String,
    ): EcomTaxonomyImage?
}
