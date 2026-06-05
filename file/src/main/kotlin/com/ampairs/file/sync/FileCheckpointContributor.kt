package com.ampairs.file.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.file.repository.EntityImageRepository
import com.ampairs.file.repository.FileRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes file/image checkpoints for the current workspace. Images are stored centrally in the
 * `entity_image` table keyed by an uppercased `entityType`, so `customer_image` / `product_image`
 * map to that table filtered by type; `file` maps to the `file` table. Queries are `@TenantId`-filtered.
 */
@Component
class FileCheckpointContributor(
    private val fileRepository: FileRepository,
    private val entityImageRepository: EntityImageRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "file" to fileRepository.findMaxUpdatedAt(),
        "customer_image" to entityImageRepository.findMaxUpdatedAtByEntityType("CUSTOMER"),
        "product_image" to entityImageRepository.findMaxUpdatedAtByEntityType("PRODUCT"),
    )
}
