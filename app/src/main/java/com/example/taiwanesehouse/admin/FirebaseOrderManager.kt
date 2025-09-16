package com.example.taiwanesehouse.admin

import android.util.Log
import com.example.taiwanesehouse.database.entities.OrderEntity
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.dataclass.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseOrderManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val ordersCollection = firestore.collection("orders")
    private val usersCollection = firestore.collection("users")

    // State flows for reactive UI
    private val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    companion object {
        private const val TAG = "FirebaseOrderManager"
    }

    // Create order with coin usage integration
    suspend fun createOrderWithCoinUsage(
        order: OrderEntity,
        coinsUsed: Int,
        coinDiscount: Double
    ): Result<String> {
        return try {
            _isLoading.value = true

            val currentUser = auth.currentUser ?: return Result.failure(
                Exception("User not authenticated")
            )

            // Start a batch operation to ensure atomicity
            val batch = firestore.batch()

            // 1. Create order document
            val orderData = mapOf(
                "orderId" to order.orderId,
                "userId" to order.userId,
                "customerName" to order.customerName,
                "customerEmail" to order.customerEmail,
                "customerPhone" to order.customerPhone,
                "orderItems" to order.orderItems.map { item ->
                    mapOf(
                        "foodName" to item.foodName,
                        "basePrice" to item.basePrice,
                        "quantity" to item.quantity,
                        "addOns" to item.addOns,
                        "removals" to item.removals,
                        "itemTotalPrice" to item.itemTotalPrice,
                        "imageRes" to item.imageRes
                    )
                },
                "subtotalAmount" to order.subtotalAmount,
                "coinDiscount" to order.coinDiscount,
                "coinsUsed" to order.coinsUsed,
                "totalAmount" to order.totalAmount,
                "orderStatus" to order.orderStatus,
                "orderDate" to order.orderDate,
                "notes" to order.notes,
                "paymentStatus" to order.paymentStatus,
                "paymentMethod" to order.paymentMethod,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt
            )

            val orderDocRef = ordersCollection.document(order.orderId)
            batch.set(orderDocRef, orderData)

            // 2. Deduct coins from user account if coins were used
            if (coinsUsed > 0) {
                val userDocRef = usersCollection.document(currentUser.uid)
                val userSnapshot = userDocRef.get().await()

                if (userSnapshot.exists()) {
                    val currentCoins = userSnapshot.getLong("coins") ?: 0
                    val newCoins = (currentCoins - coinsUsed).coerceAtLeast(0)

                    batch.update(userDocRef, mapOf(
                        "coins" to newCoins,
                        "updatedAt" to Date()
                    ))
                }
            }

            // Execute the batch
            batch.commit().await()

            // Refresh orders list
            loadUserOrders()

            Result.success(order.orderId)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating order with coin usage: ${e.message}")
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // Original createOrderFromCart method (keep for compatibility)
    suspend fun createOrderFromCart(
        cartItems: List<CartItem>,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        deliveryAddress: String? = null,
        notes: String? = null,
        paymentMethod: String? = null
    ): Result<String> {
        return try {
            _isLoading.value = true

            val currentUser = auth.currentUser ?: return Result.failure(
                Exception("User not authenticated")
            )

            // Generate unique order ID
            val orderId = "ORDER_${currentUser.uid}_${System.currentTimeMillis()}"

            // Convert cart items to order items
            val orderItems = cartItems.map { cartItem ->
                val addOnCost = calculateAddOnCost(cartItem.foodAddOns)
                val itemTotalPrice = (cartItem.basePrice + addOnCost) * cartItem.foodQuantity

                mapOf(
                    "foodName" to cartItem.foodName,
                    "basePrice" to cartItem.basePrice,
                    "quantity" to cartItem.foodQuantity,
                    "addOns" to cartItem.foodAddOns,
                    "removals" to cartItem.foodRemovals,
                    "itemTotalPrice" to itemTotalPrice,
                    "imageRes" to cartItem.imagesRes
                )
            }

            // Calculate total amount
            val totalAmount = orderItems.sumOf {
                (it["itemTotalPrice"] as Double)
            }

            // Create order data
            val orderData = mapOf(
                "orderId" to orderId,
                "userId" to currentUser.uid,
                "customerName" to customerName,
                "customerEmail" to customerEmail,
                "customerPhone" to customerPhone,
                "orderItems" to orderItems,
                "subtotalAmount" to totalAmount,
                "coinDiscount" to 0.0,
                "coinsUsed" to 0,
                "totalAmount" to totalAmount,
                "orderStatus" to "pending",
                "orderDate" to Date(),
                "estimatedDeliveryTime" to calculateEstimatedDeliveryTime(),
                "deliveryAddress" to deliveryAddress,
                "notes" to notes,
                "paymentStatus" to "pending",
                "paymentMethod" to paymentMethod,
                "createdAt" to Date(),
                "updatedAt" to Date()
            )

            // Save to Firestore
            ordersCollection.document(orderId).set(orderData).await()

            // Refresh orders list
            loadUserOrders()

            Result.success(orderId)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating order: ${e.message}")
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // Load user orders with updated parsing
    suspend fun loadUserOrders(): Result<List<OrderEntity>> {
        return try {
            _isLoading.value = true

            val currentUser = auth.currentUser ?: return Result.failure(
                Exception("User not authenticated")
            )

            val querySnapshot = ordersCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null

                    // Parse order items
                    val orderItemsData = data["orderItems"] as? List<Map<String, Any>> ?: emptyList()
                    val orderItems = orderItemsData.map { itemData ->
                        OrderItemEntity(
                            orderId = document.id,
                            foodName = itemData["foodName"] as? String ?: "",
                            basePrice = (itemData["basePrice"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                            addOns = (itemData["addOns"] as? List<String>) ?: emptyList(),
                            removals = (itemData["removals"] as? List<String>) ?: emptyList(),
                            itemTotalPrice = (itemData["itemTotalPrice"] as? Number)?.toDouble() ?: 0.0,
                            imageRes = (itemData["imageRes"] as? Number)?.toInt() ?: 0
                        )
                    }

                    OrderEntity(
                        orderId = document.id,
                        userId = data["userId"] as? String ?: "",
                        customerName = data["customerName"] as? String ?: "",
                        customerEmail = data["customerEmail"] as? String ?: "",
                        customerPhone = data["customerPhone"] as? String ?: "",
                        orderItems = orderItems,
                        subtotalAmount = (data["subtotalAmount"] as? Number)?.toDouble() ?: 0.0,
                        coinDiscount = (data["coinDiscount"] as? Number)?.toDouble() ?: 0.0,
                        coinsUsed = (data["coinsUsed"] as? Number)?.toInt() ?: 0,
                        totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        orderStatus = data["orderStatus"] as? String ?: "pending",
                        orderDate = (data["orderDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        notes = data["notes"] as? String,
                        paymentStatus = data["paymentStatus"] as? String ?: "pending",
                        paymentMethod = data["paymentMethod"] as? String,
                        createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing order document: ${e.message}")
                    null
                }
            }

            _orders.value = orders
            Result.success(orders)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading user orders: ${e.message}")
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // Rest of the existing methods remain the same...
    // (getOrderById, updateOrderStatus, updatePaymentStatus, etc.)

    private fun calculateAddOnCost(addOns: List<String>): Double {
        return addOns.sumOf { addOn ->
            when (addOn.lowercase()) {
                "egg" -> 1.0
                "vegetable" -> 2.0
                else -> 0.0
            }
        }
    }

    private fun calculateEstimatedDeliveryTime(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 25)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }
}