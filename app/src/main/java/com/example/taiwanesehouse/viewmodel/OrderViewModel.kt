package com.example.taiwanesehouse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.database.entities.OrderEntity
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.dataclass.CartItem
import com.example.taiwanesehouse.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository
    private val auth = FirebaseAuth.getInstance()

    init {
        val db = AppDatabase.getDatabase(application)
        val orderDao = db.orderDao()
        val userDao = db.userDao()
        repository = OrderRepository(orderDao, userDao)
    }

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _orderCreated = MutableStateFlow<String?>(null)
    val orderCreated: StateFlow<String?> = _orderCreated.asStateFlow()

    // Get current user's orders
    fun getUserOrders(): Flow<List<OrderEntity>> {
        val userId = auth.currentUser?.uid
        return if (userId != null) {
            repository.getOrdersByUserId(userId)
        } else {
            flowOf(emptyList())
        }
    }

    // Get order by ID
    suspend fun getOrderById(orderId: String): OrderEntity? {
        return repository.getOrderById(orderId)
    }

    // Create order from cart items (original method - keep for compatibility)
    fun createOrderFromCart(
        cartItems: List<CartItem>,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        deliveryAddress: String? = null,
        notes: String? = null,
        paymentMethod: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "User not authenticated"
                    return@launch
                }

                // Generate unique order ID
                val orderId = "ORDER_${System.currentTimeMillis()}"

                // Convert cart items to order items
                val orderItems = cartItems.map { cartItem ->
                    val addOnCost = calculateAddOnCost(cartItem.foodAddOns)
                    val itemTotalPrice = (cartItem.basePrice + addOnCost) * cartItem.foodQuantity

                    OrderItemEntity(
                        orderId = orderId,
                        foodName = cartItem.foodName,
                        basePrice = cartItem.basePrice,
                        quantity = cartItem.foodQuantity,
                        addOns = cartItem.foodAddOns,
                        removals = cartItem.foodRemovals,
                        itemTotalPrice = itemTotalPrice,
                        imageRes = cartItem.imagesRes // Fixed property name
                    )
                }

                // Calculate total amount
                val totalAmount = orderItems.sumOf { it.itemTotalPrice }

                // Create order entity
                val order = OrderEntity(
                    orderId = orderId,
                    userId = currentUser.uid,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    orderItems = orderItems,
                    subtotalAmount = totalAmount,
                    coinDiscount = 0.0,
                    coinsUsed = 0,
                    totalAmount = totalAmount,
                    orderStatus = "pending",
                    orderDate = Date(),
                    notes = notes,
                    paymentStatus = "pending",
                    paymentMethod = paymentMethod,
                    createdAt = Date(),
                    updatedAt = Date()
                )

                // Save order
                val result = repository.createOrder(order)

                if (result.isSuccess) {
                    _orderCreated.value = orderId
                    // Sync with Firebase
                    syncUserOrders()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to create order"
                }

            } catch (e: Exception) {
                _error.value = "Error creating order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW: Create order with cart data integration
    fun createOrderWithCartData(
        cartItems: List<CartItem>,
        orderItems: List<OrderItemEntity>,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        deliveryAddress: String?,
        notes: String?,
        paymentMethod: String,
        subtotal: Double,
        coinDiscount: Double,
        finalTotal: Double,
        coinsUsed: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _error.value = "User not authenticated"
                    return@launch
                }

                // Generate unique order ID
                val orderId = "ORDER_${currentUser.uid}_${System.currentTimeMillis()}"

                // Update order items with the generated order ID
                val updatedOrderItems = orderItems.map { it.copy(orderId = orderId) }

                val order = OrderEntity(
                    orderId = orderId,
                    userId = currentUser.uid,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    orderItems = updatedOrderItems,
                    subtotalAmount = subtotal,
                    coinDiscount = coinDiscount,
                    coinsUsed = coinsUsed,
                    totalAmount = finalTotal, // Use final total with discounts
                    orderStatus = "pending",
                    orderDate = Date(),
                    notes = notes,
                    paymentStatus = if (paymentMethod == "cash") "pending" else "pending",
                    paymentMethod = paymentMethod,
                    createdAt = Date(),
                    updatedAt = Date()
                )

                // Do NOT deduct coins here; handle coin deduction only after successful payment

                val result = repository.createOrder(order)
                if (result.isSuccess) {
                    _orderCreated.value = orderId
                    syncUserOrders()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to create order"
                }

            } catch (e: Exception) {
                _error.value = "Error creating order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update order status
    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateOrderStatus(orderId, newStatus)

                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update order status"
                }
            } catch (e: Exception) {
                _error.value = "Error updating order status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update payment status
    fun updatePaymentStatus(
        orderId: String,
        paymentStatus: String,
        paymentMethod: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updatePaymentStatus(orderId, paymentStatus, paymentMethod)

                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update payment status"
                }
            } catch (e: Exception) {
                _error.value = "Error updating payment status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete order
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.deleteOrder(orderId)

                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to delete order"
                }
            } catch (e: Exception) {
                _error.value = "Error deleting order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Sync orders from Firebase
    private fun syncUserOrders() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                repository.syncOrdersFromFirebase(userId)
            } catch (e: Exception) {
                // Silent sync - don't show error to user
            }
        }
    }

    // Get orders by status
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return repository.getOrdersByStatus(status)
    }

    // Get user analytics
    suspend fun getUserAnalytics(): UserOrderAnalytics {
        val userId = auth.currentUser?.uid ?: return UserOrderAnalytics()

        return UserOrderAnalytics(
            totalOrders = repository.getOrderCountByUser(userId),
            totalSpent = repository.getTotalSpentByUser(userId)
        )
    }

    // Helper functions
    private fun calculateAddOnCost(addOns: List<String>): Double {
        return addOns.sumOf { addOn ->
            when (addOn.lowercase()) {
                "egg" -> 1.0
                "vegetable" -> 2.0
                else -> 0.0
            }
        }
    }

    // Clear error and success states
    fun clearError() {
        _error.value = null
    }

    fun clearOrderCreated() {
        _orderCreated.value = null
    }
}

// Data class for user analytics
data class UserOrderAnalytics(
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0
)