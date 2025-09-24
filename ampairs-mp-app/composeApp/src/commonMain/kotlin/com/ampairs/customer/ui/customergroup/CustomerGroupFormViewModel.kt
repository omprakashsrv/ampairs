package com.ampairs.customer.ui.customergroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.util.UidGenerator
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.util.CustomerConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerGroupFormState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val discountPercentage: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class CustomerGroupFormViewModel(
    private val customerGroupRepository: CustomerGroupRepository,
    private val customerGroupId: String? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(CustomerGroupFormState())
    val formState: StateFlow<CustomerGroupFormState> = _formState.asStateFlow()

    init {
        if (!customerGroupId.isNullOrBlank()) {
            _formState.update { it.copy(isEditMode = true, id = customerGroupId) }
            loadCustomerGroup(customerGroupId)
        }
    }

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, error = null) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateDiscountPercentage(discountPercentage: String) {
        // Validate numeric input
        if (discountPercentage.isEmpty() || discountPercentage.toDoubleOrNull() != null) {
            _formState.update { it.copy(discountPercentage = discountPercentage) }
        }
    }

    fun updateActive(active: Boolean) {
        _formState.update { it.copy(active = active) }
    }

    fun saveCustomerGroup(): Flow<Boolean> = flow {
        val state = _formState.value

        // Validate input
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Customer group name is required") }
            emit(false)
            return@flow
        }

        // Validate discount percentage
        val discountPercentage = if (state.discountPercentage.isNotBlank()) {
            val percentage = state.discountPercentage.toDoubleOrNull()
            if (percentage == null || percentage < 0 || percentage > 100) {
                _formState.update { it.copy(error = "Discount percentage must be between 0 and 100") }
                emit(false)
                return@flow
            }
            percentage
        } else null

        _formState.update { it.copy(isLoading = true, error = null) }

        try {
            val customerGroup = CustomerGroup(
                id = if (state.isEditMode) state.id else UidGenerator.generateUid(CustomerConstants.UID_PREFIX),
                name = state.name.trim(),
                description = state.description.trim().ifBlank { null },
                discountPercentage = discountPercentage,
                active = state.active
            )

            val result = if (state.isEditMode) {
                customerGroupRepository.updateCustomerGroup(customerGroup)
            } else {
                customerGroupRepository.createCustomerGroup(customerGroup)
            }

            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                emit(true)
            } else {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save customer group"
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

    private fun loadCustomerGroup(customerGroupId: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }

            // Load from local database first
            try {
                val customerGroup = customerGroupRepository.getCustomerGroupByName(customerGroupId) // This should be by ID, but we need to adjust the repository
                if (customerGroup != null) {
                    _formState.update {
                        it.copy(
                            id = customerGroup.id,
                            name = customerGroup.name,
                            description = customerGroup.description ?: "",
                            discountPercentage = customerGroup.discountPercentage?.toString() ?: "",
                            active = customerGroup.active,
                            isLoading = false
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = "Customer group not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer group"
                    )
                }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(error = null) }
    }
}