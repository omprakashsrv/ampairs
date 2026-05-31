package com.ampairs.ecom.service

import com.ampairs.ecom.domain.enums.CartStatus
import com.ampairs.ecom.repository.EcomCartRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class CartCleanupTask(
    private val cartRepository: EcomCartRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    fun abandonExpiredCarts() {
        val expired = cartRepository.findByExpiresAtBefore(Instant.now())
            .filter { it.status == CartStatus.ACTIVE }
        if (expired.isEmpty()) return
        expired.forEach { it.status = CartStatus.ABANDONED }
        cartRepository.saveAll(expired)
        log.info("Marked {} expired carts as ABANDONED", expired.size)
    }
}
