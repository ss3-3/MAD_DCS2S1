package com.example.taiwanesehouse.payment

import android.content.Context
import android.util.Log
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.database.entities.PaymentEntity
import com.example.taiwanesehouse.repository.PaymentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebasePaymentManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val paymentsCollection = firestore.collection("payments")
    
    // Use PaymentRepository for proper Firebase + Room sync
    private val paymentRepository: PaymentRepository by lazy {
        val database = AppDatabase.getDatabase(context)
        PaymentRepository(database.paymentDao())
    }

    // State flows
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    companion object {
        private const val TAG = "PaymentManager"
    }

    // Create payment record
    suspend fun createPayment(
        orderId: String,
        amount: Double,
        paymentMethod: String
    ): Result<PaymentEntity> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(
                Exception("User not authenticated")
            )

            val paymentId = "PAYMENT_${currentUser.uid}_${System.currentTimeMillis()}"

            val payment = PaymentEntity(
                paymentId = paymentId,
                orderId = orderId,
                userId = currentUser.uid,
                amount = amount,
                paymentMethod = paymentMethod,
                paymentStatus = "pending",
                currency = "MYR",
                createdAt = Date(),
                updatedAt = Date()
            )

            // Use PaymentRepository to save to both Firebase and Room
            val result = paymentRepository.createPayment(payment)
            
            if (result.isSuccess) {
                Result.success(payment)
            } else {
                result.map { payment }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment: ${e.message}")
            Result.failure(e)
        }
    }

    // Process payment based on method
    suspend fun processPayment(
        payment: PaymentEntity,
        paymentDetails: Map<String, String> = emptyMap()
    ): Result<PaymentResult> {
        return try {
            _isProcessing.value = true

            val result = when (payment.paymentMethod) {
                "card" -> processCardPayment(payment, paymentDetails)
                "ewallet" -> processEWalletPayment(payment, paymentDetails)
                "counter" -> processCounterPayment(payment)
                else -> throw IllegalArgumentException("Unsupported payment method: ${payment.paymentMethod}")
            }

            _paymentResult.value = result
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing payment: ${e.message}")
            val failureResult = PaymentResult(
                paymentId = payment.paymentId,
                success = false,
                message = e.message ?: "Payment processing failed",
                status = "failed"
            )
            _paymentResult.value = failureResult
            Result.failure(e)
        } finally {
            _isProcessing.value = false
        }
    }

    // Card payment processing (integrate with your preferred payment gateway)
    private suspend fun processCardPayment(
        payment: PaymentEntity,
        cardDetails: Map<String, String>
    ): PaymentResult {
        // This is where you'd integrate with Stripe, Razorpay, etc.
        // For now, we'll simulate the process

        return try {
            // Update payment status to processing
            updatePaymentStatus(payment.paymentId, "processing")

            // Simulate payment gateway processing
            // In real implementation, you'd call the payment gateway API here
            kotlinx.coroutines.delay(2000) // Simulate network delay

            // Simulate success (you'd handle actual gateway response here)
            val transactionId = "TXN_${System.currentTimeMillis()}"

            // Update payment as completed
            completePayment(payment.paymentId, transactionId)

            PaymentResult(
                paymentId = payment.paymentId,
                success = true,
                message = "Payment completed successfully",
                status = "completed",
                transactionId = transactionId
            )

        } catch (e: Exception) {
            // Handle payment failure
            failPayment(payment.paymentId, e.message ?: "Card payment failed")
            throw e
        }
    }

    // E-wallet payment processing
    private suspend fun processEWalletPayment(
        payment: PaymentEntity,
        ewalletDetails: Map<String, String>
    ): PaymentResult {
        return try {
            updatePaymentStatus(payment.paymentId, "processing")

            // Simulate e-wallet processing (integrate with TNG eWallet, GrabPay, etc.)
            kotlinx.coroutines.delay(1500)

            val transactionId = "EW_${System.currentTimeMillis()}"
            completePayment(payment.paymentId, transactionId)

            PaymentResult(
                paymentId = payment.paymentId,
                success = true,
                message = "E-wallet payment completed successfully",
                status = "completed",
                transactionId = transactionId
            )

        } catch (e: Exception) {
            failPayment(payment.paymentId, e.message ?: "E-wallet payment failed")
            throw e
        }
    }

    // Counter payment (no actual processing needed, just mark as pending for counter)
    private suspend fun processCounterPayment(payment: PaymentEntity): PaymentResult {
        return try {
            // For counter payment, we just mark it as pending for staff to handle
            updatePaymentStatus(payment.paymentId, "pending")

            PaymentResult(
                paymentId = payment.paymentId,
                success = true,
                message = "Please pay at the counter. Order ID: ${payment.orderId}",
                status = "pending",
                requiresCounterPayment = true
            )

        } catch (e: Exception) {
            failPayment(payment.paymentId, e.message ?: "Counter payment setup failed")
            throw e
        }
    }

    // Update payment status (Firebase + Room)
    private suspend fun updatePaymentStatus(paymentId: String, status: String) {
        try {
            paymentRepository.updatePaymentStatus(paymentId, status)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
        }
    }

    // Complete payment (Firebase + Room)
    private suspend fun completePayment(paymentId: String, transactionId: String) {
        try {
            paymentRepository.completePayment(paymentId, transactionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing payment: ${e.message}")
        }
    }

    // Fail payment (Firebase + Room)
    private suspend fun failPayment(paymentId: String, reason: String) {
        try {
            paymentRepository.failPayment(paymentId, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Error failing payment: ${e.message}")
        }
    }

    // Get payment by ID
    suspend fun getPaymentById(paymentId: String): Result<PaymentEntity?> {
        return try {
            val document = paymentsCollection.document(paymentId).get().await()

            if (!document.exists()) {
                return Result.success(null)
            }

            val data = document.data ?: return Result.success(null)

            val payment = PaymentEntity(
                paymentId = data["paymentId"] as? String ?: paymentId,
                orderId = data["orderId"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                paymentMethod = data["paymentMethod"] as? String ?: "",
                paymentStatus = data["paymentStatus"] as? String ?: "pending",
                transactionId = data["transactionId"] as? String,
                paymentGateway = data["paymentGateway"] as? String,
                currency = data["currency"] as? String ?: "MYR",
                paymentDate = (data["paymentDate"] as? com.google.firebase.Timestamp)?.toDate(),
                failureReason = data["failureReason"] as? String,
                createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
            )

            Result.success(payment)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting payment by ID: ${e.message}")
            Result.failure(e)
        }
    }

    // Clear payment result
    fun clearPaymentResult() {
        _paymentResult.value = null
    }
}

// Payment result data class
data class PaymentResult(
    val paymentId: String,
    val success: Boolean,
    val message: String,
    val status: String,
    val transactionId: String? = null,
    val requiresCounterPayment: Boolean = false
)