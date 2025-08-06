package com.ampairs.workspace.service

import com.ampairs.user.model.User
import com.ampairs.workspace.model.UserWorkspace
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.enums.Role
import com.ampairs.workspace.repository.UserWorkspaceRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceService @Autowired constructor(
    val companyRepository: WorkspaceRepository,
    val userWorkspaceRepository: UserWorkspaceRepository,
) {
    fun getCompanies(userId: String): List<Workspace> {
        val userCompanies = getUserCompanies(userId)
        val companies: MutableList<Workspace> = mutableListOf()
        for (userWorkspace in userCompanies) {
            companies.add(userWorkspace.company)
        }
        return companies
    }

    fun getUserCompanies(userId: String): List<UserWorkspace> {
        return userWorkspaceRepository.findAllByUserId(userId)
    }

    @Transactional
    fun updateWorkspace(company: Workspace, user: User): Workspace {
        val newWorkspace = company.uid.isEmpty()
        if (!newWorkspace) {
            val existingWorkspace =
                companyRepository.findByUid(company.uid).orElseThrow {
                    Exception("No company found with given id")
                }
            company.id = existingWorkspace.id
        }
        val updatedWorkspace = companyRepository.save(company)
        if (newWorkspace) {
            val userWorkspace = UserWorkspace()
            userWorkspace.companyId = updatedWorkspace.uid
            userWorkspace.userId = user.uid
            userWorkspace.role = Role.OWNER
            userWorkspaceRepository.save(userWorkspace)
        }
        return updatedWorkspace
    }

}