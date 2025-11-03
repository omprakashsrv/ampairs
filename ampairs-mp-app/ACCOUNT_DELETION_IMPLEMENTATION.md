# Account Deletion Implementation Guide - Kotlin Multiplatform Mobile App

## Overview

This guide provides step-by-step instructions for implementing the account deletion feature in the Ampairs Kotlin Multiplatform (KMP) application for Android, iOS, and Desktop.

---

## 📋 Prerequisites

- Kotlin Multiplatform project configured
- HTTP client (Ktor) installed
- Navigation (Voyager/Compose Navigation) configured
- JWT authentication implemented
- Compose UI dependencies

---

## 🏗️ Architecture

```
commonMain/
├── data/
│   ├── api/
│   │   └── AccountDeletionApi.kt
│   ├── model/
│   │   └── AccountDeletionModels.kt
│   └── repository/
│       └── AccountDeletionRepository.kt
├── domain/
│   └── usecase/
│       ├── RequestAccountDeletionUseCase.kt
│       ├── CancelAccountDeletionUseCase.kt
│       └── GetAccountDeletionStatusUseCase.kt
└── presentation/
    └── settings/
        ├── DeleteAccountScreen.kt
        ├── DeleteAccountViewModel.kt
        ├── components/
        │   ├── DeleteConfirmationDialog.kt
        │   └── BlockingWorkspacesDialog.kt
        └── DeleteAccountState.kt
```

---

## 📦 Data Models

**File:** `commonMain/kotlin/com/ampairs/data/model/AccountDeletionModels.kt`

```kotlin
package com.ampairs.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails? = null,
    val timestamp: String,
    val path: String? = null,
    @SerialName("trace_id") val traceId: String? = null
)

@Serializable
data class ErrorDetails(
    val message: String,
    val code: String? = null
)

@Serializable
data class AccountDeletionRequest(
    val reason: String? = null,
    val confirmed: Boolean
)

@Serializable
data class AccountDeletionResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("deletion_requested") val deletionRequested: Boolean,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deletion_scheduled_for") val deletionScheduledFor: String? = null,
    @SerialName("days_until_permanent_deletion") val daysUntilPermanentDeletion: Long? = null,
    val message: String,
    @SerialName("blocking_workspaces") val blockingWorkspaces: List<WorkspaceOwnershipInfo>? = null,
    @SerialName("can_restore") val canRestore: Boolean
)

@Serializable
data class AccountDeletionStatusResponse(
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deletion_scheduled_for") val deletionScheduledFor: String? = null,
    @SerialName("days_remaining") val daysRemaining: Long? = null,
    @SerialName("can_restore") val canRestore: Boolean,
    @SerialName("deletion_reason") val deletionReason: String? = null,
    @SerialName("status_message") val statusMessage: String
)

@Serializable
data class WorkspaceOwnershipInfo(
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("workspace_name") val workspaceName: String,
    @SerialName("workspace_slug") val workspaceSlug: String,
    @SerialName("member_count") val memberCount: Int
)
```

---

## 🌐 API Client

**File:** `commonMain/kotlin/com/ampairs/data/api/AccountDeletionApi.kt`

```kotlin
package com.ampairs.data.api

import com.ampairs.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AccountDeletionApi(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    private val accountEndpoint = "$baseUrl/api/v1/account"

    suspend fun requestAccountDeletion(
        request: AccountDeletionRequest,
        token: String
    ): ApiResponse<AccountDeletionResponse> {
        return httpClient.post("$accountEndpoint/delete-request") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }.body()
    }

    suspend fun cancelAccountDeletion(
        token: String
    ): ApiResponse<AccountDeletionResponse> {
        return httpClient.post("$accountEndpoint/delete-cancel") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
        }.body()
    }

    suspend fun getAccountDeletionStatus(
        token: String
    ): ApiResponse<AccountDeletionStatusResponse> {
        return httpClient.get("$accountEndpoint/delete-status") {
            bearerAuth(token)
        }.body()
    }
}
```

---

## 📚 Repository

**File:** `commonMain/kotlin/com/ampairs/data/repository/AccountDeletionRepository.kt`

