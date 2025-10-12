package com.ampairs.unit.repository

import com.ampairs.unit.domain.model.UnitConversion
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface UnitConversionRepository : CrudRepository<UnitConversion, Long> {

    fun findByUid(uid: String): UnitConversion?

    @EntityGraph("UnitConversion.withUnits")
    fun findByProductId(productId: String): List<UnitConversion>

    @EntityGraph("UnitConversion.withUnits")
    fun findByBaseUnitIdOrDerivedUnitId(baseUnitId: String, derivedUnitId: String): List<UnitConversion>

    @EntityGraph("UnitConversion.withUnits")
    @Query(
        """
        select uc from unit_conversion uc
        where uc.baseUnitId = :baseUnitId
          and uc.derivedUnitId = :derivedUnitId
          and ((:productId is null and uc.productId is null) or uc.productId = :productId)
    """
    )
    fun findExactConversion(baseUnitId: String, derivedUnitId: String, productId: String?): UnitConversion?

    @EntityGraph("UnitConversion.withUnits")
    @Query(
        """
        select uc from unit_conversion uc
        where uc.baseUnitId = :unitId or uc.derivedUnitId = :unitId
    """
    )
    fun findAllLinkedToUnit(unitId: String): List<UnitConversion>
}
