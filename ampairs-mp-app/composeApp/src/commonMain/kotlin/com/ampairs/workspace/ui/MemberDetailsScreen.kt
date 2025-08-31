package com.ampairs.workspace.ui

import androidx.compose.animation.*
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
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.viewmodel.MemberDetailsViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Comprehensive member details screen with view and edit capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailsScreen(
    workspaceId: String,
    memberId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MemberDetailsViewModel = koinInject { parametersOf(workspaceId, memberId) }
    val state by viewModel.state.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) {
        viewModel.loadMemberDetails()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with navigation and actions
        MemberDetailsHeader(
            isEditing = isEditing,
            canEdit = state.canEdit,
            onNavigateBack = onNavigateBack,
            onToggleEdit = { isEditing = !isEditing },
            onSave = {
                viewModel.saveMemberChanges()
                isEditing = false
            },
            onCancel = {
                viewModel.discardChanges()
                isEditing = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on state
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorState(
                    error = state.error!!,
                    onRetry = { viewModel.loadMemberDetails() }
                )
            }

            state.member != null -> {
                MemberDetailsContent(
                    member = state.member!!,
                    userRole = state.userRole,
                    isEditing = isEditing,
                    onRoleChange = viewModel::updateRole,
                    onStatusChange = viewModel::updateStatus,
                    onPermissionsChange = viewModel::updatePermissions,
                    onRemoveMember = {
                        viewModel.removeMember()
                        onNavigateBack()
                    },
                    canManageMembers = state.canEdit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun MemberDetailsHeader(
    isEditing: Boolean,
    canEdit: Boolean,
    onNavigateBack: () -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Member Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        AnimatedVisibility(visible = canEdit) {
            Row {
                if (isEditing) {
                    // Save and Cancel buttons when editing
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                } else {
                    // Edit button when viewing
                    Button(onClick = onToggleEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberDetailsContent(
    member: WorkspaceMember,
    userRole: com.ampairs.workspace.api.model.UserRoleResponse?,
    isEditing: Boolean,
    onRoleChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPermissionsChange: (List<String>) -> Unit,
    onRemoveMember: () -> Unit,
    canManageMembers: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Member Profile Card
        MemberProfileCard(
            member = member,
            isEditing = isEditing
        )

        // Role and Status Card
        MemberRoleStatusCard(
            member = member,
            isEditing = isEditing,
            canEdit = canManageMembers,
            onRoleChange = onRoleChange,
            onStatusChange = onStatusChange
        )

        // Permissions Card
        MemberPermissionsCard(
            member = member,
            userRole = userRole,
            isEditing = isEditing,
            canEdit = canManageMembers,
            onPermissionsChange = onPermissionsChange
        )

        // Activity Card
        MemberActivityCard(member = member)

        // Danger Zone (only if can manage members)
        if (canManageMembers && member.role != "OWNER") {
            MemberDangerZoneCard(
                member = member,
                onRemoveMember = onRemoveMember
            )
        }
    }
}

@Composable
private fun MemberProfileCard(
    member: WorkspaceMember,
    isEditing: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Member Avatar",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    member.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    member.phone?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRoleStatusCard(
    member: WorkspaceMember,
    isEditing: Boolean,
    canEdit: Boolean,
    onRoleChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Role & Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Role Section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Role",
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (isEditing && canEdit && member.role != "OWNER") {
                        RoleDropdown(
                            currentRole = member.role,
                            onRoleChange = onRoleChange
                        )
                    } else {
                        MemberRoleChip(role = member.role)
                    }
                }

                // Status Section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (isEditing && canEdit) {
                        StatusDropdown(
                            currentStatus = member.status,
                            onStatusChange = onStatusChange
                        )
                    } else {
                        MemberStatusChip(status = member.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberPermissionsCard(
    member: WorkspaceMember,
    userRole: com.ampairs.workspace.api.model.UserRoleResponse?,
    isEditing: Boolean,
    canEdit: Boolean,
    onPermissionsChange: (List<String>) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing && canEdit) {
                PermissionEditor(
                    currentPermissions = member.permissions,
                    availablePermissions = userRole?.permissions ?: emptyMap(),
                    onPermissionsChange = onPermissionsChange
                )
            } else {
                PermissionViewer(permissions = member.permissions)
            }
        }
    }
}

@Composable
private fun MemberActivityCard(
    member: WorkspaceMember,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityItem(
                    label = "Joined",
                    value = member.joinedAt
                )

                member.lastActivity?.let { lastActivity ->
                    ActivityItem(
                        label = "Last Activity",
                        value = lastActivity
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberDangerZoneCard(
    member: WorkspaceMember,
    onRemoveMember: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showRemoveDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.PersonRemove, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Member")
            }
        }
    }

    // Remove member confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Member") },
            text = { 
                Text("Are you sure you want to remove ${member.name} from this workspace? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper Composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    currentRole: String,
    onRoleChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("ADMIN", "MANAGER", "MEMBER", "VIEWER")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentRole,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role) },
                    onClick = {
                        onRoleChange(role)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    currentStatus: String,
    onStatusChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("ACTIVE", "INACTIVE")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentStatus,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MemberRoleChip(role: String) {
    val roleColors = when (role) {
        "OWNER" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "ADMIN" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "MANAGER" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "MEMBER" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "VIEWER" -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { Text(role) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = roleColors.first,
            labelColor = roleColors.second
        )
    )
}

@Composable
private fun MemberStatusChip(status: String) {
    val statusColors = when (status) {
        "ACTIVE" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "INACTIVE" -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        "PENDING" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { Text(status) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = statusColors.first,
            labelColor = statusColors.second
        )
    )
}

@Composable
private fun PermissionEditor(
    currentPermissions: Map<String, Any>,
    availablePermissions: Map<String, Map<String, Boolean>>,
    onPermissionsChange: (List<String>) -> Unit,
) {
    Column {
        Text("Permission editing will be implemented based on backend structure")
        // TODO: Implement based on actual permission structure from backend
    }
}

@Composable
private fun PermissionViewer(
    permissions: Map<String, Any>,
) {
    if (permissions.isEmpty()) {
        Text(
            text = "No specific permissions assigned",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column {
            permissions.forEach { (key, value) ->
                Text("$key: $value")
            }
        }
    }
}

@Composable
private fun ActivityItem(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Failed to load member details",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}