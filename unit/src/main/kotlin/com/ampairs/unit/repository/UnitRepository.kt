package com.ampairs.unit.repository

import com.ampairs.unit.domain.model.Unit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UnitRepository : CrudRepository<Unit, Long> {

    // Basic queries
    fun findByUid(uid: String?): Unit?
    fun findByRefId(refId: String?): Unit?

    // Pagination with owner isolation
    @EntityGraph("Unit.basic")
    @Query("SELECT u FROM unit u WHERE u.ownerId = :ownerId ORDER BY u.name")
    fun findAllByOwnerId(@Param("ownerId") ownerId: String, pageable: Pageable): Page<Unit>

    // Sync query: get units updated after timestamp
    @EntityGraph("Unit.basic")
    @Query("SELECT u FROM unit u WHERE u.ownerId = :ownerId AND u.updatedAt > :updatedAt ORDER BY u.updatedAt ASC")
    fun findByOwnerIdAndUpdatedAtAfter(
        @Param("ownerId") ownerId: String,
        @Param("updatedAt") updatedAt: Instant,
        pageable: Pageable
    ): Page<Unit>

    // Search by name or shortName
    @EntityGraph("Unit.basic")
    @Query("""
        SELECT u FROM unit u
        WHERE u.ownerId = :ownerId
        AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.shortName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun searchByNameOrShortName(
        @Param("ownerId") ownerId: String,
        @Param("search") search: String,
        pageable: Pageable
    ): Page<Unit>

    // Filter by category
    @EntityGraph("Unit.basic")
    @Query("SELECT u FROM unit u WHERE u.ownerId = :ownerId AND u.category = :category")
    fun findByOwnerIdAndCategory(
        @Param("ownerId") ownerId: String,
        @Param("category") category: String,
        pageable: Pageable
    ): Page<Unit>

    // Filter by active status
    @EntityGraph("Unit.basic")
    @Query("SELECT u FROM unit u WHERE u.ownerId = :ownerId AND u.active = :active")
    fun findByOwnerIdAndActive(
        @Param("ownerId") ownerId: String,
        @Param("active") active: Boolean,
        pageable: Pageable
    ): Page<Unit>

    // Uniqueness checks for tenant isolation
    fun existsByOwnerIdAndNameIgnoreCase(ownerId: String, name: String): Boolean
    fun existsByOwnerIdAndShortNameIgnoreCase(ownerId: String, shortName: String): Boolean
    fun existsByOwnerIdAndNameIgnoreCaseAndUidNot(ownerId: String, name: String, uid: String): Boolean
    fun existsByOwnerIdAndShortNameIgnoreCaseAndUidNot(ownerId: String, shortName: String, uid: String): Boolean

    // Check if unit is in use (for safe deletion)
    @Query("""
        SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END
        FROM unit_conversion uc
        WHERE uc.baseUnitId = :unitId OR uc.derivedUnitId = :unitId
    """)
    fun isUnitInUse(@Param("unitId") unitId: String): Boolean

    @EntityGraph("Unit.basic")
    fun findAllByActiveTrueOrderByName(): List<Unit>

    @Query("SELECT u FROM unit u WHERE u.active = true ORDER BY u.name")
    fun findAllActive(): List<Unit>
}
