package com.ampairs.workspace.repository

import com.ampairs.workspace.model.UserWorkspace
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserWorkspaceRepository : CrudRepository<UserWorkspace, Long> {
    fun findByUid(uid: String): Optional<UserWorkspace>
    fun findAllByUserId(userId: String): List<UserWorkspace>
}