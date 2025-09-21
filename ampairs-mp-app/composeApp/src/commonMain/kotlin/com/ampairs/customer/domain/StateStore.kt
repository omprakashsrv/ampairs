package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.StateDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toDomainList
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class StateStore(
    private val customerApi: CustomerApi,
    private val stateDao: StateDao
) {
    val stateStore: Store<StateKey, List<State>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    emit(customerApi.getStates())
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { _: StateKey ->
                    stateDao.getAllStates().map { entities ->
                        entities.toDomainList()
                    }
                },
                writer = { _: StateKey, states: List<State> ->
                    stateDao.insertStates(states.map { it.toEntity() })
                }
            )
        )
        .build()

    fun searchStatesFlow(query: String): Flow<List<State>> {
        return stateDao.searchStates(query).map { entities ->
            entities.toDomainList()
        }
    }

    suspend fun getStateById(stateId: String): State? {
        return stateDao.getStateById(stateId)?.toDomain()
    }

    suspend fun createState(state: State): Result<State> {
        return try {
            val createdState = customerApi.createState(state)
            stateDao.insertState(createdState.toEntity())
            Result.success(createdState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateState(state: State): Result<State> {
        return try {
            val updatedState = customerApi.updateState(state)
            stateDao.updateState(updatedState.toEntity())
            Result.success(updatedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteState(stateId: String): Result<Unit> {
        return try {
            customerApi.deleteState(stateId)
            stateDao.deleteStateById(stateId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}