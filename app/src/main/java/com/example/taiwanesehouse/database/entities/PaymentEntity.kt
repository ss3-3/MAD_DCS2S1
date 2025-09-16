package com.example.taiwanesehouse.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.Date

@Entity(tableName = "payments")
@androidx.room.TypeConverters(com.example.taiwanesehouse.database.converters.TypeConverters::class)
data class PaymentEntity(
    @PrimaryKey
    val paymentId: String, // Firebase document ID
    val orderId: String, // Reference to order
    val userId: String, // Firebase Auth UID
    val amount: Double,
    val paymentMethod: String, // "card", "ewallet", "counter"
    val paymentStatus: String, // "pending", "processing", "completed", "failed", "cancelled"
    val transactionId: String? = null, // From payment gateway
    val paymentGateway: String? = null, // "stripe", "paypal", "razorpay", etc.
    val paymentDetails: Map<String, String> = emptyMap(), // Additional payment info
    val currency: String = "MYR",
    val paymentDate: Date? = null, // When payment was completed
    val failureReason: String? = null, // If payment failed
    val refundAmount: Double? = null, // If partially refunded
    val refundStatus: String? = null, // "none", "partial", "full"
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)