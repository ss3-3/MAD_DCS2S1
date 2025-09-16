// 2. DAO (Data Access Object)
package com.example.taiwanesehouse.database.dao

import androidx.room.*
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY category, name")
    fun getAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY name")
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE :searchQuery OR description LIKE :searchQuery ORDER BY name")
    fun searchFoodItems(searchQuery: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE name = :name")
    suspend fun getFoodItemByName(name: String): FoodItemEntity?

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodItems: List<FoodItemEntity>)

    @Update
    suspend fun update(foodItem: FoodItemEntity)

    @Delete
    suspend fun delete(foodItem: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM food_items")
    suspend fun deleteAll()
}