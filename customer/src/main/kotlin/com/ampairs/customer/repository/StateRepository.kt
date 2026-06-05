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
     * Find first state by master state code
     */
    fun findFirstByMasterStateCode(masterStateCode: String): State?
}