package com.ampairs.order.repository

import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface OrderRepository : CrudRepository<Order, Long>, PagingAndSortingRepository<Order, Long> {
    fun findByUid(uid: String): Optional<Order>
    fun findByOrderNumber(orderNumber: String): Optional<Order>
    fun findByCustomerId(customerId: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
    fun findByOrderType(orderType: String): List<Order>
    fun findByPaymentMethod(paymentMethod: String): List<Order>
    @Query("SELECT o FROM customer_order o WHERE o.isWalkIn = :walkIn")
    fun findByIsWalkIn(walkIn: Boolean): List<Order>

    @Query("SELECT MAX(CAST(co.orderNumber AS INTEGER)) FROM customer_order co")
    fun findMaxOrderNumber(): Optional<String>

    @Query("SELECT o FROM customer_order o WHERE o.customerId = :customerId AND o.status IN :statuses")
    fun findByCustomerIdAndStatusIn(customerId: String, statuses: List<OrderStatus>, pageable: Pageable): Page<Order>

    @Query("SELECT o FROM customer_order o WHERE o.status = :status AND o.orderDate BETWEEN :startDate AND :endDate")
    fun findByStatusAndDateRange(status: OrderStatus, startDate: Date, endDate: Date, pageable: Pageable): Page<Order>

    @Query("SELECT o FROM customer_order o WHERE o.orderNumber LIKE %:searchTerm% OR o.customerName LIKE %:searchTerm% OR o.customerPhone LIKE %:searchTerm%")
    fun searchOrders(searchTerm: String, pageable: Pageable): Page<Order>

    @Query("SELECT o FROM customer_order o WHERE o.status IN :statuses ORDER BY o.orderDate DESC")
    fun findByStatusInOrderByOrderDateDesc(statuses: List<OrderStatus>, pageable: Pageable): Page<Order>

    @Query("SELECT o FROM customer_order o WHERE o.totalAmount BETWEEN :minAmount AND :maxAmount AND o.status = :status")
    fun findByTotalAmountRangeAndStatus(minAmount: Double, maxAmount: Double, status: OrderStatus, pageable: Pageable): Page<Order>

    @Query("SELECT o FROM customer_order o WHERE o.deliveryDate IS NOT NULL AND o.deliveryDate BETWEEN :startDate AND :endDate")
    fun findByDeliveryDateRange(startDate: LocalDateTime, endDate: LocalDateTime, pageable: Pageable): Page<Order>

    @Query("SELECT COUNT(o) FROM customer_order o WHERE o.customerId = :customerId")
    fun countByCustomerId(customerId: String): Long

    @Query("SELECT SUM(o.totalAmount) FROM customer_order o WHERE o.customerId = :customerId AND o.status IN :statuses")
    fun sumTotalAmountByCustomerIdAndStatusIn(customerId: String, statuses: List<OrderStatus>): Double?
}