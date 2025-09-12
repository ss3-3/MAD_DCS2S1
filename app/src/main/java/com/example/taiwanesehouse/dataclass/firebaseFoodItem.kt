package com.example.taiwanesehouse.dataclass

import com.example.taiwanesehouse.database.FoodItemEntity

// 2. Firebase Data Model
data class FirebaseFoodItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "", // Firebase will use URLs instead of resource IDs
    val imageRes: Int = 0, // Keep for backward compatibility
    val category: String = "",
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFoodItemEntity(): FoodItemEntity {
        return FoodItemEntity(
            id = id,
            name = name,
            description = description,
            price = price,
            imageRes = imageRes, // You'll need to map URLs to resources
            category = category
        )
    }

    companion object {
        fun fromFoodItemEntity(entity: FoodItemEntity): FirebaseFoodItem {
            return FirebaseFoodItem(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                price = entity.price,
                imageRes = entity.imageRes,
                category = entity.category,
                isAvailable = true
            )
        }
    }
}