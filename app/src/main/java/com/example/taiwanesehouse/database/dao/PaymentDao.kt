// Payment DAO
package com.example.taiwanesehouse.database.dao

import androidx.room.*
import com.example.taiwanesehouse.database.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPaymentHistory(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE userId = :userId AND status = :status ORDER BY timestamp DESC")
    fun getPaymentsByStatus(userId: String, status: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Query("DELETE FROM payments WHERE userId = :userId")
    suspend fun deleteAllPayments(userId: String)
}