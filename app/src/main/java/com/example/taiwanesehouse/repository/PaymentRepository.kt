// Payment Repository
package com.example.taiwanesehouse.repository

import com.example.taiwanesehouse.database.dao.PaymentDao
import com.example.taiwanesehouse.database.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val paymentDao: PaymentDao) {
    fun getPaymentHistory(userId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentHistory(userId)
    }

    fun getPaymentsByStatus(userId: String, status: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsByStatus(userId, status)
    }

    suspend fun insertPayment(payment: PaymentEntity) {
        paymentDao.insertPayment(payment)
    }

    suspend fun updatePayment(payment: PaymentEntity) {
        paymentDao.updatePayment(payment)
    }

    suspend fun getPaymentById(paymentId: String): PaymentEntity? {
        return paymentDao.getPaymentById(paymentId)
    }
}