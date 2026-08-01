package com.ampairs.supplier.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.core.sync.EntityChangeType
import com.ampairs.supplier.domain.model.Supplier
import com.ampairs.supplier.repository.SupplierRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
@Transactional(readOnly = true)
class SupplierService(
    val supplierRepository: SupplierRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional
    fun updateSupplier(supplier: Supplier): Supplier {
        return supplierRepository.save(supplier)
    }

    /**
     * Offline-sync bulk upsert (push). Client UID is authoritative — preserved on insert and matched on
     * update. The body MAY contain soft-deleted rows (status = DELETED) so deletions propagate in-band.
     * Broadcasts each change so other devices of this workspace pull it.
     */
    @Transactional
    fun updateSuppliers(suppliers: List<Supplier>): List<Supplier> {
        suppliers.forEach { supplier ->
            if (supplier.uid.isNotEmpty()) {
                val existing = supplierRepository.findByUid(supplier.uid)
                supplier.id = existing?.id ?: 0
                // Prefer the client-sent ref_id; fall back to the existing one so a blank value never wipes it.
                supplier.refId = supplier.refId?.takeIf { it.isNotBlank() } ?: existing?.refId ?: ""
                supplier.createdAt = existing?.createdAt ?: Instant.now()
                supplier.updatedAt = existing?.updatedAt ?: Instant.now()
            } else if (supplier.refId?.isNotEmpty() == true) {
                val existing = supplierRepository.findByRefId(supplier.refId)
                supplier.id = existing?.id ?: 0
                supplier.uid = existing?.uid ?: ""
                supplier.createdAt = existing?.createdAt ?: Instant.now()
                supplier.updatedAt = existing?.updatedAt ?: Instant.now()
            }
            val saved = supplierRepository.save(supplier)
            entityChangePublisher.publish(
                "supplier",
                saved.uid,
                if (saved.status.equals("DELETED", ignoreCase = true)) EntityChangeType.DELETED
                else EntityChangeType.UPDATED,
            )
        }
        return suppliers
    }

    /**
     * Incremental sync feed (pull): suppliers updated at/after [lastSync] (ISO-8601), INCLUDING
     * soft-deleted rows (status = DELETED). Falls back to the full feed when the cursor is absent/invalid.
     */
    fun getSuppliersAfterSync(lastSync: String?, pageable: Pageable): Page<Supplier> {
        return if (lastSync.isNullOrBlank()) {
            supplierRepository.findAll(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                supplierRepository.findSuppliersUpdatedAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                supplierRepository.findAll(pageable)
            }
        }
    }

    @Transactional
    fun createSupplier(supplier: Supplier): Supplier {
        if (!supplier.isValidGstNumber()) {
            throw IllegalArgumentException("Invalid GST number format: ${supplier.gstNumber}")
        }
        supplier.status = "ACTIVE"
        val saved = supplierRepository.save(supplier)
        entityChangePublisher.publish("supplier", saved.uid, EntityChangeType.UPDATED)
        return saved
    }

    fun getSupplierByUid(uid: String): Supplier? = supplierRepository.findByUid(uid)

    /**
     * Soft delete a supplier by setting status to DELETED.
     */
    @Transactional
    fun deleteSupplier(supplierId: String): Boolean {
        val supplier = supplierRepository.findByUid(supplierId) ?: return false
        supplier.status = "DELETED"
        supplierRepository.save(supplier)
        entityChangePublisher.publish("supplier", supplier.uid, EntityChangeType.DELETED)
        return true
    }

    @Transactional
    fun upsertSupplier(supplier: Supplier): Supplier {
        return if (supplier.uid.isNotEmpty()) {
            val existing = supplierRepository.findByUid(supplier.uid)
            if (existing != null) {
                supplier.id = existing.id
                updateSupplier(supplier)
            } else {
                createSupplier(supplier)
            }
        } else {
            createSupplier(supplier)
        }
    }
}
