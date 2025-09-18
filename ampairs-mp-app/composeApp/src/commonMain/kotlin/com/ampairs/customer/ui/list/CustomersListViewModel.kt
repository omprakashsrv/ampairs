package com.ampairs.customer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.CustomerStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
                customerStore.observeCustomers(workspaceId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { customers ->
                        _uiState.update {
                            it.copy(
                                customers = customers,
                                isLoading = false,
                                error = null
                            )
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
            val customersFlow = if (query.isBlank()) {
                customerStore.observeCustomers(workspaceId)
            } else {
                customerStore.searchCustomers(workspaceId, query)
            }

            customersFlow
                .catch { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Search failed")
                    }
                }
                .collect { customers ->
                    _uiState.update {
                        it.copy(
                            customers = customers,
                            error = null
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = e.message ?: "Search failed")
            }
        }
    }
}