```kotlin
package com.ampairs.data.repository

import com.ampairs.data.api.AccountDeletionApi
import com.ampairs.data.model.*

class AccountDeletionRepository(
    private val api: AccountDeletionApi,
    private val tokenProvider: () -> String?
) {
    suspend fun requestAccountDeletion(
        reason: String? = null
    ): Result<AccountDeletionResponse> = runCatching {
        val token = tokenProvider() ?: throw IllegalStateException("No auth token")
        val request = AccountDeletionRequest(
            reason = reason,
            confirmed = true
        )
        val response = api.requestAccountDeletion(request, token)
        if (response.success && response.data != null) {
            response.data
        } else {
            throw Exception(response.error?.message ?: "Unknown error")
        }
    }

    suspend fun cancelAccountDeletion(): Result<AccountDeletionResponse> = runCatching {
        val token = tokenProvider() ?: throw IllegalStateException("No auth token")
        val response = api.cancelAccountDeletion(token)
        if (response.success && response.data != null) {
            response.data
        } else {
            throw Exception(response.error?.message ?: "Unknown error")
        }
    }

    suspend fun getAccountDeletionStatus(): Result<AccountDeletionStatusResponse> = runCatching {
        val token = tokenProvider() ?: throw IllegalStateException("No auth token")
        val response = api.getAccountDeletionStatus(token)
        if (response.success && response.data != null) {
            response.data
        } else {
            throw Exception(response.error?.message ?: "Unknown error")
        }
    }
}
```

---

## 🎯 Use Cases

**File:** `commonMain/kotlin/com/ampairs/domain/usecase/RequestAccountDeletionUseCase.kt`

```kotlin
package com.ampairs.domain.usecase

import com.ampairs.data.model.AccountDeletionResponse
import com.ampairs.data.repository.AccountDeletionRepository

class RequestAccountDeletionUseCase(
    private val repository: AccountDeletionRepository
) {
    suspend operator fun invoke(reason: String? = null): Result<AccountDeletionResponse> {
        return repository.requestAccountDeletion(reason)
    }
}
```

**File:** `commonMain/kotlin/com/ampairs/domain/usecase/CancelAccountDeletionUseCase.kt`

```kotlin
package com.ampairs.domain.usecase

import com.ampairs.data.model.AccountDeletionResponse
import com.ampairs.data.repository.AccountDeletionRepository

class CancelAccountDeletionUseCase(
    private val repository: AccountDeletionRepository
) {
    suspend operator fun invoke(): Result<AccountDeletionResponse> {
        return repository.cancelAccountDeletion()
    }
}
```

**File:** `commonMain/kotlin/com/ampairs/domain/usecase/GetAccountDeletionStatusUseCase.kt`

```kotlin
package com.ampairs.domain.usecase

import com.ampairs.data.model.AccountDeletionStatusResponse
import com.ampairs.data.repository.AccountDeletionRepository

class GetAccountDeletionStatusUseCase(
    private val repository: AccountDeletionRepository
) {
    suspend operator fun invoke(): Result<AccountDeletionStatusResponse> {
        return repository.getAccountDeletionStatus()
    }
}
```

---

## 🎨 UI State

**File:** `commonMain/kotlin/com/ampairs/presentation/settings/DeleteAccountState.kt`

```kotlin
package com.ampairs.presentation.settings

import com.ampairs.data.model.AccountDeletionStatusResponse
import com.ampairs.data.model.WorkspaceOwnershipInfo

sealed interface DeleteAccountState {
    object Loading : DeleteAccountState

    data class Active(
        val reason: String = "",
        val isProcessing: Boolean = false
    ) : DeleteAccountState

    data class PendingDeletion(
        val status: AccountDeletionStatusResponse,
        val isProcessing: Boolean = false
    ) : DeleteAccountState

    data class Error(
        val message: String,
        val previousState: DeleteAccountState? = null
    ) : DeleteAccountState
}

sealed interface DeleteAccountEvent {
    data class ShowBlockingWorkspaces(
        val workspaces: List<WorkspaceOwnershipInfo>
    ) : DeleteAccountEvent

    data class ShowSuccess(
        val message: String,
        val shouldLogout: Boolean
    ) : DeleteAccountEvent

    data class ShowError(
        val message: String
    ) : DeleteAccountEvent
}
```

---

## 🎬 ViewModel

**File:** `commonMain/kotlin/com/ampairs/presentation/settings/DeleteAccountViewModel.kt`

