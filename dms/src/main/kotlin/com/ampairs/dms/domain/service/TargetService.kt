package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.model.SalesTarget
import com.ampairs.dms.repository.SalesTargetRepository
import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.service.CrossTenantReadGuard
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Sales targets by tier/grain. Cross-tenant reads (a distributor's targets) pass the consent gate. */
@Service
@Transactional
class TargetService(
    private val repository: SalesTargetRepository,
    private val guard: CrossTenantReadGuard,
) {

    fun create(target: SalesTarget): SalesTarget = repository.save(target)

    @Transactional(readOnly = true)
    fun readTargets(brandWorkspaceId: String, distributorWorkspaceId: String?): List<SalesTarget> {
        if (distributorWorkspaceId != null) {
            guard.requireActiveLink(brandWorkspaceId, distributorWorkspaceId, DataCategory.TARGETS)
        }
        return repository.findByBrandWorkspaceId(brandWorkspaceId)
    }
}
