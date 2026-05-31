package com.ampairs.ecom.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.ecom.domain.dto.StorefrontAccessBulkImportResult
import com.ampairs.ecom.domain.dto.StorefrontAccessEntryRequest
import com.ampairs.ecom.domain.enums.StorefrontAccessIdentifierType
import com.ampairs.ecom.domain.model.StorefrontAccessEntry
import com.ampairs.ecom.repository.StorefrontAccessEntryRepository
import com.ampairs.user.model.User
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StorefrontAccessService(
    private val storefrontAccessEntryRepository: StorefrontAccessEntryRepository,
) {
    private val logger = LoggerFactory.getLogger(StorefrontAccessService::class.java)

    private val accessCache: Cache<String, List<StorefrontAccessEntry>> = Caffeine.newBuilder()
        .maximumSize(100)
        .build()

    fun checkAccess(storefrontId: String, authentication: Authentication, request: HttpServletRequest): Boolean {
        val entries = accessCache.get(storefrontId) {
            storefrontAccessEntryRepository.findAllByStorefrontId(storefrontId)
        }

        if (entries.isEmpty()) return false

        val principal = authentication.principal as? User
        val userId = principal?.uid
        val phone = principal?.phone?.takeIf { it.isNotBlank() }
        val email = principal?.email?.takeIf { !it.isNullOrBlank() }
        val externalId = extractExternalId(request)

        return entries.any { entry ->
            when (entry.identifierType) {
                StorefrontAccessIdentifierType.USER_ID -> userId != null && entry.identifierValue == userId
                StorefrontAccessIdentifierType.PHONE -> phone != null && entry.identifierValue == phone
                StorefrontAccessIdentifierType.EMAIL -> email != null && entry.identifierValue == email
                StorefrontAccessIdentifierType.EXTERNAL_ID -> externalId != null && entry.identifierValue == externalId
            }
        }
    }

    @Transactional
    fun addAccessEntry(storefrontId: String, workspaceId: String, request: StorefrontAccessEntryRequest): StorefrontAccessEntry {
        val entry = StorefrontAccessEntry().apply {
            ownerId = workspaceId
            this.storefrontId = storefrontId
            identifierType = request.identifierType
            identifierValue = request.identifierValue
        }
        val saved = storefrontAccessEntryRepository.save(entry)
        accessCache.invalidate(storefrontId)
        return saved
    }

    @Transactional
    fun removeAccessEntry(entryUid: String, storefrontId: String) {
        val entry = storefrontAccessEntryRepository.findByUid(entryUid)
            ?: throw NotFoundException("Access entry not found: $entryUid")
        storefrontAccessEntryRepository.delete(entry)
        accessCache.invalidate(storefrontId)
    }

    @Transactional(readOnly = true)
    fun listAccessEntries(storefrontId: String, pageable: Pageable): Page<StorefrontAccessEntry> =
        storefrontAccessEntryRepository.findAllByStorefrontId(storefrontId, pageable)

    @Transactional
    fun bulkImport(storefrontId: String, workspaceId: String, csvContent: String): StorefrontAccessBulkImportResult {
        var imported = 0
        var skipped = 0
        val failed = mutableListOf<String>()

        val lines = csvContent.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        for (line in lines) {
            val parts = line.split(",", limit = 2).map { it.trim() }
            if (parts.size != 2) {
                failed.add("Invalid format: $line")
                continue
            }
            val (typePart, value) = parts
            val identifierType = try {
                StorefrontAccessIdentifierType.valueOf(typePart.uppercase())
            } catch (e: IllegalArgumentException) {
                failed.add("Unknown type '$typePart': $line")
                continue
            }
            if (value.isBlank()) {
                failed.add("Empty value: $line")
                continue
            }
            if (storefrontAccessEntryRepository.existsByStorefrontIdAndIdentifierTypeAndIdentifierValue(storefrontId, identifierType, value)) {
                skipped++
                continue
            }
            val entry = StorefrontAccessEntry().apply {
                ownerId = workspaceId
                this.storefrontId = storefrontId
                this.identifierType = identifierType
                this.identifierValue = value
            }
            storefrontAccessEntryRepository.save(entry)
            imported++
        }

        if (imported > 0) {
            accessCache.invalidate(storefrontId)
        }

        logger.info("Bulk import complete: storefrontId={}, imported={}, skipped={}, failed={}", storefrontId, imported, skipped, failed.size)
        return StorefrontAccessBulkImportResult(imported = imported, skipped = skipped, failed = failed)
    }

    fun invalidateCache(storefrontId: String) {
        accessCache.invalidate(storefrontId)
    }

    private fun extractExternalId(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization") ?: return null
        if (!authHeader.startsWith("Bearer ")) return null
        return try {
            val token = authHeader.substring(7)
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(java.util.Base64.getUrlDecoder().decode(padBase64(parts[1])))
            extractStringClaimFromJson(payload, "external_id")
        } catch (e: Exception) {
            null
        }
    }

    private fun padBase64(base64: String): String {
        val remainder = base64.length % 4
        return if (remainder != 0) base64 + "=".repeat(4 - remainder) else base64
    }

    private fun extractStringClaimFromJson(json: String, claimName: String): String? {
        val pattern = """"$claimName"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }
}
