package com.example.taiwanesehouse.repository

import android.util.Log
import com.example.taiwanesehouse.database.dao.OrderDao
import com.example.taiwanesehouse.database.dao.UserDao
import com.example.taiwanesehouse.database.entities.OrderEntity
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.database.entities.UserEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val userDao: com.example.taiwanesehouse.database.dao.UserDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")

    companion object {
        private const val TAG = "OrderRepository"
    }

    // Local database operations
    fun getOrdersByUserId(userId: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByUserId(userId)
    }

    suspend fun getOrderById(orderId: String): OrderEntity? {
        return try {
            // First try local database
            val localOrder = orderDao.getOrderById(orderId)
            if (localOrder != null) {
                return localOrder
            }

            // If not found locally, try Firebase
            val firebaseOrder = getOrderFromFirebase(orderId)
            firebaseOrder?.let {
                // Cache in local database
                orderDao.insertOrder(it)
            }
            firebaseOrder
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order by ID: ${e.message}")
            null
        }
    }

    fun getAllOrders(): Flow<List<OrderEntity>> {
        return orderDao.getAllOrders()
    }

    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(status)
    }

    // Firebase and local sync operations
    suspend fun createOrder(order: OrderEntity): Result<String> {
        return try {
            // Save to Firebase first
            val firebaseResult = saveOrderToFirebase(order)

            if (firebaseResult.isSuccess) {
                // If Firebase save is successful, save to local database
                orderDao.insertOrder(order)
                Result.success(order.orderId)
            } else {
                firebaseResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Boolean> {
        return try {
            val currentTime = Date().time

            // Update in Firebase first
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "orderStatus" to newStatus,
                        "updatedAt" to Date()
                    )
                )
                .await()

            // Update local database
            orderDao.updateOrderStatus(orderId, newStatus, currentTime)

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updatePaymentStatus(
        orderId: String,
        paymentStatus: String,
        paymentMethod: String? = null
    ): Result<Boolean> {
        return try {
            val currentTime = Date().time
            val updateData = mutableMapOf<String, Any>(
                "paymentStatus" to paymentStatus,
                "updatedAt" to Date()
            )

            paymentMethod?.let { updateData["paymentMethod"] = it }

            // Update in Firebase first
            ordersCollection.document(orderId)
                .update(updateData)
                .await()

            // Update local database
            orderDao.updatePaymentStatus(orderId, paymentStatus, paymentMethod, currentTime)

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Boolean> {
        return try {
            // Delete from Firebase first
            ordersCollection.document(orderId).delete().await()

            // Delete from local database
            orderDao.deleteOrderById(orderId)
            orderDao.deleteOrderItemsByOrderId(orderId)

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order: ${e.message}")
            Result.failure(e)
        }
    }

    // Firebase-specific operations
    private suspend fun saveOrderToFirebase(order: OrderEntity): Result<String> {
        return try {
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
                "totalAmount" to order.totalAmount,
                "orderStatus" to order.orderStatus,
                "orderDate" to order.orderDate,
                "estimatedDeliveryTime" to order.estimatedDeliveryTime,
                "deliveryAddress" to order.deliveryAddress,
                "notes" to order.notes,
                "paymentStatus" to order.paymentStatus,
                "paymentMethod" to order.paymentMethod,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt
            )

            ordersCollection.document(order.orderId).set(orderData).await()
            Result.success(order.orderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order to Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun getOrderFromFirebase(orderId: String): OrderEntity? {
        return try {
            val document = ordersCollection.document(orderId).get().await()

            if (document.exists()) {
                val data = document.data ?: return null

                // Parse order items
                val orderItemsData = data["orderItems"] as? List<Map<String, Any>> ?: emptyList()
                val orderItems = orderItemsData.map { itemData ->
                    OrderItemEntity(
                        orderId = orderId,
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
                    orderId = data["orderId"] as? String ?: orderId,
                    userId = data["userId"] as? String ?: "",
                    customerName = data["customerName"] as? String ?: "",
                    customerEmail = data["customerEmail"] as? String ?: "",
                    customerPhone = data["customerPhone"] as? String ?: "",
                    orderItems = orderItems,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    orderStatus = data["orderStatus"] as? String ?: "pending",
                    orderDate = (data["orderDate"] as? Timestamp)?.toDate()
                        ?: Date(),
                    estimatedDeliveryTime = data["estimatedDeliveryTime"] as? String,
                    deliveryAddress = data["deliveryAddress"] as? String,
                    notes = data["notes"] as? String,
                    paymentStatus = data["paymentStatus"] as? String ?: "pending",
                    paymentMethod = data["paymentMethod"] as? String,
                    createdAt = (data["createdAt"] as? Timestamp)?.toDate()
                        ?: Date(),
                    updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()
                        ?: Date(),
                    subtotalAmount = TODO(),
                    coinDiscount = TODO(),
                    coinsUsed = TODO(),
                    coinsEarned = TODO()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order from Firebase: ${e.message}")
            null
        }
    }

    // Sync operations
    suspend fun syncOrdersFromFirebase(userId: String): Result<List<OrderEntity>> {
        return try {
            val query = ordersCollection
                .whereEqualTo("userId", userId)
                .orderBy("orderDate", Query.Direction.DESCENDING)

            val documents = query.get().await()
            val orders = mutableListOf<OrderEntity>()

            for (document in documents) {
                val order = getOrderFromFirebase(document.id)
                order?.let {
                    orders.add(it)
                    // Cache in local database
                    orderDao.insertOrder(it)
                }
            }

            Result.success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing orders from Firebase: ${e.message}")
            Result.failure(e)
        }
    }

    // Analytics operations
    suspend fun getOrderCountByUser(userId: String): Int {
        return orderDao.getOrderCountByUser(userId)
    }

    suspend fun getTotalSpentByUser(userId: String): Double {
        return orderDao.getTotalSpentByUser(userId) ?: 0.0
    }

    suspend fun createOrderWithCoinUsage(
        order: OrderEntity,
        coinsUsed: Int,
        coinDiscount: Double
    ): Result<String> {
        return try {
            // Validate and deduct coins BEFORE creating order
            if (coinsUsed > 0) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    return Result.failure(Exception("User not authenticated"))
                }

                // Deduct coins first
                val userRepository = UserRepository(userDao)
                val coinDeductionResult =
                    userRepository.deductCoinsFromUser(currentUser.uid, coinsUsed)
                if (coinDeductionResult.isFailure) {
                    return Result.failure(
                        coinDeductionResult.exceptionOrNull()
                            ?: Exception("Failed to deduct coins")
                    )
                }
            }

            // Create order (Firebase + Local DB)
            val firebaseResult = saveOrderToFirebase(order)

            if (firebaseResult.isSuccess) {
                orderDao.insertOrder(order)
                Result.success(order.orderId)
            } else {
                // Refund coins if order creation failed
                if (coinsUsed > 0) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.let { user ->
                        val userRepository = UserRepository(userDao)
                        userRepository.addCoinsToUser(user.uid, coinsUsed) // Refund
                    }
                }
                firebaseResult
            }
        } catch (e: Exception) {
            // Refund coins on any exception
            if (coinsUsed > 0) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.let { user ->
                    try {
                        val userRepository = UserRepository(userDao)
                        userRepository.addCoinsToUser(user.uid, coinsUsed)
                    } catch (refundException: Exception) {
                        Log.e(TAG, "Failed to refund coins: ${refundException.message}")
                    }
                }
            }
            Result.failure(e)
        }
    }

    suspend fun updateOrderWithCoinsEarned(orderId: String, coinsEarned: Int): Result<Boolean> {
        return try {
            // Update in Firebase
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "coinsEarned" to coinsEarned,
                        "updatedAt" to Date()
                    )
                )
                .await()

            // Update local database - you'll need to add this field to OrderEntity
            orderDao.updateOrderCoinsEarned(orderId, coinsEarned)

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order with coins earned: ${e.message}")
            Result.failure(e)
        }
    }
}