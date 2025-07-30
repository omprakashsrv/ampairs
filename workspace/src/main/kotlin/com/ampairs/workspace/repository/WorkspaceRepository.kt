package com.ampairs.workspace.repository

import com.ampairs.workspace.model.Workspace
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceRepository : CrudRepository<Workspace, String>