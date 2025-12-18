package com.ampairs.unit.repository

import com.ampairs.unit.domain.model.UnitConversion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UnitConversionRepository : CrudRepository<UnitConversion, Long> {

    // Basic queries
    fun findByUid(uid: String): UnitConversion?

    // Pagination with owner isolation
    @EntityGraph("UnitConversion.withUnits")
    @Query("SELECT uc FROM unit_conversion uc WHERE uc.ownerId = :ownerId")
    fun findAllByOwnerId(@Param("ownerId") ownerId: String, pageable: Pageable): Page<UnitConversion>

    // Sync query: get conversions updated after timestamp
    @EntityGraph("UnitConversion.withUnits")
    @Query("SELECT uc FROM unit_conversion uc WHERE uc.ownerId = :ownerId AND uc.updatedAt > :updatedAt ORDER BY uc.updatedAt ASC")
    fun findByOwnerIdAndUpdatedAtAfter(
        @Param("ownerId") ownerId: String,
        @Param("updatedAt") updatedAt: Instant,
        pageable: Pageable
    ): Page<UnitConversion>

    // Filter by product
    @EntityGraph("UnitConversion.withUnits")
    @Query("SELECT uc FROM unit_conversion uc WHERE uc.ownerId = :ownerId AND uc.productId = :productId")
    fun findByOwnerIdAndProductId(
        @Param("ownerId") ownerId: String,
        @Param("productId") productId: String,
        pageable: Pageable
    ): Page<UnitConversion>

    // Filter by base unit
    @EntityGraph("UnitConversion.withUnits")
    @Query("SELECT uc FROM unit_conversion uc WHERE uc.ownerId = :ownerId AND uc.baseUnitId = :baseUnitId")
    fun findByOwnerIdAndBaseUnitId(
        @Param("ownerId") ownerId: String,
        @Param("baseUnitId") baseUnitId: String,
        pageable: Pageable
    ): Page<UnitConversion>

    @EntityGraph("UnitConversion.withUnits")
    fun findByProductIdAndActiveTrue(productId: String): List<UnitConversion>

    // Find specific conversion for product and units
    @EntityGraph("UnitConversion.withUnits")
    @Query(
        """
        SELECT uc FROM unit_conversion uc
        WHERE uc.ownerId = :ownerId
          AND uc.productId = :productId
          AND uc.baseUnitId = :baseUnitId
          AND uc.derivedUnitId = :derivedUnitId
          AND uc.active = true
    """
    )
    fun findConversionByProductAndUnits(
        @Param("ownerId") ownerId: String,
        @Param("productId") productId: String,
        @Param("baseUnitId") baseUnitId: String,
        @Param("derivedUnitId") derivedUnitId: String
    ): UnitConversion?

    @EntityGraph("UnitConversion.withUnits")
    @Query(
        """
        select uc from unit_conversion uc
        where uc.active = true
          and uc.baseUnitId = :baseUnitId
          and uc.derivedUnitId = :derivedUnitId
          and ((:productId is null and uc.productId is null) or uc.productId = :productId)
    """
    )
    fun findActiveExactConversion(baseUnitId: String, derivedUnitId: String, productId: String?): UnitConversion?

    // Uniqueness check
    fun existsByOwnerIdAndProductIdAndBaseUnitIdAndDerivedUnitId(
        ownerId: String,
        productId: String,
        baseUnitId: String,
        derivedUnitId: String
    ): Boolean

    fun existsByOwnerIdAndProductIdAndBaseUnitIdAndDerivedUnitIdAndUidNot(
        ownerId: String,
        productId: String,
        baseUnitId: String,
        derivedUnitId: String,
        uid: String
    ): Boolean

    @EntityGraph("UnitConversion.withUnits")
    @Query(
        """
        select uc from unit_conversion uc
        where uc.active = true
          and (uc.baseUnitId = :unitId or uc.derivedUnitId = :unitId)
    """
    )
    fun findAllActiveLinkedToUnit(unitId: String): List<UnitConversion>

    @EntityGraph("UnitConversion.withUnits")
    @Query("select uc from unit_conversion uc where uc.active = true")
    fun findAllActive(): List<UnitConversion>
}
