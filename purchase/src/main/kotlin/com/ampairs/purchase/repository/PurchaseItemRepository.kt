package com.ampairs.purchase.repository

import com.ampairs.purchase.domain.model.PurchaseItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PurchaseItemRepository : CrudRepository<PurchaseItem, Long> {
    fun findByUid(uid: String): Optional<PurchaseItem>
}
