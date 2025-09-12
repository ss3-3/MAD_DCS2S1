package com.example.taiwanesehouse

data class CartItem(
    val documentId: String = "", // Firestore document ID as primary key
    val foodName: String,
    val basePrice: Double,
    val foodQuantity: Int,
    val foodAddOns: List<String> = emptyList(),
    val foodRemovals: List<String> = emptyList(),
    val imagesRes: Int,
    val addedAt: Long? = null,
    val foodId: String? = null
) {
    fun getTotalPrice(): Double {
        val addOnPrice = foodAddOns.size * 1.5
        return (basePrice + addOnPrice) * foodQuantity
    }

    fun getAddOnTotal(): Double {
        return foodAddOns.size * 1.5
    }

    fun getItemTotal(): Double {
        return basePrice + getAddOnTotal()
    }
}
