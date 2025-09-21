package com.ampairs.customer.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.StateStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4

data class StateFormState(
    val name: String = "",
    val nameError: String? = null,
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null
    }

    fun toState(id: String = ""): State {
        return State(
            id = id,
            name = name.trim(),
        )
    }
}

data class StateFormUiState(
    val formState: StateFormState = StateFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false
)

class StateFormViewModel(
    private val stateId: String?,
    private val stateStore: StateStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StateFormUiState())
    val uiState: StateFlow<StateFormUiState> = _uiState.asStateFlow()

    private var originalState: State? = null

    init {
        observeFormValidation()
        loadState()
    }

    fun loadState() {
        if (stateId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val state = stateStore.getStateById(stateId)
                if (state != null) {
                    originalState = state
                    _uiState.update {
                        it.copy(
                            formState = state.toFormState(),
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "State not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load state"
                    )
                }
            }
        }
    }

    fun updateForm(formState: StateFormState) {
        val validatedForm = validateForm(formState)
        _uiState.update {
            it.copy(
                formState = validatedForm,
                error = null
            )
        }
    }

    fun saveState(onSuccess: () -> Unit) {
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
                val result = if (stateId == null) {
                    // Create new state
                    val newState = currentFormState.toState(id = uuid4().toString())
                    stateStore.createState(newState)
                } else {
                    // Update existing state
                    val updatedState = originalState?.copy(
                        name = currentFormState.name.trim(),
                    ) ?: return@launch

                    stateStore.updateState(updatedState)
                }

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save state"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save state"
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

    private fun validateForm(formState: StateFormState): StateFormState {
        val nameError = when {
            formState.name.isBlank() -> "State name is required"
            formState.name.length < 2 -> "State name must be at least 2 characters"
            else -> null
        }

        return formState.copy(
            nameError = nameError,
        )
    }
}

private fun State.toFormState(): StateFormState {
    return StateFormState(
        name = name,
        nameError = null,
    )
}