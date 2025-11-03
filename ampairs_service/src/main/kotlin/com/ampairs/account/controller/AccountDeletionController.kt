package com.ampairs.account.controller

import com.ampairs.account.dto.AccountDeletionRequest
import com.ampairs.account.dto.AccountDeletionResponse
import com.ampairs.account.dto.AccountDeletionStatusResponse
import com.ampairs.account.service.AccountDeletionService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Controller for account deletion operations
 * Located in ampairs_service module to coordinate between auth and workspace modules
 */
@RestController
@RequestMapping("/api/v1/account")
class AccountDeletionController @Autowired constructor(
    private val accountDeletionService: AccountDeletionService
) {

    /**
     * Request account deletion
     * Validates workspace ownership and marks account for deletion with 30-day grace period
     */
    @PostMapping("/delete-request")
    fun requestAccountDeletion(
        @RequestBody @Valid request: AccountDeletionRequest
    ): ApiResponse<AccountDeletionResponse> {
        val response = accountDeletionService.requestAccountDeletion(request)
        return ApiResponse.success(response)
    }

    /**
     * Cancel account deletion (restore account within grace period)
     */
    @PostMapping("/delete-cancel")
    fun cancelAccountDeletion(): ApiResponse<AccountDeletionResponse> {
        val response = accountDeletionService.cancelAccountDeletion()
        return ApiResponse.success(response)
    }

    /**
     * Get account deletion status
     */
    @GetMapping("/delete-status")
    fun getAccountDeletionStatus(): ApiResponse<AccountDeletionStatusResponse> {
        val status = accountDeletionService.getAccountDeletionStatus()
        return ApiResponse.success(status)
    }
}
