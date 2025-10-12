package com.ampairs.unit.repository

import com.ampairs.unit.domain.model.UnitConversion
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface UnitConversionRepository : CrudRepository<UnitConversion, Long> {

    fun findByUid(uid: String): UnitConversion?

    @EntityGraph("UnitConversion.withUnits")
    fun findByProductIdAndActiveTrue(productId: String): List<UnitConversion>

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
