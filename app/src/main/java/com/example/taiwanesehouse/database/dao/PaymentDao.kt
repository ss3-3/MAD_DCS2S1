package com.example.taiwanesehouse.database.dao

import androidx.room.*
import com.example.taiwanesehouse.database.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    suspend fun getPaymentByOrderId(orderId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE paymentId = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPaymentsByUserId(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE paymentStatus = :status ORDER BY createdAt DESC")
    fun getPaymentsByStatus(status: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET paymentStatus = :status, updatedAt = :updatedAt WHERE paymentId = :paymentId")
    suspend fun updatePaymentStatus(paymentId: String, status: String, updatedAt: Long)

    @Query("UPDATE payments SET paymentStatus = :status, transactionId = :transactionId, paymentDate = :paymentDate, updatedAt = :updatedAt WHERE paymentId = :paymentId")
    suspend fun completePayment(paymentId: String, status: String, transactionId: String?, paymentDate: Long, updatedAt: Long)

    @Query("UPDATE payments SET paymentStatus = :status, failureReason = :reason, updatedAt = :updatedAt WHERE paymentId = :paymentId")
    suspend fun failPayment(paymentId: String, status: String, reason: String?, updatedAt: Long)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE paymentId = :paymentId")
    suspend fun deletePaymentById(paymentId: String)

    // Analytics
    @Query("SELECT SUM(amount) FROM payments WHERE userId = :userId AND paymentStatus = 'completed'")
    suspend fun getTotalPaidByUser(userId: String): Double?

    @Query("SELECT COUNT(*) FROM payments WHERE userId = :userId AND paymentStatus = 'completed'")
    suspend fun getCompletedPaymentCount(userId: String): Int
}