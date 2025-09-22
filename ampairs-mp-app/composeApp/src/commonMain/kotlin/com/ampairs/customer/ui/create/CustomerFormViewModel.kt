package com.ampairs.customer.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.gstin.GstinValidationError
import com.ampairs.common.validation.gstin.GstinValidator
import com.ampairs.common.validation.phone.PhoneNumberValidationError
import com.ampairs.common.validation.phone.PhoneNumberValidator
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerAddress
import com.ampairs.customer.domain.CustomerKey
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateKey
import com.ampairs.customer.domain.StateStore
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

data class CustomerFormState(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val landline: String = "",
    val countryCode: Int = 91,
    val gstin: String = "",
    val address: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    // Billing Address
    val useBillingAsMainAddress: Boolean = true,
    val billingStreet: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingPincode: String = "",
    val billingCountry: String = "India",
    // Shipping Address
    val useShippingAsMainAddress: Boolean = true,
    val shippingStreet: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPincode: String = "",
    val shippingCountry: String = "India",
    // Validation errors
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val landlineError: String? = null,
    val gstinError: String? = null
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null &&
                emailError == null &&
                phoneError == null &&
                landlineError == null &&
                gstinError == null
    }

    fun toCustomer(): Customer {
        return Customer(
            uid = uid,
            name = name.trim(),
            email = email.trim().takeIf { it.isNotBlank() },
            phone = phone.trim().takeIf { it.isNotBlank() },
            landline = landline.trim().takeIf { it.isNotBlank() },
            countryCode = countryCode,
            gstin = gstin.trim().takeIf { it.isNotBlank() },
            address = address.trim().takeIf { it.isNotBlank() },
            street = street.trim().takeIf { it.isNotBlank() },
            city = city.trim().takeIf { it.isNotBlank() },
            state = state.trim().takeIf { it.isNotBlank() },
            pincode = pincode.trim().takeIf { it.isNotBlank() },
            country = country.trim(),
            billingAddress = if (useBillingAsMainAddress) {
                CustomerAddress(
                    street = street.trim(),
                    city = city.trim(),
                    state = state.trim(),
                    pincode = pincode.trim(),
                    country = country.trim()
                ).takeIf { street.isNotBlank() || city.isNotBlank() }
            } else {
                CustomerAddress(
                    street = billingStreet.trim(),
                    city = billingCity.trim(),
                    state = billingState.trim(),
                    pincode = billingPincode.trim(),
                    country = billingCountry.trim()
                ).takeIf { billingStreet.isNotBlank() || billingCity.isNotBlank() }
            },
            shippingAddress = if (useShippingAsMainAddress) {
                CustomerAddress(
                    street = street.trim(),
                    city = city.trim(),
                    state = state.trim(),
                    pincode = pincode.trim(),
                    country = country.trim()
                ).takeIf { street.isNotBlank() || city.isNotBlank() }
            } else {
                CustomerAddress(
                    street = shippingStreet.trim(),
                    city = shippingCity.trim(),
                    state = shippingState.trim(),
                    pincode = shippingPincode.trim(),
                    country = shippingCountry.trim()
                ).takeIf { shippingStreet.isNotBlank() || shippingCity.isNotBlank() }
            },
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
    val isLoadingStates: Boolean = false,
    val cities: List<String> = emptyList(),
    val pincodes: List<String> = emptyList(),
    val isLoadingCitiesAndPincodes: Boolean = false
)

class CustomerFormViewModel(
    private val customerId: String?,
    private val customerStore: CustomerStore,
    private val stateStore: StateStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerFormUiState())
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    private var originalCustomer: Customer? = null

    init {
        observeFormValidation()
        loadStates()
        loadCitiesAndPincodes()
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
                val result = if (currentFormState.uid.isBlank()) {
                    // Create new customer
                    val updatedFormState = currentFormState.copy(uid = uuid4().toString())
                    val newCustomer = updatedFormState.toCustomer()
                    customerStore.createCustomer(newCustomer)
                } else {
                    // Update existing customer
                    val updatedCustomer = currentFormState.toCustomer()
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
                                                    error = response.error.message
                                                        ?: "Failed to load states"
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

    private fun loadCitiesAndPincodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCitiesAndPincodes = true) }

            try {
                val cities = customerStore.getUniqueCities()
                val pincodes = customerStore.getUniquePincodes()

                _uiState.update {
                    it.copy(
                        cities = cities,
                        pincodes = pincodes,
                        isLoadingCitiesAndPincodes = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCitiesAndPincodes = false,
                        error = e.message ?: "Failed to load cities and pincodes"
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

        val landlineError = if (formState.landline.isNotBlank()) {
            when {
                !isValidLandline(formState.landline) -> "Please enter a valid landline number"
                else -> null
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
            landlineError = landlineError,
            gstinError = gstinError
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isValidLandline(landline: String): Boolean {
        // Indian landline format: STD Code (2-5 digits) + Local number (6-8 digits)
        // Examples: 080-12345678, 0120-1234567, 022-12345678
        val cleanedLandline = landline.replace(Regex("[\\s-()]"), "")

        // Check for basic landline pattern (6-13 digits total)
        if (!cleanedLandline.matches(Regex("^0?\\d{6,12}$"))) {
            return false
        }

        // Must start with 0 (STD code prefix) if more than 8 digits
        if (cleanedLandline.length > 8 && !cleanedLandline.startsWith("0")) {
            return false
        }

        return true
    }
}

private fun Customer.toFormState(): CustomerFormState {
    return CustomerFormState(
        uid = uid,
        name = name,
        email = email ?: "",
        phone = phone ?: "",
        landline = landline ?: "",
        countryCode = countryCode,
        gstin = gstin ?: "",
        address = address ?: "",
        street = street ?: "",
        city = city ?: "",
        state = state ?: "",
        pincode = pincode ?: "",
        country = country,
        // Billing Address
        useBillingAsMainAddress = billingAddress == null ||
                (billingAddress.street == street && billingAddress.city == city &&
                        billingAddress.state == state && billingAddress.pincode == pincode),
        billingStreet = billingAddress?.street ?: "",
        billingCity = billingAddress?.city ?: "",
        billingState = billingAddress?.state ?: "",
        billingPincode = billingAddress?.pincode ?: "",
        billingCountry = billingAddress?.country ?: "India",
        // Shipping Address
        useShippingAsMainAddress = shippingAddress == null ||
                (shippingAddress.street == street && shippingAddress.city == city &&
                        shippingAddress.state == state && shippingAddress.pincode == pincode),
        shippingStreet = shippingAddress?.street ?: "",
        shippingCity = shippingAddress?.city ?: "",
        shippingState = shippingAddress?.state ?: "",
        shippingPincode = shippingAddress?.pincode ?: "",
        shippingCountry = shippingAddress?.country ?: "India",
        // Validation errors
        nameError = null,
        emailError = null,
        phoneError = null,
        gstinError = null
    )
}