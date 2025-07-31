package com.ampairs.workspace.controller

import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/workspace/v1")
class WorkspaceController @Autowired constructor(
    private val companyService: WorkspaceService,
) {

    @PostMapping("")
    fun registerWorkspace(@RequestBody @Valid companyRequest: WorkspaceRequest): WorkspaceRequest {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        return companyService.updateWorkspace(companyRequest.toWorkspace(), user).toWorkspaceRequest()
    }

    @GetMapping("")
    fun getCompanies(): List<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        return companyService.getCompanies(user.seqId).toWorkspaceResponse()
    }

}