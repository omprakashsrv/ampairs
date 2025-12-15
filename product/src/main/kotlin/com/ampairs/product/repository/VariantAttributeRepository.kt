package com.ampairs.product.repository

import com.ampairs.product.domain.model.VariantAttribute
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VariantAttributeRepository : CrudRepository<VariantAttribute, Long> {

    @Query("SELECT DISTINCT va.attributeValue FROM VariantAttribute va WHERE va.productId = :productId AND va.attributeName = :attributeName")
    fun findAttributeValuesByProductIdAndName(
        @Param("productId") productId: String,
        @Param("attributeName") attributeName: String
    ): List<String>

    @Query("SELECT DISTINCT va.attributeName FROM VariantAttribute va WHERE va.productId = :productId")
    fun findAttributeNamesByProductId(@Param("productId") productId: String): List<String>

    fun deleteByProductId(productId: String)
}
