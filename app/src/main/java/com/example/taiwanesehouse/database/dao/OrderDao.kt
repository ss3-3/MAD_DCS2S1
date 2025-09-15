package com.example.taiwanesehouse.database.dao

import androidx.room.*
import com.example.taiwanesehouse.database.entities.OrderEntity
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // Order operations
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getOrdersByUserId(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderStatus = :status ORDER BY orderDate DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET orderStatus = :status, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: Long)

    @Query("UPDATE orders SET paymentStatus = :paymentStatus, paymentMethod = :paymentMethod, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updatePaymentStatus(orderId: String, paymentStatus: String, paymentMethod: String?, updatedAt: Long)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: String)

    // Order items operations
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(orderItem: OrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(orderItems: List<OrderItemEntity>)

    @Delete
    suspend fun deleteOrderItem(orderItem: OrderItemEntity)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsByOrderId(orderId: String)

    // Analytics queries
    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId")
    suspend fun getOrderCountByUser(userId: String): Int

    @Query("SELECT SUM(totalAmount) FROM orders WHERE userId = :userId AND paymentStatus = 'paid'")
    suspend fun getTotalSpentByUser(userId: String): Double?

    @Query("SELECT * FROM orders WHERE orderDate BETWEEN :startDate AND :endDate ORDER BY orderDate DESC")
    suspend fun getOrdersByDateRange(startDate: Long, endDate: Long): List<OrderEntity>

    @Query("UPDATE orders SET coinsEarned = :coinsEarned WHERE orderId = :orderId")
    suspend fun updateOrderCoinsEarned(orderId: String, coinsEarned: Int)
}