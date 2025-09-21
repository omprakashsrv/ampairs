package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.State
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StateRepository : CrudRepository<State, String> {

    /**
     * Find state by UID
     */
    fun findByUid(uid: String): State?

    /**
     * Find states by workspace/owner ID
     */
    fun findByOwnerId(ownerId: String): List<State>

    /**
     * Find state by master state code and workspace
     */
    fun findByMasterStateCodeAndOwnerId(masterStateCode: String, ownerId: String): State?

    /**
     * Find all states by master state code
     */
    fun findByMasterStateCode(masterStateCode: String): List<State>

    /**
     * Find active states for a workspace
     */
    fun findByOwnerIdAndActiveTrue(ownerId: String): List<State>

    /**
     * Find states by name pattern for a workspace
     */
    fun findByOwnerIdAndNameContainingIgnoreCase(ownerId: String, namePattern: String): List<State>
}