```kotlin
package com.ampairs.presentation.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.ampairs.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DeleteAccountViewModel(
    private val requestAccountDeletionUseCase: RequestAccountDeletionUseCase,
    private val cancelAccountDeletionUseCase: CancelAccountDeletionUseCase,
    private val getAccountDeletionStatusUseCase: GetAccountDeletionStatusUseCase
) : ScreenModel {

    private val _state = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Loading)
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DeleteAccountEvent>()
    val events: SharedFlow<DeleteAccountEvent> = _events.asSharedFlow()

    init {
        loadDeletionStatus()
    }

    fun loadDeletionStatus() {
        screenModelScope.launch {
            _state.value = DeleteAccountState.Loading

            getAccountDeletionStatusUseCase().fold(
                onSuccess = { status ->
                    _state.value = if (status.isDeleted) {
                        DeleteAccountState.PendingDeletion(status)
                    } else {
                        DeleteAccountState.Active()
                    }
                },
                onFailure = { error ->
                    _state.value = DeleteAccountState.Error(
                        message = error.message ?: "Failed to load deletion status"
                    )
                }
            )
        }
    }

    fun updateReason(reason: String) {
        val currentState = _state.value
        if (currentState is DeleteAccountState.Active) {
            _state.value = currentState.copy(reason = reason)
        }
    }

    fun requestAccountDeletion(reason: String?) {
        screenModelScope.launch {
            val currentState = _state.value
            if (currentState !is DeleteAccountState.Active) return@launch

            _state.value = currentState.copy(isProcessing = true)

            requestAccountDeletionUseCase(reason).fold(
                onSuccess = { response ->
                    if (response.deletionRequested) {
                        // Success - show message and logout
                        _events.emit(
                            DeleteAccountEvent.ShowSuccess(
                                message = response.message,
                                shouldLogout = true
                            )
                        )
                    } else if (!response.blockingWorkspaces.isNullOrEmpty()) {
                        // Blocked by workspace ownership
                        _events.emit(
                            DeleteAccountEvent.ShowBlockingWorkspaces(
                                workspaces = response.blockingWorkspaces
                            )
                        )
                        _state.value = currentState.copy(isProcessing = false)
                    }
                },
                onFailure = { error ->
                    _events.emit(
                        DeleteAccountEvent.ShowError(
                            message = error.message ?: "Failed to delete account"
                        )
                    )
                    _state.value = currentState.copy(isProcessing = false)
                }
            )
        }
    }

    fun cancelAccountDeletion() {
        screenModelScope.launch {
            val currentState = _state.value
            if (currentState !is DeleteAccountState.PendingDeletion) return@launch

            _state.value = currentState.copy(isProcessing = true)

            cancelAccountDeletionUseCase().fold(
                onSuccess = { response ->
                    _events.emit(
                        DeleteAccountEvent.ShowSuccess(
                            message = response.message,
                            shouldLogout = false
                        )
                    )
                    loadDeletionStatus() // Reload status
                },
                onFailure = { error ->
                    _events.emit(
                        DeleteAccountEvent.ShowError(
                            message = error.message ?: "Failed to cancel deletion"
                        )
                    )
                    _state.value = currentState.copy(isProcessing = false)
                }
            )
        }
    }
}
```

---

## 🎨 UI Components

### Main Delete Account Screen

**File:** `commonMain/kotlin/com/ampairs/presentation/settings/DeleteAccountScreen.kt`

