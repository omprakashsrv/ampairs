package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.enums.StorefrontAccessIdentifierType
import com.ampairs.ecom.domain.model.StorefrontAccessEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface StorefrontAccessEntryRepository : JpaRepository<StorefrontAccessEntry, Long> {

    fun findAllByStorefrontId(storefrontId: String): List<StorefrontAccessEntry>

    fun findAllByStorefrontId(storefrontId: String, pageable: Pageable): Page<StorefrontAccessEntry>

    fun existsByStorefrontIdAndIdentifierTypeAndIdentifierValue(
        storefrontId: String,
        identifierType: StorefrontAccessIdentifierType,
        identifierValue: String,
    ): Boolean

    fun findByUid(uid: String): StorefrontAccessEntry?

    @Transactional
    fun deleteByUid(uid: String)
}
