package com.example.taiwanesehouse

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query  // Add this missing import
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseCartManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _memberCoins = MutableStateFlow(0)
    val memberCoins: StateFlow<Int> = _memberCoins.asStateFlow()

    private val _coinsToUse = MutableStateFlow(0)
    val coinsToUse: StateFlow<Int> = _coinsToUse.asStateFlow()

    private var cartListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    init {
        auth.currentUser?.let { user ->
            setupCartListener(user.uid)
            setupUserListener(user.uid)
        }
    }

    private fun setupUserListener(userId: String) {
        userListener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val coins = snapshot?.getLong("memberCoins")?.toInt() ?: 0
                _memberCoins.value = coins
            }
    }

    // Updated setupCartListener to include document ID
    private fun setupCartListener(userId: String) {
        cartListener = firestore.collection("users")
            .document(userId)
            .collection("cart")
            .orderBy("addedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        CartItem(
                            documentId = doc.id, // Include the document ID
                            foodName = doc.getString("foodName") ?: "",
                            basePrice = doc.getDouble("basePrice") ?: 0.0,
                            foodQuantity = doc.getLong("foodQuantity")?.toInt() ?: 1,
                            foodAddOns = doc.get("foodAddOns") as? List<String> ?: emptyList(),
                            foodRemovals = doc.get("foodRemovals") as? List<String> ?: emptyList(),
                            imagesRes = doc.getLong("imagesRes")?.toInt() ?: 0,
                            addedAt = doc.getTimestamp("addedAt")?.toDate()?.time,
                            foodId = doc.getString("foodId")
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                _cartItems.value = items
            }
    }

    // Add the missing addToCart function
    suspend fun addToCart(cartItem: CartItem): Boolean {
        return try {
            val user = auth.currentUser ?: return false

            // Create a unique document ID for this cart item
            val cartRef = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
                .document()

            val cartData = hashMapOf(
                "foodName" to cartItem.foodName,
                "basePrice" to cartItem.basePrice,
                "foodQuantity" to cartItem.foodQuantity,
                "foodAddOns" to cartItem.foodAddOns,
                "foodRemovals" to cartItem.foodRemovals,
                "imagesRes" to cartItem.imagesRes,
                "foodId" to cartItem.foodId,
                "addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            cartRef.set(cartData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Better update method using document ID
    suspend fun updateCartItemQuantityById(documentId: String, newQuantity: Int): Boolean {
        return try {
            val user = auth.currentUser ?: return false

            if (newQuantity < 1) return false

            firestore.collection("users")
                .document(user.uid)
                .collection("cart")
                .document(documentId)
                .update("foodQuantity", newQuantity)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Better remove method using document ID
    suspend fun removeFromCartById(documentId: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false

            firestore.collection("users")
                .document(user.uid)
                .collection("cart")
                .document(documentId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Keep the index-based methods for backward compatibility if needed
    suspend fun updateCartItemQuantity(index: Int, newQuantity: Int): Boolean {
        val currentItems = _cartItems.value
        return if (index < currentItems.size) {
            updateCartItemQuantityById(currentItems[index].documentId, newQuantity)
        } else {
            false
        }
    }

    suspend fun removeFromCart(index: Int): Boolean {
        val currentItems = _cartItems.value
        return if (index < currentItems.size) {
            removeFromCartById(currentItems[index].documentId)
        } else {
            false
        }
    }

    suspend fun clearCart(): Boolean {
        return try {
            val user = auth.currentUser ?: return false

            val cartSnapshot = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
                .get()
                .await()

            val batch = firestore.batch()
            cartSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            // Reset coins to use
            _coinsToUse.value = 0

            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateCoinsToUse(coins: Int) {
        val maxCoins = minOf(_memberCoins.value, (_cartItems.value.sumOf { it.getTotalPrice() } * 100).toInt())
        _coinsToUse.value = coins.coerceIn(0, maxCoins)
    }

    fun cleanup() {
        cartListener?.remove()
        userListener?.remove()
    }
}