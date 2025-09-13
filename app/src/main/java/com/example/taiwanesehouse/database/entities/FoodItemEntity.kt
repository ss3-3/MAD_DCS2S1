package com.example.taiwanesehouse.database.entities

// 1. Entity (Database Table)
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageRes: Int,
    val category: String, // "Rice", "Noodles", "Not Too Full", "Snacks", "Drinks"
    val addOns: String = "", // JSON string of available add-ons
    val removableItems: String = "" // JSON string of removable items
)
