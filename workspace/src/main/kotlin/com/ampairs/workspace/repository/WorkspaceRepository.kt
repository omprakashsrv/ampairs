package com.ampairs.workspace.repository

import com.ampairs.workspace.model.Workspace
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkspaceRepository : CrudRepository<Workspace, Long> {
    fun findByUid(uid: String): Optional<Workspace>
}