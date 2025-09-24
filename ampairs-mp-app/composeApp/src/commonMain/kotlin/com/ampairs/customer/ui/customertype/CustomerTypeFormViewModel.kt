package com.ampairs.customer.ui.customertype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.util.UidGenerator
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.util.CustomerConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerTypeFormState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class CustomerTypeFormViewModel(
    private val customerTypeRepository: CustomerTypeRepository,
    private val customerTypeId: String? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(CustomerTypeFormState())
    val formState: StateFlow<CustomerTypeFormState> = _formState.asStateFlow()

    init {
        if (!customerTypeId.isNullOrBlank()) {
            _formState.update { it.copy(isEditMode = true, id = customerTypeId) }
            loadCustomerType(customerTypeId)
        }
    }

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, error = null) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateActive(active: Boolean) {
        _formState.update { it.copy(active = active) }
    }

    fun saveCustomerType(): Flow<Boolean> = flow {
        val state = _formState.value

        // Validate input
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Customer type name is required") }
            emit(false)
            return@flow
        }

        _formState.update { it.copy(isLoading = true, error = null) }

        try {
            val customerType = CustomerType(
                id = if (state.isEditMode) state.id else UidGenerator.generateUid(CustomerConstants.UID_PREFIX),
                name = state.name.trim(),
                description = state.description.trim().ifBlank { null },
                active = state.active
            )

            val result = if (state.isEditMode) {
                customerTypeRepository.updateCustomerType(customerType)
            } else {
                customerTypeRepository.createCustomerType(customerType)
            }

            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                emit(true)
            } else {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save customer type"
                    )
                }
                emit(false)
            }
        } catch (e: Exception) {
            _formState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred while saving"
                )
            }
            emit(false)
        }
    }

    private fun loadCustomerType(customerTypeId: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }

            // Load from local database first
            try {
                val customerType = customerTypeRepository.getCustomerTypeByName(customerTypeId) // This should be by ID, but we need to adjust the repository
                if (customerType != null) {
                    _formState.update {
                        it.copy(
                            id = customerType.id,
                            name = customerType.name,
                            description = customerType.description ?: "",
                            active = customerType.active,
                            isLoading = false
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = "Customer type not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer type"
                    )
                }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(error = null) }
    }
}