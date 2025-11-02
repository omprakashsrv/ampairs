package com.ampairs.business.service

import com.ampairs.business.exception.BusinessAlreadyExistsException
import com.ampairs.business.exception.BusinessNotFoundException
import com.ampairs.business.exception.InvalidBusinessDataException
import com.ampairs.business.model.Business
import com.ampairs.business.model.dto.BusinessCreateRequest
import com.ampairs.business.model.dto.BusinessUpdateRequest
import com.ampairs.business.model.dto.applyUpdate
import com.ampairs.business.model.dto.applyProfileUpdate
import com.ampairs.business.model.dto.applyOperationsUpdate
import com.ampairs.business.model.dto.applyTaxConfigUpdate
import com.ampairs.business.model.dto.toBusiness
import com.ampairs.business.repository.BusinessRepository
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for Business profile management.
 *
 * **Separation of Concerns**:
 * - Business: Profile, operations, configuration (THIS SERVICE)
 * - Workspace: Tenant management, subscriptions, members (Workspace module)
 *
 * **Business Rules**:
 * - One Business per Workspace (enforced by unique constraint on ownerId)
 * - Business hours validation (closing > opening)
 * - Multi-tenant aware (uses TenantContextHolder)
 */
@Service
class BusinessService @Autowired constructor(
    private val businessRepository: BusinessRepository
) {

    /**
     * Get current workspace ID from tenant context.
     */
    private fun getWorkspaceId(): String {
        return TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No tenant context available")
    }

    /**
     * Get current user ID from security context.
     */
    private fun getCurrentUserId(): String? {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) }
    }

    /**
     * Get business profile for current workspace.
     *
     * @return Business profile
     * @throws BusinessNotFoundException if no business found for workspace
     */
    fun getBusinessProfile(): Business {
        val workspaceId = getWorkspaceId()
        return businessRepository.findByOwnerId(workspaceId)
            ?: throw BusinessNotFoundException(workspaceId)
    }

    /**
     * Get business profile by UID.
     *
     * @param uid The business UID
     * @return Business profile
     * @throws BusinessNotFoundException if business not found
     */
    fun getBusinessByUid(uid: String): Business {
        return businessRepository.findByUid(uid)
            ?: throw BusinessNotFoundException("Business not found with uid: $uid")
    }

    /**
     * Create new business profile for current workspace.
     *
     * @param request The business creation request
     * @return Created business profile
     * @throws BusinessAlreadyExistsException if business already exists for workspace
     * @throws InvalidBusinessDataException if business data is invalid
     */
    @Transactional
    fun createBusinessProfile(request: BusinessCreateRequest): Business {
        val workspaceId = getWorkspaceId()
        val userId = getCurrentUserId()

        // Check if business already exists for this workspace
        if (businessRepository.existsByOwnerId(workspaceId)) {
            throw BusinessAlreadyExistsException(workspaceId)
        }

        // Convert DTO to entity
        val business = request.toBusiness(workspaceId, userId)

        // Validate business hours if provided
        try {
            business.validateBusinessHours()
        } catch (e: IllegalStateException) {
            throw InvalidBusinessDataException(e.message ?: "Invalid business hours")
        }

        // Save and return
        return businessRepository.save(business)
    }

    /**
     * Update business profile for current workspace.
     *
     * @param request The business update request
     * @return Updated business profile
     * @throws BusinessNotFoundException if business not found
     * @throws InvalidBusinessDataException if business data is invalid
     */
    @Transactional
    fun updateBusinessProfile(request: BusinessUpdateRequest): Business {
        val workspaceId = getWorkspaceId()
        val userId = getCurrentUserId()

        // Find existing business
        val business = businessRepository.findByOwnerId(workspaceId)
            ?: throw BusinessNotFoundException(workspaceId)

        // Apply updates
        business.applyUpdate(request, userId)

        // Validate business hours if they were updated
        if (request.openingHours != null || request.closingHours != null) {
            try {
                business.validateBusinessHours()
            } catch (e: IllegalStateException) {
                throw InvalidBusinessDataException(e.message ?: "Invalid business hours")
            }
        }

        // Save and return
        return businessRepository.save(business)
    }

    /**
     * Check if business profile exists for current workspace.
     *
     * @return true if exists, false otherwise
     */
    fun businessProfileExists(): Boolean {
        val workspaceId = getWorkspaceId()
        return businessRepository.existsByOwnerId(workspaceId)
    }

    /**
     * Check if business operates on a specific day.
     *
     * @param dayName The day name (e.g., "Monday")
     * @return true if business operates on that day
     * @throws BusinessNotFoundException if business not found
     */
    fun operatesOnDay(dayName: String): Boolean {
        val business = getBusinessProfile()
        return business.operatesOn(dayName)
    }

    /**
     * Get full address for current business.
     *
     * @return Formatted address string
     * @throws BusinessNotFoundException if business not found
     */
    fun getFullAddress(): String {
        val business = getBusinessProfile()
        return business.getFullAddress()
    }

    // ==================== Specific Section Endpoints ====================

    /**
     * Get business overview for dashboard.
     *
     * @return Business with overview data
     * @throws BusinessNotFoundException if business not found
     */
    fun getBusinessOverview(): Business {
        return getBusinessProfile()
    }

    /**
     * Get detailed business profile information.
     *
     * @return Business with profile details
     * @throws BusinessNotFoundException if business not found
     */
    fun getBusinessProfileDetails(): Business {
        return getBusinessProfile()
    }

    /**
     * Update business profile information only.
     *
     * @param request The profile update request
     * @return Updated business
     * @throws BusinessNotFoundException if business not found
     * @throws InvalidBusinessDataException if business data is invalid
     */
    @Transactional
    fun updateBusinessProfileDetails(request: com.ampairs.business.model.dto.BusinessProfileUpdateRequest): Business {
        val workspaceId = getWorkspaceId()
        val userId = getCurrentUserId()

        val business = businessRepository.findByOwnerId(workspaceId)
            ?: throw BusinessNotFoundException(workspaceId)

        business.applyProfileUpdate(request, userId)
        return businessRepository.save(business)
    }

    /**
     * Get business operational settings.
     *
     * @return Business with operations data
     * @throws BusinessNotFoundException if business not found
     */
    fun getBusinessOperations(): Business {
        return getBusinessProfile()
    }

    /**
     * Update business operational settings only.
     *
     * @param request The operations update request
     * @return Updated business
     * @throws BusinessNotFoundException if business not found
     * @throws InvalidBusinessDataException if business data is invalid
     */
    @Transactional
    fun updateBusinessOperations(request: com.ampairs.business.model.dto.BusinessOperationsUpdateRequest): Business {
        val workspaceId = getWorkspaceId()
        val userId = getCurrentUserId()

        val business = businessRepository.findByOwnerId(workspaceId)
            ?: throw BusinessNotFoundException(workspaceId)

        business.applyOperationsUpdate(request, userId)

        // Validate business hours
        try {
            business.validateBusinessHours()
        } catch (e: IllegalStateException) {
            throw InvalidBusinessDataException(e.message ?: "Invalid business hours")
        }

        return businessRepository.save(business)
    }

    /**
     * Get tax configuration settings.
     *
     * @return Business with tax configuration
     * @throws BusinessNotFoundException if business not found
     */
    fun getTaxConfiguration(): Business {
        return getBusinessProfile()
    }

    /**
     * Update tax configuration settings only.
     *
     * @param request The tax configuration update request
     * @return Updated business
     * @throws BusinessNotFoundException if business not found
     */
    @Transactional
    fun updateTaxConfiguration(request: com.ampairs.business.model.dto.TaxConfigurationUpdateRequest): Business {
        val workspaceId = getWorkspaceId()
        val userId = getCurrentUserId()

        val business = businessRepository.findByOwnerId(workspaceId)
            ?: throw BusinessNotFoundException(workspaceId)

        business.applyTaxConfigUpdate(request, userId)
        return businessRepository.save(business)
    }
}
