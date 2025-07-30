package com.ampairs.workspace.repository

import com.ampairs.workspace.model.UserWorkspace
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserWorkspaceRepository : CrudRepository<UserWorkspace, String> {

    fun findAllByUserId(userId: String): List<UserWorkspace>
}