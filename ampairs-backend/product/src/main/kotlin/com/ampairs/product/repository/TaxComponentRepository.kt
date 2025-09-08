package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxComponent
import com.ampairs.product.domain.model.TaxComponentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.LocalDateTime
import java.util.*

interface TaxComponentRepository : CrudRepository<TaxComponent, Long>, PagingAndSortingRepository<TaxComponent, Long> {
    fun findByUid(uid: String): Optional<TaxComponent>
    fun findByComponentCode(componentCode: String): Optional<TaxComponent>
    fun findByComponentType(componentType: TaxComponentType): List<TaxComponent>
    fun findByActive(active: Boolean): List<TaxComponent>
    fun findByActiveOrderByCalculationOrder(active: Boolean): List<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.active = true AND tc.componentType IN :types ORDER BY tc.calculationOrder")
    fun findActiveByComponentTypesOrderByCalculationOrder(types: List<TaxComponentType>): List<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.active = true AND (tc.applicableFrom IS NULL OR tc.applicableFrom <= :date) AND (tc.applicableTo IS NULL OR tc.applicableTo > :date)")
    fun findActiveForDate(date: LocalDateTime): List<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.stateCodes LIKE %:stateCode% AND tc.active = true")
    fun findByStateCode(stateCode: String): List<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.businessTypes LIKE %:businessType% AND tc.active = true")
    fun findByBusinessType(businessType: String): List<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.componentName ILIKE %:searchTerm% OR tc.description ILIKE %:searchTerm%")
    fun searchComponents(searchTerm: String, pageable: Pageable): Page<TaxComponent>

    @Query("SELECT tc FROM tax_component tc WHERE tc.isCompound = true AND tc.active = true ORDER BY tc.calculationOrder")
    fun findCompoundComponents(): List<TaxComponent>

    @Query("SELECT DISTINCT tc.componentType FROM tax_component tc WHERE tc.active = true")
    fun findDistinctActiveComponentTypes(): List<TaxComponentType>

    @Query("SELECT tc FROM tax_component tc WHERE tc.defaultRate BETWEEN :minRate AND :maxRate AND tc.active = true")
    fun findByRateRange(minRate: Double, maxRate: Double): List<TaxComponent>
}