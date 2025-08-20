package com.ampairs.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentWorkspaceName: String?,
    userFullName: String,
    isUserLoading: Boolean = false,
    isWorkspaceLoading: Boolean = false,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier,
    isWorkspaceSelection: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Workspace selector
            WorkspaceSelector(
                workspaceName = currentWorkspaceName,
                isLoading = isWorkspaceLoading,
                onWorkspaceClick = onWorkspaceClick,
                modifier = Modifier.weight(1f, fill = false)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Right side - User profile menu
            UserProfileMenu(
                userFullName = userFullName,
                isLoading = isUserLoading,
                onEditProfile = onEditProfile,
                onLogout = onLogout,
                onSwitchUser = onSwitchUser
            )
        }
    }
}

@Composable
private fun WorkspaceSelector(
    workspaceName: String?,
    isLoading: Boolean,
    onWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .clickable { onWorkspaceClick() }
            .widthIn(min = 120.dp, max = 200.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = "Workspace",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workspace",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                if (isLoading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = workspaceName ?: "Select Workspace",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (workspaceName != null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
            
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = "Switch workspace",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun UserProfileMenu(
    userFullName: String,
    isLoading: Boolean,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // User Avatar
            UserAvatar(
                userFullName = userFullName,
                isLoading = isLoading,
                size = 36.dp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // User Name
            Column {
                Text(
                    text = if (isLoading) "Loading..." else userFullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Menu Button
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Profile menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                text = "Edit Profile",
                onClick = {
                    expanded = false
                    onEditProfile()
                }
            )
            
            ProfileMenuItem(
                icon = Icons.Default.SwapHoriz,
                text = "Switch User",
                onClick = {
                    expanded = false
                    onSwitchUser()
                }
            )
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
            
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                text = "Logout",
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun UserAvatar(
    userFullName: String,
    isLoading: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.6f),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            val initials = userFullName
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (textColor == MaterialTheme.colorScheme.error) {
                        textColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        onClick = onClick
    )
}