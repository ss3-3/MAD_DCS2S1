package com.example.taiwanesehouse.repository

import android.util.Log
import com.example.taiwanesehouse.database.dao.PaymentDao
import com.example.taiwanesehouse.database.entities.PaymentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date

class PaymentRepository(
    private val paymentDao: PaymentDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val paymentsCollection = firestore.collection("payments")

    companion object {
        private const val TAG = "PaymentRepository"
    }

    // Get payments by user ID (Room first, then sync from Firebase)
    fun getPaymentsByUserId(userId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsByUserId(userId)
    }

    // Get payment by ID
    suspend fun getPaymentById(paymentId: String): PaymentEntity? {
        return paymentDao.getPaymentById(paymentId)
    }

    // Get payment by order ID
    suspend fun getPaymentByOrderId(orderId: String): PaymentEntity? {
        return paymentDao.getPaymentByOrderId(orderId)
    }

    // Create payment (Firebase + Room)
    suspend fun createPayment(payment: PaymentEntity): Result<String> {
        return try {
            // Save to Firebase first
            val firebaseResult = savePaymentToFirebase(payment)
            
            if (firebaseResult.isSuccess) {
                // Save to Room database
                paymentDao.insertPayment(payment)
                Log.d(TAG, "Payment created successfully: ${payment.paymentId}")
                Result.success(payment.paymentId)
            } else {
                firebaseResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment: ${e.message}")
            Result.failure(e)
        }
    }

    // Update payment status (Firebase + Room)
    suspend fun updatePaymentStatus(paymentId: String, status: String): Result<Boolean> {
        return try {
            val currentTime = Date().time

            // Update in Firebase first
            paymentsCollection.document(paymentId)
                .update(
                    mapOf(
                        "paymentStatus" to status,
                        "updatedAt" to Date()
                    )
                )
                .await()

            // Update in Room database
            paymentDao.updatePaymentStatus(paymentId, status, currentTime)

            Log.d(TAG, "Payment status updated: $paymentId -> $status")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
            Result.failure(e)
        }
    }

    // Complete payment (Firebase + Room)
    suspend fun completePayment(paymentId: String, transactionId: String?): Result<Boolean> {
        return try {
            val currentTime = Date().time
            val paymentDate = Date().time

            // Update in Firebase first
            paymentsCollection.document(paymentId)
                .update(
                    mapOf(
                        "paymentStatus" to "completed",
                        "transactionId" to transactionId,
                        "paymentDate" to Date(),
                        "updatedAt" to Date()
                    )
                )
                .await()

            // Update in Room database
            paymentDao.completePayment(paymentId, "completed", transactionId, paymentDate, currentTime)

            Log.d(TAG, "Payment completed: $paymentId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing payment: ${e.message}")
            Result.failure(e)
        }
    }

    // Fail payment (Firebase + Room)
    suspend fun failPayment(paymentId: String, reason: String?): Result<Boolean> {
        return try {
            val currentTime = Date().time

            // Update in Firebase first
            paymentsCollection.document(paymentId)
                .update(
                    mapOf(
                        "paymentStatus" to "failed",
                        "failureReason" to reason,
                        "updatedAt" to Date()
                    )
                )
                .await()

            // Update in Room database
            paymentDao.failPayment(paymentId, "failed", reason, currentTime)

            Log.d(TAG, "Payment failed: $paymentId - $reason")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error failing payment: ${e.message}")
            Result.failure(e)
        }
    }

    // Sync payments from Firebase to Room
    suspend fun syncPaymentsFromFirebase(userId: String): Result<List<PaymentEntity>> {
        return try {
            val query = paymentsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val documents = query.get().await()
            val payments = mutableListOf<PaymentEntity>()

            for (document in documents) {
                val payment = getPaymentFromFirebase(document.id)
                payment?.let {
                    payments.add(it)
                    // Cache in local database
                    paymentDao.insertPayment(it)
                }
            }

            Log.d(TAG, "Synced ${payments.size} payments from Firebase")
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing payments from Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    // Firebase-specific operations
    private suspend fun savePaymentToFirebase(payment: PaymentEntity): Result<String> {
        return try {
            val paymentData = mapOf(
                "paymentId" to payment.paymentId,
                "orderId" to payment.orderId,
                "userId" to payment.userId,
                "amount" to payment.amount,
                "paymentMethod" to payment.paymentMethod,
                "paymentStatus" to payment.paymentStatus,
                "transactionId" to payment.transactionId,
                "paymentGateway" to payment.paymentGateway,
                "paymentDetails" to payment.paymentDetails,
                "currency" to payment.currency,
                "paymentDate" to payment.paymentDate,
                "failureReason" to payment.failureReason,
                "refundAmount" to payment.refundAmount,
                "refundStatus" to payment.refundStatus,
                "createdAt" to payment.createdAt,
                "updatedAt" to payment.updatedAt
            )

            paymentsCollection.document(payment.paymentId).set(paymentData).await()
            Result.success(payment.paymentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving payment to Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun getPaymentFromFirebase(paymentId: String): PaymentEntity? {
        return try {
            val document = paymentsCollection.document(paymentId).get().await()

            if (document.exists()) {
                val data = document.data ?: return null

                PaymentEntity(
                    paymentId = data["paymentId"] as? String ?: paymentId,
                    orderId = data["orderId"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    paymentMethod = data["paymentMethod"] as? String ?: "",
                    paymentStatus = data["paymentStatus"] as? String ?: "pending",
                    transactionId = data["transactionId"] as? String,
                    paymentGateway = data["paymentGateway"] as? String,
                    paymentDetails = (data["paymentDetails"] as? Map<String, String>) ?: emptyMap(),
                    currency = data["currency"] as? String ?: "MYR",
                    paymentDate = (data["paymentDate"] as? com.google.firebase.Timestamp)?.toDate(),
                    failureReason = data["failureReason"] as? String,
                    refundAmount = (data["refundAmount"] as? Number)?.toDouble(),
                    refundStatus = data["refundStatus"] as? String,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting payment from Firebase: ${e.message}")
            null
        }
    }
}
