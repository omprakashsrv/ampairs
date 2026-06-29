package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.model.ConsentScope
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.domain.model.TradeNetwork
import com.ampairs.trade.exception.LinkStateException
import com.ampairs.trade.exception.TradeException
import com.ampairs.trade.repository.TradeLinkRepository
import com.ampairs.trade.repository.TradeNetworkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The brand↔distributor link state machine: INVITED → ACCEPTED → REVOKED, INVITED → DECLINED.
 * No data flows until ACCEPTED; revoke is terminal. At most one non-revoked link per (brand, distributor).
 */
@Service
@Transactional
class TradeLinkService(
    private val linkRepository: TradeLinkRepository,
    private val networkRepository: TradeNetworkRepository,
) {

    fun invite(brandWorkspaceId: String, distributorWorkspaceId: String, scope: ConsentScope?): TradeLink {
        if (brandWorkspaceId.isBlank() || distributorWorkspaceId.isBlank()) {
            throw TradeException("brand and distributor workspace ids are required")
        }
        if (brandWorkspaceId == distributorWorkspaceId) {
            throw TradeException("a workspace cannot link to itself")
        }
        if (linkRepository.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(
                brandWorkspaceId, distributorWorkspaceId, LinkStatus.REVOKED,
            )
        ) {
            throw LinkStateException("a non-revoked link already exists for this brand/distributor pair")
        }
        ensureNetwork(brandWorkspaceId)
        val link = TradeLink().apply {
            this.brandWorkspaceId = brandWorkspaceId
            this.distributorWorkspaceId = distributorWorkspaceId
            this.status = LinkStatus.INVITED
            this.consentScope = scope ?: ConsentScope()
        }
        return linkRepository.save(link)
    }

    fun accept(uid: String, scope: ConsentScope?): TradeLink {
        val link = require(uid)
        if (link.status != LinkStatus.INVITED) {
            throw LinkStateException("only an INVITED link can be accepted (was ${link.status})")
        }
        link.status = LinkStatus.ACCEPTED
        scope?.let { link.consentScope = it } // the distributor may tighten the scope on accept
        return linkRepository.save(link)
    }

    fun decline(uid: String): TradeLink {
        val link = require(uid)
        if (link.status != LinkStatus.INVITED) {
            throw LinkStateException("only an INVITED link can be declined (was ${link.status})")
        }
        link.status = LinkStatus.DECLINED
        return linkRepository.save(link)
    }

    fun revoke(uid: String): TradeLink {
        val link = require(uid)
        if (link.status != LinkStatus.ACCEPTED) {
            throw LinkStateException("only an ACCEPTED link can be revoked (was ${link.status})")
        }
        link.status = LinkStatus.REVOKED
        return linkRepository.save(link)
    }

    @Transactional(readOnly = true)
    fun findActiveLink(brandWorkspaceId: String, distributorWorkspaceId: String): TradeLink? =
        linkRepository.findByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatus(
            brandWorkspaceId, distributorWorkspaceId, LinkStatus.ACCEPTED,
        )

    fun require(uid: String): TradeLink =
        linkRepository.findByUid(uid) ?: throw TradeException("trade link $uid not found")

    private fun ensureNetwork(brandWorkspaceId: String) {
        if (networkRepository.findByBrandWorkspaceId(brandWorkspaceId) == null) {
            networkRepository.save(TradeNetwork().apply { this.brandWorkspaceId = brandWorkspaceId })
        }
    }
}
