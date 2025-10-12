package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessStore
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class BusinessProfileUiState(
    val business: Business? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val lastSyncedEpochMillis: Long? = null
)

class BusinessProfileViewModel(
    private val businessStore: BusinessStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()

    init {
        observeBusiness()
    }

    private fun observeBusiness() {
        businessStore.observeBusiness()
            .onEach { business ->
                _uiState.value = _uiState.value.copy(
                    business = business,
                    error = null
                )
            }
            .launchIn(viewModelScope)
    }

    fun refresh(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = businessStore.refresh()
            _uiState.value = result.fold(
                onSuccess = { business ->
                    _uiState.value.copy(
                        business = business,
                        isLoading = false,
                        error = null,
                        lastSyncedEpochMillis = currentTime()
                    )
                },
                onFailure = { error ->
                    _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to refresh business profile"
                    )
                }
            )
        }
    }

    fun saveBusiness(updatedBusiness: Business) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = businessStore.upsertBusiness(updatedBusiness)
            _uiState.value = result.fold(
                onSuccess = { business ->
                    _uiState.value.copy(
                        business = business,
                        isSaving = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unable to save business profile"
                    )
                }
            )
        }
    }

    fun syncPending() {
        viewModelScope.launch {
            val result = businessStore.syncPending()
            result.onSuccess { synced ->
                if (synced) {
                    _uiState.value = _uiState.value.copy(
                        lastSyncedEpochMillis = currentTime(),
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to sync business changes"
                )
            }
        }
    }

    fun hasWorkspaceContext(): Boolean = workspaceContextManager.currentWorkspace.value != null

    @OptIn(ExperimentalTime::class)
    private fun currentTime(): Long = Clock.System.now().toEpochMilliseconds()
}
