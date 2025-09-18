// 1. Firebase Menu Repository
package com.example.taiwanesehouse.repository

import android.util.Log
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.database.AppDatabase
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import com.example.taiwanesehouse.dataclass.FirebaseFoodItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val foodItemDao = database.foodItemDao()
    private val menuCollection = firestore.collection("menu_items")

    companion object {
        private const val TAG = "MenuRepository"
        private const val LAST_SYNC_KEY = "last_menu_sync"
        private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    // Get all food items (Room first, then Firebase if needed)
    suspend fun getAllFoodItems(): Flow<List<FoodItemEntity>> {
        // Check if we need to sync from Firebase
        val shouldSync = shouldSyncFromFirebase()

        if (shouldSync) {
            syncFromFirebase()
        }

        // Return Room data (cached)
        return foodItemDao.getAllFoodItems()
    }

    // Get food items by category
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItemEntity>> {
        return foodItemDao.getFoodItemsByCategory(category)
    }

    // Search food items
    fun searchFoodItems(query: String): Flow<List<FoodItemEntity>> {
        return foodItemDao.searchFoodItems("%$query%")
    }

    // Get food item by ID
    suspend fun getFoodItemById(id: String): FoodItemEntity? {
        return foodItemDao.getFoodItemById(id)
    }

    // Get food item by name
    suspend fun getFoodItemByName(name: String): FoodItemEntity? {
        return foodItemDao.getFoodItemByName(name)
    }

    // Initialize menu data (upload to Firebase if needed)
    suspend fun initializeMenuData() {
        try {
            // Check if Firebase has data
            val firebaseCount = getFirebaseItemCount()

            if (firebaseCount == 0) {
                // Upload initial data to Firebase
                uploadInitialDataToFirebase()
                Log.d(TAG, "Initial data uploaded to Firebase")
            }

            // Always sync to Room for local caching
            syncFromFirebase()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing menu data", e)
            // Fallback to local data if Firebase fails
            initializeLocalData()
        }
    }

    private suspend fun shouldSyncFromFirebase(): Boolean {
        val localCount = foodItemDao.getCount()
        if (localCount == 0) return true // No local data

        // Check last sync time (you can implement this using SharedPreferences)
        // For now, we'll sync if local data is empty
        return false
    }

    suspend fun syncFromFirebase() {
        try {
            Log.d(TAG, "Syncing menu data from Firebase...")

            val snapshot = menuCollection.get().await()
            val firebaseItems = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FirebaseFoodItem::class.java)?.toFoodItemEntity()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing document ${doc.id}", e)
                    null
                }
            }

            // Use stable, local mapping for imageRes based on item id to avoid mismatches
            val imageFallbackById = getInitialFoodItems().associateBy { it.id }
            val normalizedItems = firebaseItems.map { entity ->
                val fallback = imageFallbackById[entity.id]?.imageRes
                if (fallback != null && fallback != 0) entity.copy(imageRes = fallback) else entity
            }

            if (normalizedItems.isNotEmpty()) {
                // Clear existing data and insert new
                foodItemDao.deleteAll()
                foodItemDao.insertAll(normalizedItems)
                Log.d(TAG, "Successfully synced ${normalizedItems.size} items from Firebase (with stable images)")
            } else {
                // Firebase is empty, initialize local data
                Log.d(TAG, "Firebase is empty, initializing local data...")
                initializeLocalData()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from Firebase", e)
            // Fallback to local data if Firebase fails
            initializeLocalData()
        }
    }

    private suspend fun getFirebaseItemCount(): Int {
        return try {
            val snapshot = menuCollection.get().await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase item count", e)
            0
        }
    }

    private suspend fun uploadInitialDataToFirebase() {
        try {
            val initialItems = getInitialFoodItems()
            val batch = firestore.batch()

            initialItems.forEach { item ->
                val docRef = menuCollection.document(item.id)
                batch.set(docRef, FirebaseFoodItem.fromFoodItemEntity(item))
            }

            batch.commit().await()
            Log.d(TAG, "Successfully uploaded ${initialItems.size} items to Firebase")

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Firebase", e)
            throw e
        }
    }

    suspend fun initializeLocalData() {
        val count = foodItemDao.getCount()
        if (count == 0) {
            val initialItems = getInitialFoodItems()
            foodItemDao.insertAll(initialItems)
            Log.d(TAG, "Initialized local data with ${initialItems.size} items")
        }
    }

    // Force refresh from Firebase
    suspend fun refreshFromFirebase() {
        syncFromFirebase()
    }

    // Add new item (to Firebase and Room)
    suspend fun addFoodItem(item: FoodItemEntity) {
        try {
            // Add to Firebase first
            val firebaseItem = FirebaseFoodItem.fromFoodItemEntity(item)
            menuCollection.document(item.id).set(firebaseItem).await()

            // Add to Room cache
            foodItemDao.insert(item)

            Log.d(TAG, "Successfully added item: ${item.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding food item", e)
            throw e
        }
    }

    // Update item
    suspend fun updateFoodItem(item: FoodItemEntity) {
        try {
            // Update Firebase
            val firebaseItem = FirebaseFoodItem.fromFoodItemEntity(item)
            menuCollection.document(item.id).set(firebaseItem).await()

            // Update Room cache
            foodItemDao.update(item)

            Log.d(TAG, "Successfully updated item: ${item.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating food item", e)
            throw e
        }
    }

    // Delete item
    suspend fun deleteFoodItem(itemId: String) {
        try {
            // Delete from Firebase
            menuCollection.document(itemId).delete().await()

            // Delete from Room cache
            foodItemDao.deleteById(itemId)

            Log.d(TAG, "Successfully deleted item: $itemId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting food item", e)
            throw e
        }
    }

    private fun getInitialFoodItems(): List<FoodItemEntity> {
        // Your existing getAllFoodItems() data from DatabaseInitializer
        return listOf(
            // Rice Items
            FoodItemEntity(
                id = "R1",
                name = "Signature Braised Pork Rice",
                description = "Japanese Pearl Rice - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 15.90,
                imageRes = com.example.taiwanesehouse.R.drawable.signature_braised_pork_rice,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R2",
                name = "High CP Salted Chicken Rice",
                description = "Japanese Pearl Rice - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.signature_braised_pork_rice,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R3",
                name = "Meatball & Sausage Minced Pork Rice",
                description = "Japanese Pearl Rice - Minced Pork - Taiwan Sausage - Pork Meatball - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.meatball_and_sausage_minced_pork_rice,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R4",
                name = "House Crispy Chicken Chop Rice",
                description = "Japanese Pearl Rice - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.house_crispy_chicken_chop_rice,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R5",
                name = "Taiwanese Pork Chop Rice",
                description = "Jasmine Pearl Rice - Taiwanese Pork Chop - First Egg - Taiwanese Pickled Vegetable - Sour Chili",
                price = 21.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop_rice,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R6",
                name = "Khong Bah Peng",
                description = "Jasmine Pearl Rice - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 21.90,
                imageRes = R.drawable.khong_bah_peng,
                category = "Rice"
            ),
            FoodItemEntity(
                id = "R7",
                name = "Three Cup Chicken Rice",
                description = "Japanese Pearl Rice - 3 Cup Chicken - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili",
                price = 25.90,
                imageRes = R.drawable.three_cup_chicken_rice,
                category = "Rice"
            ),

            // Noodle Items - COMPLETE LIST
            FoodItemEntity(
                id = "N1",
                name = "Signature Braised Pork QQ Noodle",
                description = "Handmade Noodle - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 15.90,
                imageRes = R.drawable.signature_braised_pork_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N2",
                name = "High CP Salted Chicken QQ Noodle",
                description = "Handmade Noodle - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.high_cp_salted_chicken_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N3",
                name = "Meatball & Sausage Minced Pork QQ Noodle",
                description = "Handmade Noodle - Minced Pork - Taiwan Sausage - Pork Meatball - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.meatball_and_sausage_minced_pork_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N4",
                name = "House Crispy Chicken Chop QQ Noodle",
                description = "Handmade Noodle - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 19.90,
                imageRes = R.drawable.house_chicken_chop_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N5",
                name = "Taiwanese Belly Pork Chop QQ Noodle",
                description = "Handmade Noodle - Pork Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N6",
                name = "Gozhabi Stewed Belly QQ Noodle",
                description = "Handmade Noodle - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.gozhabi_stewed_belly_qq_noodle,
                category = "Noodles"
            ),
            FoodItemEntity(
                id = "N7",
                name = "Twice Egg Scallion Noodle",
                description = "Handmade Noodle - Twice Wallet Egg - Side Dish (Daily) - Sour Chili",
                price = 11.90,
                imageRes = R.drawable.twice_egg_scallion_noodle,
                category = "Noodles"
            ),

            // Not Too Full Items - COMPLETE LIST
            FoodItemEntity(
                id = "E1",
                name = "Yam Floss Egg Crepe",
                description = "Yam Paste - Chicken Floss - Egg - Crepe",
                price = 8.90,
                imageRes = R.drawable.yam_floss_egg_crepe,
                category = "Not Too Full"
            ),
            FoodItemEntity(
                id = "E2",
                name = "Cheese Floss Egg Crepe",
                description = "Cheese - Chicken Floss - Egg - Crepe - Mayonnaise - Sweet Chili Sauce",
                price = 8.90,
                imageRes = R.drawable.cheese_floss_egg_crepe,
                category = "Not Too Full"
            ),
            FoodItemEntity(
                id = "E3",
                name = "Cheese Ham Egg Crepe",
                description = "Chicken Ham, Cheese - Egg - Crepe - Mayonnaise - Sweet Chili Sauce",
                price = 8.90,
                imageRes = R.drawable.cheese_ham_egg_crepe,
                category = "Not Too Full"
            ),
            FoodItemEntity(
                id = "E4",
                name = "Double Cheese Egg Scallion Sandwich",
                description = "Double Cheese - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.double_cheese_egg_scallion_sandwich,
                category = "Not Too Full"
            ),
            FoodItemEntity(
                id = "E5",
                name = "Floss Egg Scallion Sandwich",
                description = "Chicken Floss - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.floss_egg_scallion_sandwich,
                category = "Not Too Full"
            ),
            FoodItemEntity(
                id = "E6",
                name = "Ham Egg Scallion Sandwich",
                description = "Chicken Ham - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.ham_egg_scallion_sandwich,
                category = "Not Too Full"
            ),

            // Snack Items - COMPLETE LIST
            FoodItemEntity(
                id = "S1",
                name = "Garlic Slice Taiwanese Sausage",
                description = "Taiwan Sausage 2 Pcs",
                price = 8.90,
                imageRes = R.drawable.garlic_slice_taiwanese_sausage,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S2",
                name = "Tempura Oyster Mushroom",
                description = "Fried Oyster Mushroom (Spicy / Original)",
                price = 9.90,
                imageRes = R.drawable.tempura_oyster_mushroom,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S3",
                name = "Sweet Plum Potato Fries",
                description = "Fired Sweet Orange Potato",
                price = 9.90,
                imageRes = R.drawable.sweet_plum_potato_fried,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S4",
                name = "High CP Salted Chicken",
                description = "Fried Salted Chicken (Spicy / Original)",
                price = 12.90,
                imageRes = R.drawable.high_cp_salted_chicken,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S5",
                name = "Taiwanese Belly Pork Chop",
                description = "Fried Juicy Pork Chop (Spicy / Original)",
                price = 14.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S6",
                name = "House Crispy Chicken Chop",
                description = "Fried Juicy Chicken Chop (Spicy / Original)",
                price = 13.90,
                imageRes = R.drawable.house_crispy_chicken_chop,
                category = "Snacks"
            ),
            FoodItemEntity(
                id = "S7",
                name = "Sweet Not Spicy",
                description = "Tempura (No Spicy)",
                price = 12.90,
                imageRes = R.drawable.sweet_not_spicy,
                category = "Snacks"
            ),

            // Drink Items - COMPLETE LIST
            FoodItemEntity(
                id = "D1",
                name = "Aloe Yakult Tea",
                description = "",
                price = 8.90,
                imageRes = R.drawable.aloe_yakult_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D2",
                name = "TW Aiyu Jelly",
                description = "",
                price = 7.90,
                imageRes = R.drawable.tw_aiyu_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D3",
                name = "Dark Aroma Lemon Tea",
                description = "",
                price = 5.90,
                imageRes = R.drawable.dark_aroma_lemon_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D4",
                name = "Original Lemon Tea",
                description = "",
                price = 5.90,
                imageRes = R.drawable.original_lemon_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D5",
                name = "Earl Grey Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.earl_grey_milk_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D6",
                name = "Pearl Earl Milk Tea",
                description = "",
                price = 8.90,
                imageRes = R.drawable.pearl_earl_milk_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D7",
                name = "White Peach Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.white_peach_milk_tea,
                category = "Drinks"
            ),
            FoodItemEntity(
                id = "D8",
                name = "Jasmine Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.jasmine_milk_tea,
                category = "Drinks"
            )
        )
    }

    // Clear Firebase data if need to reset
    suspend fun clearFirebaseData() {
        try {
            val snapshot = menuCollection.get().await()
            val batch = firestore.batch()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Cleared all Firebase data")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Firebase data", e)
        }
    }

    // Debug method to check database status
    suspend fun getDatabaseStatus(): String {
        val localCount = foodItemDao.getCount()
        val firebaseCount = getFirebaseItemCount()
        return "Local items: $localCount, Firebase items: $firebaseCount"
    }
}