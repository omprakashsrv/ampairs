package com.ampairs.customer.ui.customergroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.domain.MasterCustomerGroup
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlinx.coroutines.FlowPreview

data class CustomerGroupListUiState(
    val customerGroups: List<CustomerGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableCustomerGroupsForImport: List<MasterCustomerGroup> = emptyList(),
    val isLoadingImportCustomerGroups: Boolean = false
)

class CustomerGroupListViewModel(
    private val customerGroupRepository: CustomerGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerGroupListUiState())
    val uiState: StateFlow<CustomerGroupListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refreshCustomerGroups()
        observeSearchQuery()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteCustomerGroup(customerGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerGroupRepository.deleteCustomerGroup(customerGroupId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete customer group")
                }
            } else {
                // Refresh the list after successful deletion
                refreshCustomerGroups()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300) // Wait 300ms after the user stops typing
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    searchCustomerGroups(query)
                } else {
                    refreshCustomerGroups()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshCustomerGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerGroupRepository.getCustomerGroupsFlow(page = 0, size = 100, forceRefresh = true)
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    customerGroups = response.value,
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
                                    error = response.error.message ?: "Failed to load customer groups"
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
        }
    }

    private fun searchCustomerGroups(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerGroupRepository.searchCustomerGroups(query)
                .collect { customerGroups ->
                    _uiState.update {
                        it.copy(
                            customerGroups = customerGroups,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun loadAvailableCustomerGroupsForImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImportCustomerGroups = true) }

            val result = customerGroupRepository.getAvailableCustomerGroupsForImport()
            _uiState.update {
                it.copy(
                    isLoadingImportCustomerGroups = false,
                    availableCustomerGroupsForImport = result.getOrElse { emptyList() }
                )
            }
        }
    }

    fun importCustomerGroup(masterCustomerGroup: MasterCustomerGroup) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerGroupRepository.importCustomerGroup(masterCustomerGroup)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer group")
                }
            } else {
                refreshCustomerGroups()
                loadAvailableCustomerGroupsForImport() // Refresh available list
            }
        }
    }

    fun bulkImportCustomerGroups(masterCustomerGroups: List<MasterCustomerGroup>) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerGroupRepository.bulkImportCustomerGroups(masterCustomerGroups)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer groups")
                }
            } else {
                refreshCustomerGroups()
                loadAvailableCustomerGroupsForImport() // Refresh available list
            }
        }
    }
}