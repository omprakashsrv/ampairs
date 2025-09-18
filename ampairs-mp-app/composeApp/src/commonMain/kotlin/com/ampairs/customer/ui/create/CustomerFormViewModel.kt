package com.ampairs.customer.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class CustomerFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val countryCode: Int = 91,
    val gstin: String = "",
    val address: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    val nameError: String? = null,
    val emailError: String? = null
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null &&
                emailError == null
    }

    fun toCustomer(id: String = "", workspaceId: String = ""): Customer {
        return Customer(
            id = id,
            name = name.trim(),
            email = email.trim().takeIf { it.isNotBlank() },
            phone = phone.trim().takeIf { it.isNotBlank() },
            countryCode = countryCode,
            gstin = gstin.trim().takeIf { it.isNotBlank() },
            address = address.trim().takeIf { it.isNotBlank() },
            street = street.trim().takeIf { it.isNotBlank() },
            city = city.trim().takeIf { it.isNotBlank() },
            state = state.trim().takeIf { it.isNotBlank() },
            pincode = pincode.trim().takeIf { it.isNotBlank() },
            country = country.trim(),
            workspaceId = workspaceId
        )
    }
}

data class CustomerFormUiState(
    val formState: CustomerFormState = CustomerFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false
)

class CustomerFormViewModel(
    private val customerId: String?,
    private val customerStore: CustomerStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    private var originalCustomer: Customer? = null

    init {
        observeFormValidation()
    }

    fun loadCustomer() {
        if (customerId == null) return

        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerStore.observeCustomer(workspaceId, customerId)
                    .collect { customer ->
                        if (customer != null) {
                            originalCustomer = customer
                            _uiState.update {
                                it.copy(
                                    formState = customer.toFormState(),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Customer not found"
                                )
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

    fun updateForm(formState: CustomerFormState) {
        val validatedForm = validateForm(formState)
        _uiState.update {
            it.copy(
                formState = validatedForm,
                error = null
            )
        }
    }

    fun saveCustomer(onSuccess: () -> Unit) {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return
        val currentFormState = _uiState.value.formState

        if (!currentFormState.isValid()) {
            _uiState.update {
                it.copy(error = "Please fix the errors before saving")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val result = if (customerId == null) {
                    // Create new customer
                    val newCustomer = currentFormState.toCustomer(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspaceId
                    )
                    customerStore.createCustomer(workspaceId, newCustomer)
                } else {
                    // Update existing customer
                    val updatedCustomer = originalCustomer?.copy(
                        name = currentFormState.name.trim(),
                        email = currentFormState.email.trim().takeIf { it.isNotBlank() },
                        phone = currentFormState.phone.trim().takeIf { it.isNotBlank() },
                        countryCode = currentFormState.countryCode,
                        gstin = currentFormState.gstin.trim().takeIf { it.isNotBlank() },
                        address = currentFormState.address.trim().takeIf { it.isNotBlank() },
                        street = currentFormState.street.trim().takeIf { it.isNotBlank() },
                        city = currentFormState.city.trim().takeIf { it.isNotBlank() },
                        state = currentFormState.state.trim().takeIf { it.isNotBlank() },
                        pincode = currentFormState.pincode.trim().takeIf { it.isNotBlank() },
                        country = currentFormState.country.trim(),
                        workspaceId = workspaceId
                    ) ?: return@launch

                    customerStore.updateCustomer(workspaceId, updatedCustomer)
                }

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save customer"
                    )
                }
            }
        }
    }

    private fun observeFormValidation() {
        uiState
            .map { it.formState }
            .distinctUntilChanged()
            .onEach { formState ->
                _uiState.update {
                    it.copy(canSave = formState.isValid())
                }
            }
            .launchIn(viewModelScope)
    }

    private fun validateForm(formState: CustomerFormState): CustomerFormState {
        val nameError = when {
            formState.name.isBlank() -> "Name is required"
            formState.name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val emailError = if (formState.email.isNotBlank() && !isValidEmail(formState.email)) {
            "Please enter a valid email address"
        } else null

        return formState.copy(
            nameError = nameError,
            emailError = emailError
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}

private fun Customer.toFormState(): CustomerFormState {
    return CustomerFormState(
        name = name,
        email = email ?: "",
        phone = phone ?: "",
        countryCode = countryCode,
        gstin = gstin ?: "",
        address = address ?: "",
        street = street ?: "",
        city = city ?: "",
        state = state ?: "",
        pincode = pincode ?: "",
        country = country
    )
}