package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.State
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StateRepository : CrudRepository<State, String> {

}