package com.ampairs.customer.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerKey
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateKey
import com.ampairs.customer.domain.StateStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import com.ampairs.common.validation.phone.PhoneNumberValidator
import com.ampairs.common.validation.phone.PhoneNumberValidationError
import com.ampairs.common.validation.gstin.GstinValidator
import com.ampairs.common.validation.gstin.GstinValidationError
import com.ampairs.common.validation.ValidationResult

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
    val emailError: String? = null,
    val phoneError: String? = null,
    val gstinError: String? = null
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null &&
                emailError == null &&
                phoneError == null &&
                gstinError == null
    }

    fun toCustomer(id: String = "", workspaceId: String = ""): Customer {
        return Customer(
            uid = id,
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
    val canSave: Boolean = false,
    val states: List<State> = emptyList(),
    val isLoadingStates: Boolean = false
)

class CustomerFormViewModel(
    private val customerId: String?,
    private val customerStore: CustomerStore,
    private val stateStore: StateStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    private var originalCustomer: Customer? = null

    init {
        observeFormValidation()
        loadStates()
    }

    fun loadCustomer() {
        if (customerId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val key = CustomerKey(customerId)
                customerStore.customerStore
                    .stream(StoreReadRequest.cached(key, refresh = false))
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                originalCustomer = response.value
                                _uiState.update {
                                    it.copy(
                                        formState = response.value.toFormState(),
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
                                        error = response.error.message ?: "Failed to load customer"
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
                        id = uuid4().toString(),
                        workspaceId = workspaceId
                    )
                    customerStore.createCustomer(newCustomer)
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

                    customerStore.updateCustomer(updatedCustomer)
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

    private fun loadStates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStates = true) }

            try {
                // First, try to get states from local DB
                stateStore.searchStatesFlow("")
                    .collect { localStates ->
                        if (localStates.isEmpty()) {
                            // If local DB is empty, fetch from backend API
                            val key = StateKey()
                            stateStore.stateStore
                                .stream(StoreReadRequest.cached(key, refresh = true))
                                .collect { response ->
                                    when (response) {
                                        is StoreReadResponse.Data -> {
                                            _uiState.update {
                                                it.copy(
                                                    states = response.value,
                                                    isLoadingStates = false
                                                )
                                            }
                                        }
                                        is StoreReadResponse.Loading -> {
                                            _uiState.update { it.copy(isLoadingStates = true) }
                                        }
                                        is StoreReadResponse.Error.Exception -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoadingStates = false,
                                                    error = response.error.message ?: "Failed to load states"
                                                )
                                            }
                                        }
                                        is StoreReadResponse.Error.Message -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoadingStates = false,
                                                    error = response.message
                                                )
                                            }
                                        }
                                        else -> {
                                            // Handle other response types if needed
                                        }
                                    }
                                }
                        } else {
                            // Use local states if available
                            _uiState.update {
                                it.copy(
                                    states = localStates,
                                    isLoadingStates = false
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingStates = false,
                        error = e.message ?: "Failed to load states"
                    )
                }
            }
        }
    }

    fun onStateSelected(state: State) {
        val currentFormState = _uiState.value.formState
        _uiState.update {
            it.copy(
                formState = currentFormState.copy(state = state.name),
                error = null
            )
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

        val phoneError = if (formState.phone.isNotBlank()) {
            when (val result = PhoneNumberValidator().validate(formState.phone)) {
                is ValidationResult.Invalid -> (result.errors.firstOrNull() as? PhoneNumberValidationError)?.message
                is ValidationResult.Valid -> null
            }
        } else null

        val gstinError = if (formState.gstin.isNotBlank()) {
            when (val result = GstinValidator().validate(formState.gstin)) {
                is ValidationResult.Invalid -> (result.errors.firstOrNull() as? GstinValidationError)?.message
                is ValidationResult.Valid -> null
            }
        } else null

        return formState.copy(
            nameError = nameError,
            emailError = emailError,
            phoneError = phoneError,
            gstinError = gstinError
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
        country = country,
        nameError = null,
        emailError = null,
        phoneError = null,
        gstinError = null
    )
}