```kotlin
package com.ampairs.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ampairs.presentation.settings.components.BlockingWorkspacesDialog
import com.ampairs.presentation.settings.components.DeleteConfirmationDialog
import kotlinx.coroutines.flow.collectLatest

class DeleteAccountScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = getScreenModel<DeleteAccountViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        var showDeleteDialog by remember { mutableStateOf(false) }
        var showRestoreDialog by remember { mutableStateOf(false) }
        var showBlockingDialog by remember { mutableStateOf(false) }
        var blockingWorkspaces by remember { mutableStateOf(emptyList<WorkspaceOwnershipInfo>()) }

        // Handle events
        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is DeleteAccountEvent.ShowSuccess -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Long
                        )
                        if (event.shouldLogout) {
                            // Navigate to login and logout
                            // authService.logout()
                            navigator.popUntilRoot()
                        }
                    }
                    is DeleteAccountEvent.ShowError -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Long
                        )
                    }
                    is DeleteAccountEvent.ShowBlockingWorkspaces -> {
                        blockingWorkspaces = event.workspaces
                        showBlockingDialog = true
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Delete Account") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val currentState = state) {
                    is DeleteAccountState.Loading -> {
                        LoadingContent()
                    }
                    is DeleteAccountState.Active -> {
                        ActiveAccountContent(
                            reason = currentState.reason,
                            isProcessing = currentState.isProcessing,
                            onReasonChange = viewModel::updateReason,
                            onDeleteClick = { showDeleteDialog = true }
                        )
                    }
                    is DeleteAccountState.PendingDeletion -> {
                        PendingDeletionContent(
                            status = currentState.status,
                            isProcessing = currentState.isProcessing,
                            onRestoreClick = { showRestoreDialog = true }
                        )
                    }
                    is DeleteAccountState.Error -> {
                        ErrorContent(
                            message = currentState.message,
                            onRetry = viewModel::loadDeletionStatus
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showDeleteDialog && state is DeleteAccountState.Active) {
            DeleteConfirmationDialog(
                isCancel = false,
                onConfirm = { reason ->
                    viewModel.requestAccountDeletion(reason)
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        if (showRestoreDialog) {
            DeleteConfirmationDialog(
                isCancel = true,
                onConfirm = {
                    viewModel.cancelAccountDeletion()
                    showRestoreDialog = false
                },
                onDismiss = { showRestoreDialog = false }
            )
        }

        if (showBlockingDialog) {
            BlockingWorkspacesDialog(
                workspaces = blockingWorkspaces,
                onDismiss = { showBlockingDialog = false },
                onManageWorkspaces = {
                    showBlockingDialog = false
                    // Navigate to workspaces
                }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ActiveAccountContent(
    reason: String,
    isProcessing: Boolean,
    onReasonChange: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Permanent Action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Text(
                    "Deleting your account will permanently remove all your personal data after a 30-day grace period. This action cannot be undone after the grace period expires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // What will be deleted
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "What will be deleted?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                DataItemRow("Your profile information")
                DataItemRow("All authentication tokens and sessions")
                DataItemRow("Your workspace memberships")
                DataItemRow("Personal preferences and settings")
            }
        }

        // Reason input
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = { Text("Reason for leaving (optional)") },
            placeholder = { Text("Help us improve by sharing why you're leaving...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            enabled = !isProcessing
        )

        // Delete button
        Button(
            onClick = onDeleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete My Account")
            }
        }
    }
}

@Composable
private fun PendingDeletionContent(
    status: AccountDeletionStatusResponse,
    isProcessing: Boolean,
    onRestoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    "Account Deletion Scheduled",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Countdown
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${status.daysRemaining ?: 0}",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "days remaining",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Divider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Status: ${status.statusMessage}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (status.deletionReason != null) {
                        Text(
                            "Reason: ${status.deletionReason}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Restore info
        if (status.canRestore) {
            Card {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "You can still cancel this deletion request and restore your account within the next ${status.daysRemaining} days.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onRestoreClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore My Account")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DataItemRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

---

### Delete Confirmation Dialog

**File:** `commonMain/kotlin/com/ampairs/presentation/settings/components/DeleteConfirmationDialog.kt`

```kotlin
package com.ampairs.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeleteConfirmationDialog(
    isCancel: Boolean,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val requiredText = if (isCancel) "RESTORE" else "DELETE"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (isCancel) Icons.Default.Restore else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isCancel)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                if (isCancel) "Restore Account" else "Confirm Account Deletion"
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (isCancel) {
                        "This will cancel the deletion request and reactivate your account immediately."
                    } else {
                        "This will schedule your account for permanent deletion in 30 days. During this grace period, you can restore your account. After 30 days, all your data will be permanently deleted and cannot be recovered."
                    }
                )

                if (!isCancel) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it.uppercase() },
                    label = { Text("Type \"$requiredText\" to confirm") },
                    placeholder = { Text(requiredText) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmText.isNotEmpty() && confirmText != requiredText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (isCancel) null else reason.ifBlank { null }) },
                enabled = confirmText == requiredText,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCancel)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isCancel) "Restore Account" else "Delete My Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

### Blocking Workspaces Dialog

**File:** `commonMain/kotlin/com/ampairs/presentation/settings/components/BlockingWorkspacesDialog.kt`

