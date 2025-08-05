package com.ampairs.order.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dao.OrderItemDao
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class OrderRoomDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
}