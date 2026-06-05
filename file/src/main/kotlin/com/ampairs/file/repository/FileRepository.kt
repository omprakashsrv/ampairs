package com.ampairs.file.repository

import com.ampairs.file.domain.model.File
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface FileRepository : CrudRepository<File, Int> {
    fun findByUid(uid: String): Optional<File>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(f.updatedAt) FROM file f")
    fun findMaxUpdatedAt(): Instant?
}
