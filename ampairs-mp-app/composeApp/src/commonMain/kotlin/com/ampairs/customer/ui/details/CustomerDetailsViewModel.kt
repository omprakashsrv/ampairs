package com.ampairs.customer.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerKey
import com.ampairs.customer.domain.CustomerStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CustomerDetailsViewModel(
    private val customerId: String,
    private val customerStore: CustomerStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    fun loadCustomer() {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val key = CustomerKey(workspaceId, customerId)
                customerStore.customerStore
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
                                        customer = response.value,
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
                        error = e.message ?: "Failed to load customer"
                    )
                }
            }
        }
    }

    fun deleteCustomer(onSuccess: () -> Unit) {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = customerStore.deleteCustomer(workspaceId, customerId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to delete customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete customer"
                    )
                }
            }
        }
    }
}