```kotlin
package com.ampairs.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.data.model.WorkspaceOwnershipInfo

@Composable
fun BlockingWorkspacesDialog(
    workspaces: List<WorkspaceOwnershipInfo>,
    onDismiss: () -> Unit,
    onManageWorkspaces: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Cannot Delete Account")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "You are the sole owner of ${workspaces.size} workspace(s).",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Before deleting your account, you must either transfer ownership to another member or delete these workspaces:"
                )

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(workspaces) { workspace ->
                            ListItem(
                                headlineContent = { Text(workspace.workspaceName) },
                                supportingContent = {
                                    Column {
                                        Text("@${workspace.workspaceSlug}")
                                        Text("${workspace.memberCount} members")
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null
                                    )
                                }
                            )
                            if (workspace != workspaces.last()) {
                                Divider()
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                "What you can do:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "• Transfer ownership to another workspace member\n• Delete the workspace entirely",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onManageWorkspaces) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Manage Workspaces")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
```

---

## 🔧 Dependency Injection (Koin)

**File:** `commonMain/kotlin/com/ampairs/di/AccountDeletionModule.kt`

```kotlin
package com.ampairs.di

import com.ampairs.data.api.AccountDeletionApi
import com.ampairs.data.repository.AccountDeletionRepository
import com.ampairs.domain.usecase.*
import com.ampairs.presentation.settings.DeleteAccountViewModel
import org.koin.dsl.module

val accountDeletionModule = module {
    // API
    single {
        AccountDeletionApi(
            httpClient = get(),
            baseUrl = get() // Provide base URL from config
        )
    }

    // Repository
    single {
        AccountDeletionRepository(
            api = get(),
            tokenProvider = { get<AuthManager>().getToken() }
        )
    }

    // Use Cases
    factory { RequestAccountDeletionUseCase(get()) }
    factory { CancelAccountDeletionUseCase(get()) }
    factory { GetAccountDeletionStatusUseCase(get()) }

    // ViewModel
    factory {
        DeleteAccountViewModel(
            requestAccountDeletionUseCase = get(),
            cancelAccountDeletionUseCase = get(),
            getAccountDeletionStatusUseCase = get()
        )
    }
}
```

---

## 🛣️ Navigation

Add the screen to your settings navigation:

```kotlin
// In SettingsScreen.kt
ListItem(
    headlineContent = { Text("Delete Account") },
    leadingContent = {
        Icon(
            Icons.Default.DeleteForever,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
    },
    modifier = Modifier.clickable {
        navigator.push(DeleteAccountScreen())
    }
)
```

---

## 🧪 Testing

### Unit Test Example

```kotlin
class DeleteAccountViewModelTest {

    private lateinit var viewModel: DeleteAccountViewModel
    private lateinit var mockRequestUseCase: RequestAccountDeletionUseCase
    private lateinit var mockCancelUseCase: CancelAccountDeletionUseCase
    private lateinit var mockStatusUseCase: GetAccountDeletionStatusUseCase

    @Before
    fun setup() {
        // Setup mocks
        mockRequestUseCase = mockk()
        mockCancelUseCase = mockk()
        mockStatusUseCase = mockk()

        viewModel = DeleteAccountViewModel(
            requestAccountDeletionUseCase = mockRequestUseCase,
            cancelAccountDeletionUseCase = mockCancelUseCase,
            getAccountDeletionStatusUseCase = mockStatusUseCase
        )
    }

    @Test
    fun `should load deletion status on init`() = runTest {
        // Given
        val mockStatus = AccountDeletionStatusResponse(
            isDeleted = false,
            canRestore = false,
            statusMessage = "Active"
        )
        coEvery { mockStatusUseCase() } returns Result.success(mockStatus)

        // When
        viewModel.loadDeletionStatus()

        // Then
        assert(viewModel.state.value is DeleteAccountState.Active)
    }

    @Test
    fun `should handle deletion request success`() = runTest {
        // Test implementation
    }
}
```

---

## 📋 Deployment Checklist

- [ ] Add Ktor HTTP client dependency
- [ ] Add Voyager navigation dependency
- [ ] Implement all data models
- [ ] Implement API client
- [ ] Implement repository
- [ ] Implement use cases
- [ ] Implement ViewModel
- [ ] Create all UI components
- [ ] Configure Koin modules
- [ ] Add navigation routes
- [ ] Test API integration
- [ ] Test all user flows
- [ ] Verify error handling
- [ ] Test on Android
- [ ] Test on iOS
- [ ] Test on Desktop (if applicable)

---

**Implementation Status:** ✅ Ready to implement
**Estimated Time:** 6-8 hours
**Difficulty:** Medium-High
**Platforms Supported:** Android, iOS, Desktop
