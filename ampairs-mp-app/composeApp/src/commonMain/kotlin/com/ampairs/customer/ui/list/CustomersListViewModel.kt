package com.ampairs.customer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.CustomerListKey
import com.ampairs.customer.domain.CustomerStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

data class CustomersListUiState(
    val customers: List<CustomerListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class CustomersListViewModel(
    private val customerStore: CustomerStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersListUiState())
    val uiState: StateFlow<CustomersListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSearchQuery()
    }

    fun loadCustomers() {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val key = CustomerListKey(workspaceId, _uiState.value.searchQuery)
                customerStore.customerListStore
                    .stream(StoreReadRequest.cached(key, refresh = false))
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _uiState.update {
                                    it.copy(
                                        customers = response.value,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            }
                            is StoreReadResponse.Loading -> {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            is StoreReadResponse.Error.Exception -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.error.message ?: "Unknown error"
                                    )
                                }
                            }
                            is StoreReadResponse.Error.Message -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.message
                                    )
                                }
                            }
                            else -> {
                                // Handle other response types if needed
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customers"
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun syncCustomers() {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = customerStore.syncCustomers(workspaceId)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Sync failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Sync failed"
                    )
                }
            }
        }
    }

    private fun observeSearchQuery() {
        uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .onEach { query ->
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    performSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun performSearch(query: String) {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        try {
            val key = CustomerListKey(workspaceId, query)
            customerStore.customerListStore
                .stream(StoreReadRequest.cached(key, refresh = false))
                .catch { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Search failed")
                    }
                }
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    customers = response.value,
                                    error = null
                                )
                            }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _uiState.update {
                                it.copy(error = response.error.message ?: "Search failed")
                            }
                        }
                        is StoreReadResponse.Error.Message -> {
                            _uiState.update {
                                it.copy(error = response.message)
                            }
                        }
                        else -> {
                            // Handle other response types if needed
                        }
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = e.message ?: "Search failed")
            }
        }
    }
}