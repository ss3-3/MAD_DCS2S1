package com.example.taiwanesehouse.dataclass

data class CartItem(
    val documentId: String = "", // Firebase document ID for updates/deletions
    val foodName: String = "",
    val basePrice: Double = 0.0,
    val foodQuantity: Int = 1,
    val foodAddOns: List<String> = emptyList(),
    val foodRemovals: List<String> = emptyList(),
    val imagesRes: Int = 0, // Resource ID for food image
    val addedAt: Long? = null, // Timestamp when added to cart
    val foodId: String? = null, // Original food item ID
    val addOnPrices: Map<String, Double> = emptyMap() // Store individual add-on prices
) {
    // Calculate total price including add-ons with proper pricing
    fun getTotalPrice(): Double {
        val addOnPrice = foodAddOns.sumOf { addOn ->
            addOnPrices[addOn] ?: getDefaultAddOnPrice(addOn)
        }
        return (basePrice + addOnPrice) * foodQuantity
    }

    // Default add-on prices (fallback if not provided)
    private fun getDefaultAddOnPrice(addOn: String): Double {
        return when (addOn.lowercase()) {
            "egg" -> 1.0
            "vegetable" -> 2.0
            else -> 2.0 // Default price
        }
    }

    // Extension function for payment history
    fun toPaymentHistoryItem(): String {
        return "$foodName x$foodQuantity"
    }

    // Get display name with modifications
    fun getDisplayName(): String {
        val modifications = mutableListOf<String>()

        if (foodAddOns.isNotEmpty()) {
            modifications.add("Add: ${foodAddOns.joinToString(", ")}")
        }

        if (foodRemovals.isNotEmpty()) {
            modifications.add("Remove: ${foodRemovals.joinToString(", ")}")
        }

        return if (modifications.isNotEmpty()) {
            "$foodName (${modifications.joinToString("; ")})"
        } else {
            foodName
        }
    }

    // Check if this cart item matches another (for duplicate detection)
    fun hasSameConfiguration(other: CartItem): Boolean {
        return foodName == other.foodName &&
                basePrice == other.basePrice &&
                foodAddOns.sorted() == other.foodAddOns.sorted() &&
                foodRemovals.sorted() == other.foodRemovals.sorted()
    }

    // Get formatted price string
    fun getFormattedPrice(): String {
        return "RM %.2f".format(getTotalPrice())
    }

    // Get formatted unit price string
    fun getFormattedUnitPrice(): String {
        val addOnPrice = foodAddOns.sumOf { addOn ->
            addOnPrices[addOn] ?: getDefaultAddOnPrice(addOn)
        }
        val unitPrice = basePrice + addOnPrice
        return "RM %.2f".format(unitPrice)
    }

    // Check if item has modifications
    fun hasModifications(): Boolean {
        return foodAddOns.isNotEmpty() || foodRemovals.isNotEmpty()
    }

    // Create a copy with updated quantity
    fun withQuantity(newQuantity: Int): CartItem {
        return this.copy(foodQuantity = newQuantity.coerceAtLeast(1))
    }

    // Validate cart item data
    fun isValid(): Boolean {
        return foodName.isNotBlank() &&
                basePrice >= 0 &&
                foodQuantity > 0
    }
}

// Additional extension functions for easier usage
fun List<CartItem>.getTotalPrice(): Double {
    return this.sumOf { it.getTotalPrice() }
}

fun List<CartItem>.getTotalQuantity(): Int {
    return this.sumOf { it.foodQuantity }
}

fun List<CartItem>.getFormattedTotalPrice(): String {
    return "RM %.2f".format(getTotalPrice())
}

// Helper function to create CartItem from food selection with proper pricing
fun createCartItem(
    foodName: String,
    basePrice: Double,
    quantity: Int = 1,
    addOns: List<String> = emptyList(),
    removals: List<String> = emptyList(),
    imageRes: Int = 0,
    foodId: String? = null,
    addOnPrices: Map<String, Double> = emptyMap()
): CartItem {
    return CartItem(
        documentId = "", // Will be set when added to Firebase
        foodName = foodName,
        basePrice = basePrice,
        foodQuantity = quantity,
        foodAddOns = addOns,
        foodRemovals = removals,
        imagesRes = imageRes,
        addedAt = System.currentTimeMillis(),
        foodId = foodId,
        addOnPrices = addOnPrices
    )
}