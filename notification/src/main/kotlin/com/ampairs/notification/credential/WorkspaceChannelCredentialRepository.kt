package com.ampairs.notification.credential

import com.ampairs.notification.provider.NotificationChannel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface WorkspaceChannelCredentialRepository : CrudRepository<WorkspaceChannelCredential, Long> {
    fun findByUid(uid: String?): WorkspaceChannelCredential?

    /** The active credential for a channel in the current workspace (@TenantId-filtered). */
    fun findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(channel: NotificationChannel): WorkspaceChannelCredential?

    fun findByChannelAndProvider(channel: NotificationChannel, provider: String): WorkspaceChannelCredential?

    @Query("SELECT c FROM workspace_channel_credential c WHERE c.active = true")
    fun findAllActive(): List<WorkspaceChannelCredential>
}
