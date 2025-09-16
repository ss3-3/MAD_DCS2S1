package com.example.taiwanesehouse.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "orders")
@TypeConverters(com.example.taiwanesehouse.database.converters.TypeConverters::class)
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val userId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val orderItems: List<OrderItemEntity>,
    val subtotalAmount: Double,
    val coinDiscount: Double = 0.0,
    val coinsUsed: Int = 0,
    val coinsEarned: Int = 0, // ADD THIS FIELD
    val totalAmount: Double,
    val orderStatus: String,
    val orderDate: Date,
    val notes: String? = null,
    val paymentStatus: String = "pending",
    val paymentMethod: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String, // Reference to parent order
    val foodName: String,
    val basePrice: Double,
    val quantity: Int,
    val addOns: List<String> = emptyList(), // e.g., ["Egg", "Vegetable"]
    val removals: List<String> = emptyList(), // e.g., ["Spring Onion"]
    val itemTotalPrice: Double, // basePrice * quantity + add-on costs
    val imageRes: Int = 0
)
